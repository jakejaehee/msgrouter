package msgrouter.engine.http.client.workerthread;

import java.io.IOException;
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

public class SrlSyncHttpReqThr extends LifeCycleObject implements Runnable {
	private final Session ss;
	private final AuthEntry ae;
	private final String urlStr;
	private final String reqEncoding;
	private final ExecutorService threadPool;
	private boolean busy = false;

	public SrlSyncHttpReqThr(Session session, AuthEntry ae, String urlStr,
			String reqEncoding) throws IOException {
		super(SrlSyncHttpReqThr.class);

		this.ss = session;
		this.ae = ae;
		this.urlStr = urlStr;
		this.reqEncoding = reqEncoding;
		this.threadPool = Executors.newFixedThreadPool(1024);
		
		setLogger(ss.getLogger());
	}

	public void run() {
		HttpClient httpClient = null;

		try {
			httpClient = new HttpClient(urlStr, reqEncoding);
			httpClient.connectInPOST();

			while (!currentLifeCycle().isShutdownMode()
					|| !currentLifeCycle().readyToShutdown()) {
				try {
					if (currentLifeCycle().sleep(Const.VAL_LOOP_SLEEP_MILLIS,
							Const.VAL_LOOP_SLEEP_NANOS)) {
						continue;
					}

					QueueEntry sendQE = (QueueEntry) ss.peekSQ();
					if (sendQE == null)
						continue;
					busy = true;

					HttpReqMessage sendMsg = (HttpReqMessage) sendQE
							.getMessage(0);
					if (sendMsg != null) {
						String queryString = sendMsg
								.getQueryString(reqEncoding);
						httpClient.sendReqInPOST(queryString);

						if (logDebugEnabled()) {
							String log = MessageLogger.toDebugingLog("sent",
									ss, sendMsg);
							if (log != null)
								logDebug(log);
						}
					}

					if (sendQE.getMessageCount() > 0)
						sendQE.removeMessage(0);

					if (sendQE.getMessageCount() == 0)
						ss.pollSQ();

					busy = false;
					if (sendMsg != null) {
						String resStr = httpClient.recvRes();
						HttpResMessage resMsg = new HttpResMessage();
						resMsg.setContent(resStr);

						if (logDebugEnabled()) {
							String log = MessageLogger.toDebugingLog("recv",
									ss, resMsg);
							if (log != null)
								logDebug(log);
						}

						if (Const.VAL_MSG_TYPE_LOGIN_REQ.equals(resMsg
								.getMessageType())) {
							String aeResultCd = (String) resMsg
									.get(AuthResult.KEY_CD);
							if (AuthResult.CD_0_SUCCESS.equals(aeResultCd)) {
							} else if (AuthResult.CD_ERR_101_NULL_AE
									.equals(aeResultCd)
									|| AuthResult.CD_ERR_102_NULL_ID
											.equals(aeResultCd)) {
								MRHttpClient.login(ss, httpClient, reqEncoding,
										ae);
							}
						} else {
							QueueEntry recvQE = new QueueEntry();
							recvQE.addMessage(resMsg);
							ss.putRQ(recvQE);
						}
					}
				} catch (Throwable e) {
					logError(e);
				} finally {
				}
			} // while
		} catch (IOException e) {
			logError(e);
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
					logInfo("closed connection");
				} catch (Throwable e) {
					logError(e);
				}
			}
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
		return busy;
	}
}
