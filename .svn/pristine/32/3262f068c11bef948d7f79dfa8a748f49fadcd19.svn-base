package msgrouter.engine.socket;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import msgrouter.constant.Const;
import msgrouter.engine.Container;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;
import msgrouter.engine.config.Config;
import msgrouter.engine.config.ContainersConfig;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import elastic.util.java.ClassLoaderUtil;
import elastic.util.java.ReflectionUtil;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.util.TechException;

public class SocketContainer extends Container {
	private Config config = null;
	private boolean clientModule = false;

	private static volatile SocketContainer _instance = null;

	public static SocketContainer getInstance(Config config,
			boolean clientModule) throws TechException {
		if (_instance == null) {
			synchronized (SocketContainer.class) {
				if (_instance == null) {
					_instance = new SocketContainer(config, clientModule);
				}
			}
		}
		return _instance;
	}

	private SocketContainer(Config config, boolean clientModule)
			throws TechException {
		super(SocketContainer.class);

		this.config = config;
		this.clientModule = clientModule;
	}

	public void run() {
		try {
			Iterator<ServiceBootstrapConfig> it = config.getContainersConfig()
					.getServiceBootstrapConfigList(Const.CONF_CONTAINER_SOCKET)
					.iterator();
			logTrace(SocketContainer.class.getSimpleName() + " starts with "
					+ it);
			while (it.hasNext()) {
				ServiceBootstrapConfig sbc = it.next();
				startService(sbc);
			}
		} catch (TechException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startService(ServiceBootstrapConfig sbc) throws IOException,
			TechException {
		if (!currentLifeCycle().isShutdownMode()) {
			try {
				ContainersConfig containersConfig = MsgRouter.getInstance()
						.getConfig().getContainersConfig();

				String svcClassName = null;
				if (!clientModule
						&& sbc.getServiceType() == Service.IVAL_SVC_TYPE_SERVER
						&& Const.SERVER) {
					svcClassName = msgrouter.engine.socket.server.Server.class
							.getName();
				} else if (sbc.getServiceType() == Service.IVAL_SVC_TYPE_CLIENT) {
					svcClassName = msgrouter.engine.socket.client.Client.class
							.getName();
				}

				ClassLoader cl = ClassLoaderUtil.getClassLoaderByClassPath(
						containersConfig.getCurrentDir(), sbc.getClasspath()
								+ ";" + sbc.getServicePath() + File.separator
								+ "classes;" + sbc.getServicePath()
								+ File.separator + "lib", Thread
								.currentThread().getContextClassLoader());

				Service svc = (Service) ReflectionUtil.newInstance(
						svcClassName, new Class[] {
								ServiceBootstrapConfig.class,
								ClassLoader.class, getClass() }, new Object[] {
								sbc, cl, this }, cl);

				new LifeCycle(svc, sbc.getServiceId(), this).start();
			} catch (Exception e) {
				throw new TechException(e);
			}
		}
	}

	public void killEventHandler() {
	}

	public boolean isBusy() {
		return false;
	}
}
