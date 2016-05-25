package msgrouter.engine.socket.client.workerthread;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import msgrouter.api.QueueEntry;
import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.adapter.MessageRecver;
import msgrouter.api.interfaces.bean.ClientLoginer;
import msgrouter.constant.Const;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.Service;
import msgrouter.engine.socket.client.ClientSession;
import elastic.util.lifecycle.LifeCycleObject;

public class ClientAsyncRecvThr extends LifeCycleObject implements Runnable {
	private final ClientSession ss;
	private final SocketConnection conn;

	private QueueEntry recvQE = null;

	public ClientAsyncRecvThr(Service svc, ClientSession session,
			SocketConnection conn, ClientLoginer loginer) {
		super(ClientAsyncRecvThr.class);

		this.ss = session;
		this.conn = conn;

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

					MessageRecver reader = conn.getRecver();
					Message recvMsg = reader.recv();

					if (recvMsg != null) {
						recvQE = new QueueEntry();
						recvQE.addMessage(recvMsg);

						if (logDebugEnabled()) {
							String log = MessageLogger.toDebugingLog("recv",
									ss, recvMsg);
							if (log != null)
								logDebug(log);
						}

						ss.putRQ(recvQE);
					}
					recvQE = null;
				} catch (SocketTimeoutException e) {
					logDebug(e.toString());
				} catch (ConnectException e) {
					logError(e);
					return;
				} catch (SocketException e) {
					logError(e);
					return;
				} catch (IOException e) {
					logError(e);
					return;
				} catch (Throwable e) {
					logError(e);
				}
			}
		} finally {
			currentLifeCycle().killParent();
		}
	}

	public void killEventHandler() {
	}

	public boolean isBusy() {
		return recvQE != null;
	}
}
