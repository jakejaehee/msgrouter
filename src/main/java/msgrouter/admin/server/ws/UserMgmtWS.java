package msgrouter.admin.server.ws;

import java.io.IOException;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;

import org.apache.log4j.Logger;

import elastic.util.authmanager.data.GroupConfig;
import elastic.util.util.TechException;

public class UserMgmtWS {
	private static final Logger LOG = Logger.getLogger(UserMgmtWS.class);

	public String getAuthMgrConfText(String svcId) throws IOException {
		Service svc = MsgRouter.getInstance().getService(svcId);
		AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
		return aeAdmin != null ? aeAdmin.readAuthMgrConfText() : null;
	}

	public String getUserDatasText(String svcId) throws IOException {
		Service svc = MsgRouter.getInstance().getService(svcId);
		AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
		return aeAdmin != null ? aeAdmin.readUserDatasText() : null;
	}

	public void saveUserDatasText(String svcId, String userDatasText)
			throws TechException {
		Service svc = MsgRouter.getInstance().getService(svcId);
		AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
		if (aeAdmin != null) {
			aeAdmin.writeUserDatasText(userDatasText);
		}
	}

	public String getGroupDataText(String svcId, String groupId) {
		try {
			Service svc = MsgRouter.getInstance().getService(svcId);
			AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
			if (aeAdmin != null) {
				return aeAdmin.readGroupDataText(groupId);
			}
		} catch (IOException e) {
			LOG.warn(e.getMessage());
		}
		return null;
	}

	public void saveGroupDataText(String svcId, String amConfText,
			String groupId, String groupDataText) throws TechException {
		Service svc = MsgRouter.getInstance().getService(svcId);
		AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
		if (aeAdmin != null) {
			aeAdmin.writeAuthMgrConfText(amConfText);
			GroupConfig grpConf = aeAdmin.getGroupConfig(groupId);
			if (grpConf != null) {
				aeAdmin.writeGroupDataText(groupId, grpConf.getDescription(),
						groupDataText, grpConf.getEncoding());
			}
		}
	}

	public void deleteGroupDataText(String svcId, String amConfText,
			String groupId) throws TechException {
		Service svc = MsgRouter.getInstance().getService(svcId);
		AuthEntryAdmin aeAdmin = svc.getAuthEntryAdmin();
		if (aeAdmin != null) {
			aeAdmin.writeAuthMgrConfText(amConfText);
			aeAdmin.deleteGroupDataText(groupId);
		}
	}
}
