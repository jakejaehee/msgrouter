package msgrouter.api.interfaces.bean;

import java.io.Serializable;

import elastic.util.concurrent.LockMgr;
import msgrouter.engine.Service;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ServiceConfig;

public abstract class Bean implements Serializable {

	private static final long serialVersionUID = 4535539062642805960L;

	private Service svc = null;

	public final void setService(Service svc) {
		this.svc = svc;
	}

	public final ServiceConfig getServiceConfig() {
		return this.svc.getServiceConfig();
	}

	public final LockMgr getLockMgrOfService() {
		return svc.getLockMgr();
	}

	public abstract void setSessionContext(SessionContext ssContext);

	public int getNrOfSessions() {
		return svc.getNrOfSessions();
	}

	public int getNrOfConnections() {
		return svc.getNrOfConnections();
	}
}
