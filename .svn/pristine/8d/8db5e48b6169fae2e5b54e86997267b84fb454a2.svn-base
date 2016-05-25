package msgrouter.api;

import java.io.IOException;
import java.util.Map;

import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.bean.SyncBean;
import msgrouter.engine.Session;
import msgrouter.engine.com.workerthread.SessionBeanThr;
import msgrouter.engine.com.workerthread.SessionCronjob;
import msgrouter.engine.config.ServiceConfig;
import msgrouter.engine.queue.QueueTimeoutException;

import org.apache.log4j.Logger;

import elastic.util.util.TechException;

public class SyncQueue {
	private static final Logger LOG = Logger.getLogger(SyncQueue.class);

	private Session ss = null;
	private Runnable workerThr = null;
	private SyncBean syncBean = null;
	private int recvTimeoutMillis = 0;

	public SyncQueue(Session ss, Runnable workerThr, SyncBean syncBean) {
		this.ss = ss;
		this.workerThr = workerThr;
		this.syncBean = syncBean;

		Map props = ss.getService().getServiceConfig().getConnectionProps();
		if (props != null) {
			if (props.containsKey(ServiceConfig.CONN_PROP_RECV_TIMEOUT_MILLIS)) {
				this.recvTimeoutMillis = (Integer) props
						.get(ServiceConfig.CONN_PROP_RECV_TIMEOUT_MILLIS);
			}
		}
		if (recvTimeoutMillis <= 0) {
			recvTimeoutMillis = 60000 * 10;
		}
	}

	public final boolean isRecvEnabled() {
		return !ss.isClosed() && syncBean.isRecvEnabled();
	}

	public final boolean isSendEnabled() {
		return !ss.isClosed() && !isRecvEnabled();
	}

	public final void send(Message msg) throws TechException,
			QueueTimeoutException {
		if (ss.isClosed()) {
			throw new TechException(Session.class.getSimpleName()
					+ " is closed");
		}

		long begin = System.currentTimeMillis();
		while (!isSendEnabled()) {
			if ((System.currentTimeMillis() - begin) > recvTimeoutMillis) {
				throw new TechException("TIMEOUT("
						+ (System.currentTimeMillis() - begin) + " ms): "
						+ syncBean.getClass().getName() + ".send()");
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
		QueueEntry qe = new QueueEntry();
		if (workerThr instanceof SessionCronjob) {
			qe.setReqres(QueueEntry.IVAL_REQ_SESSION_CRONJOB);
		} else if (workerThr instanceof SessionBeanThr) {
			qe.setReqres(QueueEntry.IVAL_RES_SESSION_BEAN_THR);
		}
		qe.setSrcId(ss.getLoginId());
		qe.setSrcIp(ss.getRemoteIp());
		qe.setSrcSessionName(ss.getAlias());
		qe.addMessage(msg);
		ss.putSQ(qe);
		syncBean.setWaiting();
	}

	/**
	 * Blocking mode, but if isRecvEnabled() returns false then return
	 * immediately.
	 * 
	 * @return
	 * @throws IOException
	 */
	public final Message recv() throws TechException {
		if (ss.isClosed()) {
			throw new TechException(Session.class.getSimpleName()
					+ " is closed");
		}

		if (!isRecvEnabled()) {
			return null;
		}
		long begin = System.currentTimeMillis();
		Message msg = null;
		while ((msg = syncBean.pollRecvMessage()) == null) {
			if ((System.currentTimeMillis() - begin) > recvTimeoutMillis) {
				throw new TechException("TIMEOUT("
						+ (System.currentTimeMillis() - begin) + " ms): "
						+ syncBean.getClass().getName() + ".recv()");
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
		return msg;
	}
}
