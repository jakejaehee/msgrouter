package msgrouter.api.resource;

import java.util.List;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;
import elastic.util.authmanager.AuthEntry;

public class LoginIds {
	private Service service = null;

	public LoginIds() {
	}

	public LoginIds(Service svc) {
		this.service = svc;
	}

	public String[] getLoginIds(String svcId) {
		Service svc = null;
		if (service != null && service.getServiceId().equals(svcId)) {
			svc = service;
		} else {
			svc = MsgRouter.getInstance().getService(svcId);
		}
		if (svc != null) {
			AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
			if (aeAdmin != null) {
				return aeAdmin.getLoginIds();
			}
		}
		return new String[0];
	}

	public List<AuthEntry> getAuthEntryList(String svcId) {
		Service svc = null;
		if (service != null && service.getServiceId().equals(svcId)) {
			svc = service;
		} else {
			svc = MsgRouter.getInstance().getService(svcId);
		}
		if (svc != null) {
			AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
			if (aeAdmin != null) {
				return aeAdmin.getAuthEntryList();
			}
		}
		return null;
	}

	public boolean isAvailableLoginId(String svcId, String loginId) {
		Service svc = null;
		if (service != null && service.getServiceId().equals(svcId)) {
			svc = service;
		} else {
			svc = MsgRouter.getInstance().getService(svcId);
		}
		if (svc != null) {
			AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
			if (aeAdmin != null) {
				return aeAdmin.isAvailableLoginId(loginId);
			}
		}
		return false;
	}
}
