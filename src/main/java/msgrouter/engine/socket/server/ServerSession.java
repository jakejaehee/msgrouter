package msgrouter.engine.socket.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import elastic.util.util.TechException;
import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.MessageUtil;
import msgrouter.api.QueueEntry;
import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.bean.Loginer;
import msgrouter.api.interfaces.bean.SentTrigger;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Session;
import msgrouter.engine.SessionContext;
import msgrouter.engine.queue.QueueTimeoutException;

public class ServerSession extends Session {
	private SocketChannel channel = null;
	private SelectionKey selKey = null;
	private SocketConnection conn = null;
	private TransferSelectorThr selThr = null;
	private volatile QueueEntry sendQE = null;
	private QueueEntry recvQE = null;

	public ServerSession(SessionContext ssContext, Loginer loginer, SocketConnection conn) {
		super(ssContext, loginer, ServerSession.class);

		this.conn = conn;
	}

	public void run() {
		try {
			execInitializer();
			startCronjob();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setSelectorInfo(TransferSelectorThr selThr, SelectionKey selKey) {
		this.selThr = selThr;
		this.selKey = selKey;
		this.channel = (SocketChannel) selKey.channel();
	}

	public TransferSelectorThr getSelectorThr() {
		return selThr;
	}

	public SelectionKey getSelectionKey() {
		return selKey;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public boolean isBusy() {
		return recvQE == null || sendQE == null;
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

		SelectionKey selKey = getSelectionKey();
		((Server) getService()).cancelSelectionKey(selKey, true);
	}

	public SocketConnection getSocketConnection() {
		return conn;
	}

	public void handleRecvMessage(Message msg) throws TechException, QueueTimeoutException {
		if (logDebugEnabled()) {
			String log = MessageLogger.toDebugingLog("recv", this, msg);
			if (log != null)
				logDebug(log);
		}

		recvQE = new QueueEntry();
		recvQE.addMessage(msg);

		putRQ(recvQE);
		recvQE = null;
	}

	public Message recv() throws IOException {
		try {
			return conn.getRecver().recv();
		} catch (IOException e) {
			logError(e);
			throw e;
		} catch (Throwable t) {
			logError(t);
			throw new RuntimeException(t);
		}
	}

	public void send() throws IOException, TechException {
		try {
			sendQE = peekSQ();
			while (sendQE != null) {
				Message msg = sendQE.getMessage(0);
				if (msg != null) {
					conn.getSender().send(msg);
					if (logDebugEnabled()) {
						String log = MessageLogger.toDebugingLog("sent", this, msg);
						if (log != null)
							logDebug(log);
					}
					sendQE.removeMessage(0);
				}
				if (sendQE.getMessageCount() == 0)
					pollSQ();
				if (msg != null) {
					if (sendQE.getSentTriggerClass() != null) {
						try {
							SentTrigger trigger = (SentTrigger) MsgRouter.getInstance().getBeanCache()
									.getBean(getService(), this, sendQE.getSentTriggerClass());
							if (logDebugEnabled())
								logDebug(SentTrigger.class.getSimpleName() + " "
										+ sendQE.getSentTriggerClass().getName() + " for " + MessageUtil.msgStr(msg));
							trigger.execute(msg);
						} catch (Throwable t) {
							logError(t);
						}
					}
				}
				if (sendQE.getMessageCount() == 0)
					sendQE = null;
			}
		} catch (IOException e) {
			logError(e);
			throw e;
		} catch (TechException e) {
			logError(e);
			throw e;
		} catch (Throwable t) {
			logError(t);
			throw new TechException(t);
		} finally {
			sendQE = null;
		}
	}
}