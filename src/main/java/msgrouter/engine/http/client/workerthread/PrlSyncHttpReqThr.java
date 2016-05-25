package msgrouter.engine.http.client.workerthread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import msgrouter.adapter.http.HttpReqMessage;
import msgrouter.adapter.http.HttpResMessage;
import msgrouter.api.QueueEntry;
import msgrouter.constant.Const;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.Session;
import msgrouter.engine.http.client.MRHttpClient;
import elastic.util.authmanager.AuthEntry;
import elastic.util.authmanager.AuthResult;
import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.web.client.HttpClient;

public class PrlSyncHttpReqThr extends LifeCycleObject implements Runnable {
	private final Session ss;
	private final ExecutorService threadPool;
	private final AuthEntry ae;
	private final String urlStr;
	private final String reqEncoding;
	private QueueEntry sendQE = null;
	private volatile int jobs = 0;

	public PrlSyncHttpReqThr(Session session, AuthEntry ae, String urlStr,
			String reqEncoding) {
		super(PrlSyncHttpReqThr.class);

		this.ss = session;
		this.threadPool = Executors.newFixedThreadPool(1024);
		this.ae = ae;
		this.urlStr = urlStr;
		this.reqEncoding = reqEncoding;

		setLogger(ss.getLogger());
	}

	public void run() {
		try {
			while (!currentLifeCycle().isShutdownMode()
					|| !currentLifeCycle().readyToShutdown()) {
				try {
					if (currentLifeCycle().sleep(Const.VAL_LOOP_SLEEP_MILLIS,
							Const.VAL_LOOP_SLEEP_NANOS)) {
						continue;
					}

					sendQE = (QueueEntry) ss.pollSQ();
					if (sendQE == null) {
						continue;
					}

					threadPool.submit(new ReqAndGetRes(ss, urlStr, reqEncoding,
							sendQE));
				} catch (Exception e) {
					logError(e);
				}
			}
		} finally {
			currentLifeCycle().killParent();
		}
	}

	public void killEventHandler() {
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Const.VAL_WAITING_TO_SHUTDOWN_MILLIS,
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		} finally {
			threadPool.shutdownNow();
		}
	}

	public boolean isBusy() {
		return jobs > 0;
	}

	public class ReqAndGetRes implements Runnable {
		private final Session ss;
		private final String urlStr;
		private final String reqEncoding;
		private final HttpClient httpClient;
		private QueueEntry sendQE = null;

		public ReqAndGetRes(Session ss, String urlStr, String reqEncoding,
				QueueEntry sendQE) {
			this.ss = ss;
			this.urlStr = urlStr;
			this.reqEncoding = reqEncoding;
			this.httpClient = new HttpClient(urlStr, reqEncoding);
			this.sendQE = sendQE;
		}

		public void run() {
			jobs++;
			try {
				while (sendQE != null) {
					HttpReqMessage sendMsg = (HttpReqMessage) sendQE
							.getMessage(0);
					if (sendMsg == null) {
						if (sendQE.getMessageCount() == 1) {
							sendQE = null;
							return;
						}
						sendQE.removeMessage(0);
						continue;
					}

					httpClient.connectInPOST();

					String queryString = sendMsg.getQueryString(reqEncoding);
					httpClient.sendReqInPOST(queryString);

					if (logDebugEnabled()) {
						String log = MessageLogger.toDebugingLog("sent", ss,
								sendMsg);
						if (log != null)
							logDebug(log);
					}

					if (sendQE.getMessageCount() == 1) {
						sendQE = null;
					} else {
						sendQE.removeMessage(0);
					}

					String resStr = httpClient.recvRes();
					HttpResMessage resMsg = new HttpResMessage();
					resMsg.setContent(resStr);

					if (logDebugEnabled()) {
						String recvLog = MessageLogger.toDebugingLog("recv",
								ss, resMsg);
						if (recvLog != null)
							logDebug(recvLog);
					}

					String msgType = resMsg.getMessageType();

					if (Const.VAL_MSG_TYPE_LOGIN_REQ.equals(msgType)) {
						String aeResultCd = (String) resMsg
								.get(AuthResult.KEY_CD);
						if (AuthResult.CD_0_SUCCESS.equals(aeResultCd)) {
						} else if (AuthResult.CD_ERR_101_NULL_AE
								.equals(aeResultCd)
								|| AuthResult.CD_ERR_102_NULL_ID
										.equals(aeResultCd)) {
							MRHttpClient.login(ss, httpClient, reqEncoding, ae);
						}
					} else {
						QueueEntry recvQE = new QueueEntry();
						recvQE.addMessage(resMsg);
						ss.putRQ(recvQE);
					}
				}
			} catch (Exception e) {
				logError(e);
			} finally {
				jobs--;
				if (httpClient != null) {
					try {
						httpClient.close();
						logInfo("closed connection: " + getLifeCyclePath());
					} catch (Exception e) {
						logError(e);
					}
				}
			}
		}
	}
}
