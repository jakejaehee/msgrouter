package msgrouter.engine.socket.client;

import java.util.List;

import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.bean.ClientLoginer;
import msgrouter.engine.Container;
import msgrouter.engine.Service;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import msgrouter.engine.event.Event;
import msgrouter.engine.event.EventInitProcMap;
import msgrouter.engine.event.EventRestartSessionRequired;
import elastic.util.util.StringUtil;
import elastic.util.util.TechException;

public class Client extends Service implements Runnable {
	private final ServiceBootstrapConfig sbc;
	private final ClassLoader cl;

	public Client(ServiceBootstrapConfig sbc, ClassLoader cl,
			Container container) {
		super(Client.class, sbc, cl, container);

		this.sbc = sbc;
		this.cl = cl;
	}

	public void run() {
		try {
			Thread.currentThread().setContextClassLoader(cl);
			super.run();
		} catch (RuntimeException e) {
			logError(e);
			throw e;
		} catch (Throwable e) {
			logError(e);
			throw new RuntimeException(e);
		}
		try {
			ClientLoginer loginer = (ClientLoginer) newLoginer();
			List<String> loginIdList = loginer.getLoginIdList();
			if (loginIdList != null && loginIdList.size() > 0
					&& !StringUtil.isEmpty(loginIdList.get(0))) {
				for (int l = 0; l < loginIdList.size(); l++) {
					String loginId = loginIdList.get(l);
					if (!StringUtil.isEmpty(loginId)) {
						ClientSession ss = createSession(loginId);
						if (ss != null) {
							startSession(ss);
						}
					}
				}
			} else {
				throw new TechException("LoginIds are undefined");
			}

			while (true) {
				Event event = getEvent();
				if (event != null) {
					if (event instanceof EventInitProcMap) {
						restartCronjobs();
					} else if (event instanceof EventRestartSessionRequired) {
						ClientSession ss = createSession(((EventRestartSessionRequired) event)
								.getLoginId());
						if (ss != null) {
							startSession(ss);
						}
					}
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
			}
		} catch (Throwable e) {
			logError(e);
		}
	}

	public ClientSession createSession(String loginId) throws TechException {
		String remoteIp = getServiceConfig().getDstIp();
		int remotePort = getServiceConfig().getDstPort();

		SessionContext context = new SessionContext(this, remoteIp, remotePort);

		ClientLoginer loginer = (ClientLoginer) newLoginer();
		if (!StringUtil.isEmpty(loginer) && !StringUtil.isEmpty(loginId)) {
			loginer.setMyLoginId(loginId);
		}

		SocketConnection conn = new SocketConnection(context,
				getServiceConfig().getConnectionProps(), getServiceConfig()
						.getRecverClass(), getServiceConfig().getSenderClass());

		ClientSession ss = new ClientSession(context, conn, loginer);
		context.setSession(ss);

		if (loginer != null) {
			context.setLoginId(loginer.getMyLoginId());
		}

		return ss;
	}

	public void killEventHandler() {
		super.killEventHandler();
	}

	public boolean isBusy() {
		return false;
	}

	private void monitor() {
		// int threads = java.lang.Thread.activeCount();
		// int allThreads = ManagementFactory.getThreadMXBean()
		// .getThreadCount();
		//
		// logDebug("Threads: " + threads
		// + " in a current thread's thread group, "
		// + allThreads + " in JVM.");
	}
}
