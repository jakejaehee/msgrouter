package msgrouter.api.interfaces.bean;

import java.util.List;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.QueueEntry;
import msgrouter.engine.Service;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ServiceConfig;
import elastic.util.util.TechException;

public abstract class ClientLoginer implements Loginer {

	private String loginId = null;
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

	public String getMyLoginId() {
		return loginId;
	}

	public void setMyLoginId(String loginId) {
		this.loginId = loginId;
	}

	public abstract List<String> getLoginIdList();

	public abstract QueueEntry onConnection(SessionContext context)
			throws TechException;
}
