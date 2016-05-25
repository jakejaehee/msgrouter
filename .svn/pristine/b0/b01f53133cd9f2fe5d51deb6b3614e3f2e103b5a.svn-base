package msgrouter.engine;

import java.io.File;

public class Naming {
	public static String switchQueueKey(Service svc) {
		return svc.getServiceId() + File.separator + "SW.Q";
	}

	public static String recvQueueKey(Service svc, Session ss) {
		String svcId = svc.getServiceId();
		String remoteIp = ss.getRemoteIp();
		String loginId = ss.getLoginId();
		String ssName = ss.getAlias();
		if (loginId != null) {
			return svcId + File.separator + convertIPStr(remoteIp)
					+ File.separator + loginId + "_R.Q";
		} else {
			return svcId + File.separator + convertIPStr(remoteIp)
					+ File.separator + ssName + File.separator + "R.Q";
		}
	}

	public static String sendQueueKey(Service svc, Session ss) {
		String svcId = svc.getServiceId();
		String remoteIp = ss.getRemoteIp();
		String loginId = ss.getLoginId();
		String ssName = ss.getAlias();
		if (loginId != null) {
			return svcId + File.separator + convertIPStr(remoteIp)
					+ File.separator + loginId + "_S.Q";
		} else {
			return svcId + File.separator + convertIPStr(remoteIp)
					+ File.separator + ssName + File.separator + "S.Q";
		}
	}

	private static String convertIPStr(String ip) {
		return ip != null ? ip.replaceAll(":", "_") : "";
	}
}
