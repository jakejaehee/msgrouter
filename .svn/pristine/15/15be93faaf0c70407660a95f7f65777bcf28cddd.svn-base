package msgrouter.engine.http.client;

import java.net.URL;
import java.util.List;

import msgrouter.adapter.http.HttpReqMessage;
import msgrouter.adapter.http.HttpResMessage;
import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.interfaces.bean.ClientLoginer;
import msgrouter.constant.Const;
import msgrouter.engine.Container;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.Service;
import msgrouter.engine.Session;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import msgrouter.engine.event.Event;
import msgrouter.engine.event.EventInitProcMap;
import elastic.util.authmanager.AuthEntry;
import elastic.util.authmanager.AuthResult;
import elastic.util.util.BizException;
import elastic.util.util.StringUtil;
import elastic.util.util.TechException;
import elastic.util.web.client.HttpClient;

public class MRHttpClient extends Service implements Runnable {
	private final ServiceBootstrapConfig sbc;
	private final ClassLoader cl;
	private AuthEntry ae = null;

	public MRHttpClient(ServiceBootstrapConfig sbc, ClassLoader cl,
			Container container) {
		super(MRHttpClient.class, sbc, cl, container);

		this.sbc = sbc;
		this.cl = cl;
	}

	public void run() {
		try {
			Thread.currentThread().setContextClassLoader(cl);
			super.run();

			while (true) {
				AuthEntryAdmin aeAdmin = getAuthEntryAdmin();
				if (aeAdmin != null) {
					List<AuthEntry> aeList = aeAdmin.getAuthEntryList();
					if (aeList != null && aeList.size() > 0
							&& !StringUtil.isEmpty(aeList.get(0))) {
						this.ae = aeList.get(0);
					} else {
						throw new TechException("LoginId is undefined.");
					}
				}

				URL url = new URL(getServiceConfig().getDstUrlStr());

				SessionContext context = new SessionContext(this,
						url.getHost(), url.getPort() != -1 ? url.getPort()
								: url.getDefaultPort());

				ClientLoginer loginer = (ClientLoginer) newLoginer();

				MRHttpClientSession ss = new MRHttpClientSession(context,
						loginer, ae);
				context.setSession(ss);

				try {
					String loginId = ae != null ? ae.getId() : null;
					String ip = url.getHost();

					startSession(ss);

					while (true) {
						Event event = getEvent();
						if (event != null) {
							if (event instanceof EventInitProcMap) {
								restartCronjobs();
							}
						}
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
						}
					}
				} catch (Throwable e) {
					logError(e);
				} finally {
					if (ss != null) {
						deregisterSession(ss);
						ss.currentLifeCycle().kill();
					}
				}
			}
		} catch (RuntimeException e) {
			logError(e);
			throw e;
		} catch (Throwable e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public void killEventHandler() {
		super.killEventHandler();
	}

	public boolean isBusy() {
		return false;
	}

	public static boolean login(Session ss, HttpClient httpClient,
			String reqEncoding, AuthEntry myAE) throws Exception {
		HttpReqMessage reqMsg = new HttpReqMessage();
		reqMsg.setMessageType(Const.VAL_MSG_TYPE_LOGIN_REQ);
		reqMsg.setAll(myAE.toMap());
		String queryStr = reqMsg.getQueryString(reqEncoding);

		httpClient.connectInPOST();

		httpClient.sendReqInPOST(queryStr);

		if (ss.logDebugEnabled()) {
			String sentLog = MessageLogger.toDebugingLog("sent", ss, reqMsg);
			if (sentLog != null)
				ss.logDebug(sentLog);
		}

		String resStr = httpClient.recvRes();
		HttpResMessage resMsg = new HttpResMessage();
		resMsg.setContent(resStr);

		if (ss.logDebugEnabled()) {
			String recvLog = MessageLogger.toDebugingLog("recv", ss, resMsg);
			if (recvLog != null)
				ss.logDebug(recvLog);
		}

		String aeResultCd = (String) resMsg.get(AuthResult.KEY_CD);

		if (!AuthResult.CD_0_SUCCESS.equals(aeResultCd)) {
			myAE.put(AuthEntryAdmin.KEY_IP, httpClient.getHostname());
			myAE.put(AuthEntryAdmin.KEY_ONLINE, false);
			throw new BizException((String) resMsg.get(AuthResult.KEY_MSG));
		}

		myAE.put(AuthEntryAdmin.KEY_IP, httpClient.getHostname());
		myAE.put(AuthEntryAdmin.KEY_ONLINE, true);
		return true;
	}
}
