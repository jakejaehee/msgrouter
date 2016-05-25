package msgrouter.api.interfaces.bean;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.interfaces.Message;
import msgrouter.engine.Service;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ServiceConfig;
import elastic.util.util.TechException;

public abstract class ServerLoginer implements Loginer {

	private Service svc = null;
	private ServiceConfig svcConf = null;

	public void setService(Service svc) {
		this.svc = svc;
	}

	public void setServiceConfig(ServiceConfig svcConf) {
		this.svcConf = svcConf;
	}

	public ServiceConfig getServiceConfig() {
		return this.svcConf;
	}

	public AuthEntryAdmin getAuthEntryAdmin() {
		return this.svc.getAuthEntryAdmin();
	}

	public abstract String onMessage(Message firstRecvMsg,
			SessionContext context) throws TechException;
}
