package msgrouter.engine.nosession;

import msgrouter.engine.Container;
import msgrouter.engine.Service;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;

import org.apache.log4j.Logger;

public class NoSessionService extends Service implements Runnable {
	private static final Logger LOG = Logger.getLogger(NoSessionService.class);

	private final ServiceBootstrapConfig sbc;
	private final ClassLoader cl;

	public NoSessionService(ServiceBootstrapConfig sbc, ClassLoader cl,
			Container container) {
		super(NoSessionService.class, sbc, cl, container);

		this.sbc = sbc;
		this.cl = cl;
	}

	public void run() {
		try {
			Thread.currentThread().setContextClassLoader(cl);

			super.run();

			while (true) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void killEventHandler() {
		super.killEventHandler();
	}

	public boolean isBusy() {
		return false;
	}
}
