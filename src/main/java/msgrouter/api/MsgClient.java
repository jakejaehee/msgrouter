package msgrouter.api;

import msgrouter.engine.Session;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.util.TechException;

public class MsgClient {
	private static MsgClient instance = null;

	private MsgRouter msgrouter = null;

	private MsgClient() {
	}

	public final static synchronized MsgClient getInstance()
			throws TechException {
		if (instance == null) {
			instance = new MsgClient();
		}
		return instance;
	}

	public final synchronized void start() throws TechException {
		if (msgrouter == null) {
			msgrouter = MsgRouter.getInstance(true);
			new LifeCycle(msgrouter).start();
		} else {
			throw new TechException(MsgClient.class.getSimpleName()
					+ " already started.");
		}
	}

	public final void stop() {
		if (msgrouter != null) {
			msgrouter.currentLifeCycle().kill();
			msgrouter = null;
		}
	}

	public final QueueConnection getQueueConnection(String svcId)
			throws TechException {
		Service svc = msgrouter.getService(svcId);
		if (svc != null) {
			Session ss = svc.getFirstSession();
			return ss != null ? new QueueConnection(ss) : null;
		}
		return null;
	}
}
