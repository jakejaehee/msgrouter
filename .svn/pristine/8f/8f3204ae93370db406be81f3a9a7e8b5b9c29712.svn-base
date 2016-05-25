package msgrouter.engine.nosession;

import java.util.Iterator;

import msgrouter.constant.Const;
import msgrouter.engine.Container;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;
import msgrouter.engine.config.Config;
import msgrouter.engine.config.ContainersConfig;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;

import org.apache.log4j.Logger;

import elastic.util.java.ClassLoaderUtil;
import elastic.util.java.ReflectionUtil;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.util.TechException;

public class NoSessionContainer extends Container {
	private static final Logger LOG = Logger
			.getLogger(NoSessionContainer.class);

	private Config config = null;

	private static volatile NoSessionContainer _instance = null;

	public static NoSessionContainer getInstance(Config config)
			throws TechException {
		if (_instance == null) {
			synchronized (NoSessionContainer.class) {
				if (_instance == null) {
					_instance = new NoSessionContainer(config);
				}
			}
		}
		return _instance;
	}

	private NoSessionContainer(Config config) throws TechException {
		super(NoSessionContainer.class);

		this.config = config;
	}

	public void run() {
		try {
			Iterator<ServiceBootstrapConfig> it = config
					.getContainersConfig()
					.getServiceBootstrapConfigList(
							Const.CONF_CONTAINER_NOSESSION).iterator();
			logTrace(NoSessionContainer.class.getSimpleName() + " starts with "
					+ it);
			while (it.hasNext()) {
				ServiceBootstrapConfig sbc = it.next();
				startService(sbc);
			}
		} catch (TechException e) {
			throw new RuntimeException(e);
		}
	}

	public void startService(ServiceBootstrapConfig sbc) throws TechException {
		if (!currentLifeCycle().isShutdownMode()) {
			try {
				ContainersConfig containersConfig = MsgRouter.getInstance()
						.getConfig().getContainersConfig();

				ClassLoader cl = ClassLoaderUtil.getClassLoaderByClassPath(
						containersConfig.getCurrentDir(), sbc.getClasspath()
								+ ";classes;lib", Thread.currentThread()
								.getContextClassLoader());
				Service svc = (Service) ReflectionUtil.newInstance(
						NoSessionService.class.getName(),
						new Class[] { ServiceBootstrapConfig.class,
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
