package msgrouter.engine;

import java.io.File;
import java.util.List;

import elastic.util.concurrent.LockMgr;
import elastic.util.java.ReflectionException;
import elastic.util.java.ReflectionUtil;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.pool.loadbalancing.LoadBalancingPool;
import elastic.util.pool.loadbalancing.PoolEntry;
import elastic.util.pool.loadbalancing.PoolEntryRobot;
import elastic.util.sqlmgr.SqlConnPool;
import elastic.util.util.KeyGenerator;
import elastic.util.util.RollingLogger;
import elastic.util.util.RollingLoggerParams;
import elastic.util.util.TechException;
import elastic.util.xml.XmlEnv;
import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.QueueEntry;
import msgrouter.api.interfaces.bean.ClientLoginer;
import msgrouter.api.interfaces.bean.Loginer;
import msgrouter.api.interfaces.bean.ServerLoginer;
import msgrouter.api.resource.LoginIds;
import msgrouter.constant.Const;
import msgrouter.engine.config.ContainersConfig;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import msgrouter.engine.config.ServiceConfig;
import msgrouter.engine.event.Event;
import msgrouter.engine.nosession.NoSessionService;
import msgrouter.engine.queue.NotSynchronizedQueue;
import msgrouter.engine.queue.QueueTimeoutException;

public abstract class Service extends LifeCycleObject {
	public static final short IVAL_SVC_TYPE_SERVER = 0;
	public static final short IVAL_SVC_TYPE_CLIENT = 1;
	public static final short IVAL_SVC_TYPE_NOSESSION = 2;

	public static final short IVAL_ROUTING_TARGET_MSGROUTER_ID = 1;
	public static final short IVAL_ROUTING_TARGET_CUSTOM_ID = 2;
	public static final short IVAL_ROUTING_TARGET_IP = 3;
	public static final short IVAL_ROUTING_TARGET_SESSION = 4;

	private final ServiceContext svcContext;
	private final Class clazz;
	private final ServiceBootstrapConfig sbc;
	private final ClassLoader cl;
	private volatile ServiceConfig svcConf = null;
	private final Container container;
	private AuthEntryAdmin aeAdmin = null;
	private LockMgr lockMgr = null;
	private volatile NoSession nosession = null;
	private volatile KeyedList<String, LoginIdGroup> loginIdGroupList = null;
	private volatile KeyedList<String, IpGroup> ipGroupList = null;
	private final KeyGenerator ssNameGen = new KeyGenerator();
	private LoadBalancingPool ssRegistryPool = null;
	private final MessageLogger svcMsgLogger;
	private final NotSynchronizedQueue<Event> pendingEvents = new NotSynchronizedQueue<Event>();

	/**
	 * 2016-04-25, Jake Jaehee Lee
	 */
	private int nrOfConnections = 0;
	private int nrOfSessions = 0;
	private final Object nrOfConnectionsSync = new Object();
	private final Object nrOfSessionsSync = new Object();

	/* commented out. Aug 11, 2015 by Jake Lee */
	// private MessageFactory msgFactory = null;
	public static final short IVAL_DUP_ALIVE_ALL = 2;
	public static final short IVAL_DUP_ALIVE_LAST = 1;
	public static final short IVAL_DUP_ALIVE_FIRST = 0;

	public Event getEvent() {
		return pendingEvents.poll();
	}

	public void putEvent(Event event) throws TechException, QueueTimeoutException {
		pendingEvents.put(event);
	}

	public Service(Class clazz, ServiceBootstrapConfig sbc, ClassLoader cl, Container container) {
		super(clazz);

		this.svcContext = new ServiceContext(this);
		this.clazz = clazz;
		this.sbc = sbc;
		this.cl = cl;
		this.container = container;

		/*
		 * Engine logger setting.
		 */
		String root = XmlEnv.get("log.dir") + File.separator + "container" + File.separator + "service-"
				+ sbc.getServiceId();

		RollingLoggerParams loggerParams = new RollingLoggerParams();
		loggerParams.setPath(root, "service.log");
		loggerParams.setDatePattern(Const.LOG_FILE_DATE_PATTERN);
		loggerParams.setEncoding(Const.LOG_ENCODING);
		loggerParams.setLayoutName(Const.LOG_LAYOUT_CLASS);
		loggerParams.setLayoutParams(Const.LOG_LAYOUT_CLASS_PARAMS_4engine1);
		loggerParams.setMaxFileSize(10240000);
		loggerParams.setLevel(getLogLevel());

		setLogger(RollingLogger.getLogger(loggerParams));

		/*
		 * Message logger setting.
		 */
		MessageLoggerParams svcMsgLogParams = getServiceConfig().getServiceMessageLoggerParams();

		if (svcMsgLogParams != null) {
			if (svcMsgLogParams.getLogPer() == Const.IVAL_MSG_LOG_PER_OFF) {
				this.svcMsgLogger = new MessageLogger();
			} else {
				this.svcMsgLogger = new MessageLogger(svcMsgLogParams);
			}
		} else {
			this.svcMsgLogger = new MessageLogger();
		}
	}

	public MessageLogger getServiceMessageLogger() {
		return svcMsgLogger;
	}

	public final SqlConnPool getSqlConnPool() {
		return MsgRouter.getInstance().getSqlConnPool(getServiceConfig().getSqlConnPoolName());
	}

	public ServiceContext getServiceContext() {
		return svcContext;
	}

	public ServiceConfig getServiceConfig() {
		if (svcConf == null) {
			synchronized (this) {
				if (svcConf == null) {
					try {
						svcConf = new ServiceConfig(sbc, cl);
					} catch (TechException e) {
						throw new RuntimeException(
								"failed to initialize " + ServiceConfig.class.getSimpleName() + ": " + e.getMessage());
					}
				}
			}
		}
		return svcConf;
	}

	public void run() {
		try {
			lockMgr = new LockMgr();

			executeThreadInitializer();

			if (getServiceConfig().getServiceInitializerClass() != null) {
				ServiceInitializer svcInit = (ServiceInitializer) getServiceConfig().getServiceInitializerClass()
						.newInstance();
				svcInit.setService(this);
				logDebug(svcInit.getClass().getName() + " is executing");
				svcInit.execute();
				logDebug(svcInit.getClass().getName() + " is done");
			}

			this.nosession = getNoSession();

			if (clazz != NoSessionService.class) {
				this.loginIdGroupList = new KeyedList<String, LoginIdGroup>();
				this.ipGroupList = new KeyedList<String, IpGroup>();

				String amConfFullPath = getServiceConfig().getAuthManagerConfigPath();
				if (amConfFullPath != null) {
					logInfo("loading " + amConfFullPath);
					this.aeAdmin = new AuthEntryAdmin(getServiceConfig());
				}

				this.ssRegistryPool = new LoadBalancingPool(new SessionRegistryFactory(this),
						getServiceConfig().getMinBeanThrs());
				this.ssRegistryPool.setLogger(getLogger());
				new LifeCycle(this.ssRegistryPool, "ssRegitryPL", this).start();
			}

			logTrace(Service.class.getSimpleName() + " " + getServiceConfig().getServiceId() + "'s ClassLoader=" + cl);

			ContainersConfig containersConfig = MsgRouter.getInstance().getConfig().getContainersConfig();

			containersConfig.addServiceConfig(getServiceConfig());

			MsgRouter.getInstance().registerService(getServiceConfig().getServiceId(), this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public ClassLoader getClassLoader() {
		return cl;
	}

	public void executeThreadInitializer() throws Exception {
		if (getServiceConfig().getThreadInitializerClass() != null) {
			ThreadInitializer thrInit = (ThreadInitializer) getServiceConfig().getThreadInitializerClass()
					.newInstance();
			logDebug(thrInit.getClass().getName() + " is executing");
			thrInit.setServiceClassLoader(cl);
			thrInit.execute();
			logDebug(thrInit.getClass().getName() + " is done");
		}
	}

	public void restartCronjobs() {
		nosession.restartCronjob();
		if (loginIdGroupList != null) {
			List<LoginIdGroup> list = loginIdGroupList.getList();
			for (int l = 0; l < list.size(); l++) {
				LoginIdGroup idGroup = list.get(l);
				List<Session> ssList = idGroup.getList();
				for (int s = 0; s < ssList.size(); s++) {
					Session ss = ssList.get(s);
					ss.restartCronjob();
				}
			}
		}
	}

	class RegisterRobot implements PoolEntryRobot {
		private Service svc;
		private Session session = null;

		RegisterRobot(Service svc, Session session) {
			this.svc = svc;
			this.session = session;
		}

		public boolean useLoadBalancingEntry(PoolEntry ssRegistry) {
			String ssName = null;
			try {
				ssName = String.valueOf(ssNameGen.newKey());
				new LifeCycle(session, ssName, ssRegistry).start();
				return true;
			} catch (Exception e) {
				if (ssName != null) {
					ssNameGen.clearKey(Integer.parseInt(ssName));
				}
			}
			return false;
		}
	}

	/**
	 * Start warm Session
	 * 
	 * @param session
	 * @return
	 */
	public boolean startSession(Session session) {
		if (svcConf.getRoutingTarget() == IVAL_ROUTING_TARGET_MSGROUTER_ID
				|| svcConf.getRoutingTarget() == IVAL_ROUTING_TARGET_CUSTOM_ID) {
			if (session.getLoginId() != null) {
				LoginIdGroup loginIdGroup = getLoginIdGroup(session.getLoginId(), false);
				if (loginIdGroup != null) {
					Session firstSession = loginIdGroup.getFirst();
					if (firstSession != null) {
						if (svcConf.getDupAliveType() == IVAL_DUP_ALIVE_FIRST) {
							session.logWarn("shutting down... because there is another session with the same loginId '"
									+ session.getLoginId() + "'.");
							session.killEventHandler();
							session = null;
						} else if (svcConf.getDupAliveType() == IVAL_DUP_ALIVE_LAST) {
							if (firstSession.currentLifeCycle() != null) {
								firstSession.logWarn(
										"shutting down... because there is another session with the same loginId '"
												+ session.getLoginId() + "'.");
								firstSession.currentLifeCycle().killNow();
							}
						}
					}
				}
			}
		} else if (svcConf.getRoutingTarget() == IVAL_ROUTING_TARGET_IP) {
			if (session.getRemoteIp() != null) {
				IpGroup ipGroup = getIpGroup(session.getRemoteIp(), false);
				if (ipGroup != null) {
					Session firstSession = ipGroup.getFirst();
					if (firstSession != null) {
						if (svcConf.getDupAliveType() == IVAL_DUP_ALIVE_FIRST) {
							session.logWarn("shutting down... because there is another session with the same remoteIp '"
									+ session.getRemoteIp() + "'.");
							session.killEventHandler();
							session = null;
						} else if (svcConf.getDupAliveType() == IVAL_DUP_ALIVE_LAST) {
							if (firstSession.currentLifeCycle() != null) {
								firstSession.logWarn(
										"shutting down... because there is another session with the same remoteIp '"
												+ session.getRemoteIp() + "'.");
								firstSession.currentLifeCycle().killNow();
							}
						}
					}
				}
			}
		}
		if (session != null) {
			RegisterRobot robot = new RegisterRobot(this, session);
			if (ssRegistryPool.execute(robot)) {
				if (session.getLoginId() != null) {
					LoginIdGroup loginIdGroup = getLoginIdGroup(session.getLoginId(), true);
					loginIdGroup.put(session.getAlias(), session);
				}
				if (session.getRemoteIp() != null) {
					IpGroup ipGroup = getIpGroup(session.getRemoteIp(), true);
					ipGroup.put(session.getAlias(), session);
				}
				return true;
			}
		}
		return false;
	}

	public void deregisterSession(Session session) {
		if (loginIdGroupList != null) {
			synchronized (loginIdGroupList) {
				LoginIdGroup loginIdGroup = loginIdGroupList.get(session.getLoginId());
				if (loginIdGroup != null) {
					if (session.logDebugEnabled())
						session.logDebug("removed from idGroup.");
					loginIdGroup.remove(session.getAlias());
					if (loginIdGroup.size() == 0) {
						if (logDebugEnabled())
							logDebug("loginIdGroup '" + session.getLoginId() + "' is removed from loginIdGroupList.");
						loginIdGroupList.remove(session.getLoginId());
					}
				}
			}
		}

		if (ipGroupList != null) {
			synchronized (ipGroupList) {
				IpGroup ipGroup = ipGroupList.get(session.getRemoteIp());
				if (ipGroup != null) {
					if (session.logDebugEnabled())
						session.logDebug("removed from ipGroup.");
					ipGroup.remove(session.getAlias());
					if (ipGroup.size() == 0) {
						if (logDebugEnabled())
							logDebug("ipGroup '" + session.getRemoteIp() + "' is removed from ipGroupList.");
						ipGroupList.remove(session.getRemoteIp());
					}
				}
			}
		}

		if (session.getAlias() != null) {
			try {
				if (session.logDebugEnabled())
					session.logDebug("Session name generator clears the session name '" + session.getAlias() + "'.");
				ssNameGen.clearKey(Integer.parseInt(session.getAlias()));
			} catch (Exception e) {
			}
		}

		decreaseNrOfSessions();
	}

	public Session getSessionByName(String ssName) {
		List<LifeCycleObject> ssRegistryList = ssRegistryPool.childList();
		if (ssRegistryList != null) {
			for (int l = 0; l < ssRegistryList.size(); l++) {
				SessionRegistry ssRegistry = (SessionRegistry) ssRegistryList.get(l);
				if (ssRegistry != null) {
					Session ss = (Session) ssRegistry.getChild(ssName);
					if (ss != null) {
						return ss;
					}
				}
			}
		}
		return null;
	}

	public Session getFirstSession() {
		SessionRegistry ssRegistry = (SessionRegistry) ssRegistryPool.getFirstChild();
		if (ssRegistry != null) {
			return (Session) ssRegistry.getFirstChild();
		}
		return null;
	}

	public final int totalSessions() {
		return ssNameGen.totalKeys();
	}

	public Container getContainer() {
		return container;
	}

	public void killEventHandler() {
		MsgRouter mr = MsgRouter.getInstance();
		String svcId = getServiceConfig().getServiceId();
		mr.deregisterService(svcId);
		loginIdGroupList.clear();
		ipGroupList.clear();
	}

	public String getServiceId() {
		return getServiceConfig().getServiceId();
	}

	public LockMgr getLockMgr() {
		return lockMgr;
	}

	public AuthEntryAdmin getAuthEntryAdmin() {
		return aeAdmin;
	}

	public LoginIds getLoginIds() {
		return new LoginIds(this);
	}

	public NoSession getNoSession() {
		if (nosession == null) {
			synchronized (this) {
				if (nosession == null) {
					logTrace("### getNoSession(): " + Thread.currentThread().toString());
					nosession = new NoSession(this);
					new LifeCycle(nosession, "noSs", this).start();
				}
			}
		}
		return nosession;
	}

	public LoginIdGroup getLoginIdGroup(String loginId, boolean create) {
		if (loginId == null || "".equals(loginId)) {
			return null;
		}
		LoginIdGroup idGroup = loginIdGroupList.get(loginId);
		if (idGroup == null) {
			if (create) {
				synchronized (loginIdGroupList) {
					idGroup = loginIdGroupList.get(loginId);
					if (idGroup == null) {
						idGroup = new LoginIdGroup(loginId);
						loginIdGroupList.put(loginId, idGroup);
					}
				}
			}
		}
		return idGroup;
	}

	public IpGroup getIpGroup(String ip, boolean create) {
		if (ip == null || "".equals(ip)) {
			return null;
		}
		IpGroup ipGroup = ipGroupList.get(ip);
		if (ipGroup == null) {
			if (create) {
				synchronized (ipGroupList) {
					ipGroup = ipGroupList.get(ip);
					if (ipGroup == null) {
						ipGroup = new IpGroup(ip);
						ipGroupList.put(ip, ipGroup);
					}
				}
			}
		}
		return ipGroup;
	}

	public void putAllSQ(QueueEntry qe) throws TechException, QueueTimeoutException {
		List<LifeCycleObject> ssRegistryList = ssRegistryPool.childList();
		if (ssRegistryList != null) {
			for (int l = 0; l < ssRegistryList.size(); l++) {
				SessionRegistry ssRegistry = (SessionRegistry) ssRegistryList.get(l);
				if (ssRegistry != null) {
					List<LifeCycleObject> list = ssRegistry.childList();
					if (list != null) {
						for (int s = 0; s < list.size(); s++) {
							Session ss = (Session) list.get(s);
							if (ss != null) {
								ss.putSQ(qe);
							}
						}
					}
				}
			}
		}
	}

	public Loginer newLoginer() throws TechException {
		try {
			if (svcConf.getRoutingTarget() == IVAL_ROUTING_TARGET_CUSTOM_ID) {
				String loginerClassName = svcConf.getLoginerClassName();
				if (loginerClassName == null || "".equals(loginerClassName)) {
					throw new TechException("loginerClass is not defined");
				}
				if (svcConf.isServerService()) {
					Loginer loginer = (Loginer) ReflectionUtil.newInstance(loginerClassName, getClassLoader());
					if (loginer != null) {
						((ServerLoginer) loginer).setService(this);
						((ServerLoginer) loginer).setServiceConfig(svcConf);
					}
					return loginer;
				} else if (svcConf.isClientService()) {
					Loginer loginer = (Loginer) ReflectionUtil.newInstance(loginerClassName, getClassLoader());
					if (loginer != null) {
						((ClientLoginer) loginer).setService(this);
						((ClientLoginer) loginer).setServiceConfig(svcConf);
					}
					return loginer;
				}
			}
			return null;
		} catch (ReflectionException e) {
			throw new TechException(e);
		}
	}

	/**
	 * 2016-04-25, Jake Jaehee Lee
	 */
	public int getNrOfSessions() {
		return nrOfSessions;
	}

	public int increaseNrOfSessions() {
		synchronized (nrOfSessionsSync) {
			++nrOfSessions;
		}
		// logTrace("increaseNrOfSessions() to " + nrOfSessions);
		return nrOfSessions;
	}

	public int decreaseNrOfSessions() {
		synchronized (nrOfSessionsSync) {
			if (nrOfSessions > 0)
				--nrOfSessions;
			else
				nrOfSessions = 0;
		}
		// logTrace("decreaseNrOfSessions() to " + nrOfSessions);
		return nrOfSessions;
	}

	public int getNrOfConnections() {
		return nrOfConnections;
	}

	public int increaseNrOfConnections() {
		synchronized (nrOfConnectionsSync) {
			++nrOfConnections;
		}
		// logTrace("increaseNrOfConnections() to " + nrOfConnections);
		return nrOfConnections;
	}

	public int decreaseNrOfConnections() {
		synchronized (nrOfConnectionsSync) {
			if (nrOfConnections > 0)
				--nrOfConnections;
			else
				nrOfConnections = 0;
		}
		// logTrace("decreaseNrOfConnections() to " + nrOfConnections);
		return nrOfConnections;
	}
}
