package msgrouter.engine.socket.client;

import java.io.IOException;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.QueueEntry;
import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.bean.ClientLoginer;
import msgrouter.engine.Service;
import msgrouter.engine.Session;
import msgrouter.engine.SessionContext;
import msgrouter.engine.event.EventRestartSessionRequired;
import msgrouter.engine.socket.client.workerthread.ClientAsyncRecvThr;
import msgrouter.engine.socket.client.workerthread.ClientAsyncSendThr;
import elastic.util.lifecycle.LifeCycle;

public class ClientSession extends Session {
	private final IncrementalInterval retryInterval;
	private ClientAsyncSendThr asyncSend = null;
	private ClientAsyncRecvThr asyncRecv = null;
	private SocketConnection conn = null;

	public ClientSession(SessionContext context, SocketConnection conn, ClientLoginer loginer) {
		super(context, loginer, ClientSession.class);

		this.conn = conn;
		this.retryInterval = new IncrementalInterval(10, 60000);
	}

	public void run() {
		try {
			execInitializer();

			while (true) {
				try {
					if (logInfoEnabled())
						logInfo("connecting...");

					conn.connect(getLoginId());

					if (logInfoEnabled())
						logInfo("connected.");

					retryInterval.clear();

					if (getLoginer() != null) {
						QueueEntry qe = ((ClientLoginer) getLoginer()).onConnection(getSessionContext());
						putSQ(qe);
					}
					break;
				} catch (IOException ioe) {
					logWarn(ioe.getMessage());
				} catch (Throwable t) {
					logError(t);
				} finally {
					try {
						Thread.sleep(retryInterval.nextValue());
					} catch (Throwable e) {
					}
				}
			}
			
			if (getService().getServiceConfig().getRoutingTarget() == Service.IVAL_ROUTING_TARGET_MSGROUTER_ID) {
				AuthEntryAdmin aeAdmin = getService().getAuthEntryAdmin();
				if (aeAdmin != null) {
					aeAdmin.setOnLine(getLoginId(), getRemoteIp() + ":" + getRemotePort());
				}
			}

			asyncSend = new ClientAsyncSendThr(getService(), this, conn, (ClientLoginer) getLoginer());
			new LifeCycle(asyncSend, "asyncSendThr", this).start();

			asyncRecv = new ClientAsyncRecvThr(getService(), this, conn, (ClientLoginer) getLoginer());
			new LifeCycle(asyncRecv, "asyncRecvThr", this).start();

			startCronjob();
		} catch (Throwable e) {
			logError(e);
		}
	}

	private volatile boolean kill = false;

	public void killEventHandler() {
		if (kill) {
			return;
		}
		kill = true;

		super.killEventHandler();

		if (conn != null) {
			conn.close();
			conn = null;
			if (logInfoEnabled())
				logInfo("closed connection.");

			AuthEntryAdmin aeAdmin = getService().getAuthEntryAdmin();
			aeAdmin.setOffLine(getLoginId());
		}

		try {
			getService().putEvent(new EventRestartSessionRequired(getLoginId()));
		} catch (Throwable e) {
			logError(e);
		}
	}

	public boolean isBusy() {
		return false;
	}
}
