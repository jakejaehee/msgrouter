package msgrouter.engine;

import msgrouter.engine.com.workerthread.SessionBeanThr;
import msgrouter.engine.com.workerthread.SessionCronjob;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.pool.loadbalancing.PoolEntry;

public class SessionRegistry extends PoolEntry {
	private Service svc = null;
	private SessionBeanThr ssBeanThr = null;
	private SessionCronjob ssCronjob = null;

	private SessionRegistry(Class implClass) {
		super(implClass);
	}

	public SessionRegistry(Service svc) {
		this(SessionRegistry.class);

		this.svc = svc;
	}

	public void run() {
		ssBeanThr = new SessionBeanThr(this);
		new LifeCycle(ssBeanThr, this.getLifeCyclePath() + "/ssBeanThr").start();

		ssCronjob = new SessionCronjob(this);
		new LifeCycle(ssCronjob, this.getLifeCyclePath() + "/ssCronjob").start();
	}

	public Service getService() {
		return svc;
	}

	public int getLoad() {
		return childCount();
	}

	public int getMaxLoad() {
		return svc.getServiceConfig().getMaxSessionsPerBeanThr();
	}

	public SessionBeanThr getSessionBeanThr() {
		return ssBeanThr;
	}

	public SessionCronjob getSessionCronjob() {
		return ssCronjob;
	}

	public boolean isBusy() {
		return false;
	}

	public void killEventHandler() {
		if (ssBeanThr != null) {
			if (ssBeanThr.currentLifeCycle() != null) {
				ssBeanThr.currentLifeCycle().killNow();
			}
		}
		if (ssCronjob != null) {
			if (ssCronjob.currentLifeCycle() != null) {
				ssCronjob.currentLifeCycle().killNow();
			}
		}
	}
}
