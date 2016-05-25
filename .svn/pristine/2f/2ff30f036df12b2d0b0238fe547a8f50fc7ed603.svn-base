package msgrouter.engine;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.bean.ServerLoginer;
import elastic.util.authmanager.AuthEntry;
import elastic.util.authmanager.AuthResult;
import elastic.util.util.TechException;

public class MRServerLoginer extends ServerLoginer {

	public String onMessage(Message msg, SessionContext context)
			throws TechException {

		AuthEntryAdmin aeAdmin = context.getService().getAuthEntryAdmin();
		if (aeAdmin == null) {
			throw new TechException(AuthEntryAdmin.class.getSimpleName()
					+ " is not defined");
		}

		String loginId = (String) msg.get(AuthEntry.KEY_ID);
		String password = (String) msg.get(AuthEntry.KEY_PW);

		if (loginId == null) {
			throw new TechException("loginId is null.");
		}

		AuthResult aeResult = aeAdmin.checkAndSetState(loginId, password,
				context.getRemoteIp() + ":" + context.getRemotePort());

		if (!AuthResult.CD_0_SUCCESS.equals((String) aeResult
				.get(AuthResult.KEY_CD))) {
			throw new TechException((String) aeResult.get(AuthResult.KEY_MSG));
		}
		return loginId;
	}
}
