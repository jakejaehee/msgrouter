package msgrouter.api.interfaces.bean;

import msgrouter.api.QueueEntry;
import msgrouter.api.SyncQueue;
import msgrouter.api.interfaces.Message;
import msgrouter.engine.Router;
import msgrouter.engine.queue.QueueTimeoutException;

import org.apache.log4j.Logger;

import elastic.util.util.TechException;

public abstract class SyncBean extends Bean {
	private static final Logger LOG = Logger.getLogger(SyncBean.class);

	private static final long serialVersionUID = -7985877911337442538L;

	private volatile boolean waiting = false;
	private transient volatile Message recvMsg = null;
	private transient volatile Thread owner = null;

	public final void setWaiting() {
		waiting = true;
	}

	public final boolean isRecvEnabled() {
		return waiting || recvMsg != null;
	}

	public final void putRecvMessage(Message recvMsg) {
		this.waiting = false;
		this.recvMsg = recvMsg;
	}

	public final Message pollRecvMessage() {
		Message tmp = recvMsg;
		recvMsg = null;
		return tmp;
	}

	public final boolean isRunning() {
		return owner != null;
	}

	public synchronized final void _execute(SyncQueue syncQueue)
			throws TechException, QueueTimeoutException {
		try {
			this.owner = Thread.currentThread();
			execute(syncQueue);
		} finally {
			this.owner = null;
		}
	}

	public abstract void execute(SyncQueue syncQueue) throws TechException,
			QueueTimeoutException;

	protected final void switchTo(String dstSvcId, QueueEntry qe)
			throws TechException, QueueTimeoutException {
		Router.switchTo(qe, dstSvcId);
	}
}
