package msgrouter.engine.http.client;

import java.io.IOException;

import msgrouter.api.interfaces.bean.Loginer;
import msgrouter.engine.Session;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ServiceConfig;
import msgrouter.engine.http.client.workerthread.PrlSyncHttpReqThr;
import msgrouter.engine.http.client.workerthread.SrlSyncHttpReqThr;
import elastic.util.authmanager.AuthEntry;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.util.TechException;

public class MRHttpClientSession extends Session {
	private final AuthEntry ae;

	public MRHttpClientSession(SessionContext context, Loginer loginer,
			AuthEntry ae) {
		super(context, loginer, MRHttpClientSession.class);

		this.ae = ae;
	}

	public void run() {
		try {
			execInitializer();

			if (!currentLifeCycle().isShutdownMode()) {
				invokeWorkerThreads(ae);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void invokeWorkerThreads(AuthEntry ae) throws TechException,
			IOException {
		ServiceConfig svcConf = getService().getServiceConfig();

		if (true) {
			PrlSyncHttpReqThr requester = new PrlSyncHttpReqThr(this, ae,
					svcConf.getDstUrlStr(), svcConf.getReqEncoding());
			new LifeCycle(requester, "prlHttpReqThr", this).start();
		} else {
			SrlSyncHttpReqThr requester = new SrlSyncHttpReqThr(this, ae,
					svcConf.getDstUrlStr(), svcConf.getReqEncoding());
			new LifeCycle(requester, "srlHttpReqThr", this).start();
		}

		startCronjob();
	}

	private volatile boolean kill = false;

	public void killEventHandler() {
		if (kill) {
			return;
		}
		kill = true;
		super.killEventHandler();
	}

	public boolean isBusy() {
		return false;
	}
}