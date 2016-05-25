package msgrouter.engine.http;

import java.io.IOException;
import java.util.Iterator;

import msgrouter.constant.Const;
import msgrouter.engine.Container;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;
import msgrouter.engine.config.Config;
import msgrouter.engine.config.ContainersConfig;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import msgrouter.engine.nosession.NoSessionContainer;
import elastic.util.java.ClassLoaderUtil;
import elastic.util.java.ReflectionUtil;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.util.TechException;

public class HttpContainer extends Container {
	private Config config = null;

	private static volatile HttpContainer _instance = null;

	public static HttpContainer getInstance(Config config) throws TechException {
		if (_instance == null) {
			synchronized (HttpContainer.class) {
				if (_instance == null) {
					_instance = new HttpContainer(config);
				}
			}
		}
		return _instance;
	}

	private HttpContainer(Config config) throws TechException {
		super(HttpContainer.class);

		this.config = config;
	}

	public void run() {
		try {
			Iterator<ServiceBootstrapConfig> it = config.getContainersConfig()
					.getServiceBootstrapConfigList(Const.CONF_CONTAINER_HTTP)
					.iterator();
			logTrace(NoSessionContainer.class.getSimpleName() + " starts with "
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

				ClassLoader cl = ClassLoaderUtil.getClassLoaderByClassPath(
						containersConfig.getCurrentDir(), sbc.getClasspath()
								+ ";classes;lib", Thread.currentThread()
								.getContextClassLoader());

				String svcClassName = null;
				if (sbc.getServiceType() == Service.IVAL_SVC_TYPE_SERVER
						&& Const.SERVER) {
					svcClassName = "msgrouter.engine.http.server.MRHttpServer";
				} else if (sbc.getServiceType() == Service.IVAL_SVC_TYPE_CLIENT) {
					svcClassName = "msgrouter.engine.http.client.MRHttpClient";
				}
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
