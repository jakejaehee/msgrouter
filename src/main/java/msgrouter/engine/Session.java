package msgrouter.engine;

import elastic.util.lifecycle.LifeCycle;
import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.util.TechException;
import msgrouter.api.MessageUtil;
import msgrouter.api.QueueEntry;
import msgrouter.api.interfaces.bean.Loginer;
import msgrouter.engine.queue.PersistentQueue;
import msgrouter.engine.queue.Queue;
import msgrouter.engine.queue.QueueParams;
import msgrouter.engine.queue.QueueTimeoutException;
import msgrouter.engine.queue.SynchronizedQueue;

public abstract class Session extends LifeCycleObject {
	private final SessionContext context;
	private final Loginer loginer;

	private volatile Queue<QueueEntry> rq = null;
	private volatile Queue<QueueEntry> sq = null;
	private long lastIO = 0L;

	public Session(SessionContext context, Loginer loginer, Class implClass) {
		super(implClass);

		this.context = context;
		this.loginer = loginer;
		this.lastIO = System.currentTimeMillis();

		setLogger(context.getService().getLogger());
	}

	public SessionContext getSessionContext() {
		return context;
	}

	public Loginer getLoginer() {
		return loginer;
	}

	public void execInitializer() {
		if (context.getService().getServiceConfig().getSessionInitializerClass() != null) {
			try {
				SessionInitializer ssInit = (SessionInitializer) context.getService().getServiceConfig()
						.getSessionInitializerClass().newInstance();
				ssInit.setService(context.getService());
				ssInit.setSession(this);

				if (logDebugEnabled())
					logDebug(ssInit.getClass().getName() + " is executing...");

				ssInit.execute();

				if (logDebugEnabled())
					logDebug(ssInit.getClass().getName() + " is done.");

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public final boolean cronjobRunning() {
		SessionRegistry ssRegistry = (SessionRegistry) getParent();
		if (ssRegistry != null) {
			return ssRegistry.getSessionCronjob().cronjobRunning(this);
		}
		return false;
	}

	public final void stopCronjob() {
		if (cronjobRunning()) {
			SessionRegistry ssRegistry = (SessionRegistry) getParent();
			if (ssRegistry != null) {
				ssRegistry.getSessionCronjob().stopCronjob(this);
			}
		}
	}

	public final void startCronjob() {
		if (!cronjobRunning()) {
			SessionRegistry ssRegistry = (SessionRegistry) getParent();
			if (ssRegistry != null) {
				ssRegistry.getSessionCronjob().startCronjob(this);
			}
		}
	}

	public final void restartCronjob() {
		stopCronjob();
		startCronjob();
	}

	public final void putRQ(QueueEntry qe) throws TechException, QueueTimeoutException {
		if (qe == null) {
			return;
		}
		lastIO = System.currentTimeMillis();

		qe.setReqres(QueueEntry.IVAL_REQ_SESSION_IN);
		qe.setSrcId(getLoginId());
		qe.setSrcIp(getRemoteIp());
		qe.setSrcSessionName(getAlias());

		Queue rq = getRecvQueue();
		rq.put(qe);
		if (logDebugEnabled())
			logDebug("rq: entries=" + rq.size() + ", put " + MessageUtil.qeStr(qe));
	}

	public final QueueEntry pollRQ() throws TechException {
		if (rq != null) {
			QueueEntry qe = rq.poll();

			if (qe != null)
				if (logDebugEnabled())
					logDebug("rq: entries=" + rq.size() + ", got " + MessageUtil.qeStr(qe));

			if (qe == null && rq.size() > 0)
				logWarn("rq: entries=" + rq.size() + ", got null");

			return qe;
		}
		return null;
	}

	public final void putSQ(QueueEntry qe) throws TechException, QueueTimeoutException {
		if (qe == null
				|| (qe.getReqres() == QueueEntry.IVAL_REQ_SESSION_IN && getAlias().equals(qe.getSrcSessionName()))) {
			return;
		}
		Queue sq = getSendQueue();
		sq.put(qe);

		if (logDebugEnabled())
			logDebug("sq: entries=" + sq.size() + ", put " + MessageUtil.qeStr(qe));
	}

	public final QueueEntry pollSQ() throws TechException {
		if (sq != null) {
			QueueEntry qe = sq.poll();
			if (qe != null) {
				lastIO = System.currentTimeMillis();
				if (logDebugEnabled())
					logDebug("sq: entries=" + sq.size() + ", got " + MessageUtil.qeStr(qe));
			}
			return qe;
		}
		return null;
	}

	public final QueueEntry peekSQ() throws TechException {
		if (sq != null) {
			QueueEntry qe = sq.peek();
			if (qe != null) {
				if (logDebugEnabled())
					logDebug("sq: entries=" + sq.size() + ", peeked " + MessageUtil.qeStr(qe));
			}
			return qe;
		}
		return null;
	}

	public final int sizeOfSQ() {
		return sq != null ? sq.size() : 0;
	}

	/**
	 * QueueTimeoutException이 발생했고 엔트리 최대 갯수에 도달한 경우 busy 상태이다. poll()과 clear()가
	 * 호출되면 busy 상태가 해제된다.
	 * 
	 * @return
	 */
	public boolean isRecvQueueBusy() {
		return rq != null ? rq.isBusy() : false;
	}

	private Queue<QueueEntry> getRecvQueue() {
		if (rq == null) {
			synchronized (this) {
				if (rq == null) {
					QueueParams qParams = context.getService().getServiceConfig().getQueueParams();

					if ((getLoginer() != null && getLoginId() == null)
							|| qParams.getQueueType() == Queue.QTYPE_MEMORY) {
						this.rq = new SynchronizedQueue<QueueEntry>(qParams.getMemoryLoadEntries(),
								qParams.getTimeoutMillisecond());

					} else if (qParams.getQueueType() == Queue.QTYPE_FILE) {
						String rqKey = Naming.recvQueueKey(context.getService(), this);
						this.rq = new PersistentQueue<QueueEntry>(rqKey, qParams);
						new LifeCycle((LifeCycleObject) rq, "RQ", this).start();
					}
				}
			}
		}
		return rq;
	}

	private Queue<QueueEntry> getSendQueue() {
		if (sq == null) {
			synchronized (this) {
				if (sq == null) {
					QueueParams qParams = context.getService().getServiceConfig().getQueueParams();

					if ((getLoginer() != null && getLoginId() == null)
							|| qParams.getQueueType() == Queue.QTYPE_MEMORY) {
						this.sq = new SynchronizedQueue<QueueEntry>(qParams.getMemoryLoadEntries() + 100,
								qParams.getTimeoutMillisecond());

					} else if (qParams.getQueueType() == Queue.QTYPE_FILE) {
						String sqKey = Naming.sendQueueKey(context.getService(), this);
						this.sq = new PersistentQueue<QueueEntry>(sqKey, qParams);
						new LifeCycle((LifeCycleObject) sq, "SQ", this).start();
					}
				}
			}
		}
		return sq;
	}

	public final long getLastIOTime() {
		return lastIO;
	}

	public final Service getService() {
		return context.getService();
	}

	public final String getLoginId() {
		return context.getLoginId();
	}

	public final String getLocalIp() {
		return context.getLocalIp();
	}

	public final int getLocalPort() {
		return context.getLocalPort();
	}

	public final String getRemoteIp() {
		return context.getRemoteIp();
	}

	public final int getRemotePort() {
		return context.getRemotePort();
	}

	public final LoginIdGroup getIdGroup() {
		return getService().getLoginIdGroup(getLoginId(), false);
	}

	public final IpGroup getIpGroup() {
		return getService().getIpGroup(getRemoteIp(), false);
	}

	public boolean isBusy() {
		return false;
	}

	public final boolean isClosed() {
		return context.isClosed();
	}

	public void killEventHandler() {
		try {
			stopCronjob();
		} catch (Throwable t) {
		}

		try {
			MsgRouter.getInstance().getBeanCache().deleteBeans(getService(), this);
		} catch (Throwable t) {
		}

		try {
			getService().deregisterSession(this);
		} catch (Throwable t) {
		}

		try {
			context.close();
		} catch (Throwable t) {
		}

		if (!getService().getServiceConfig().getQueueParams().isPersistent()) {
			if (rq != null) {
				try {
					rq.clear();
				} catch (Throwable e) {
					logError(e);
				}
			}
			if (sq != null) {
				try {
					sq.clear();
				} catch (Throwable e) {
					logError(e);
				}
			}
		}

		if (context.getService().getServiceConfig().getSessionCloserClass() != null) {
			try {
				SessionCloser sessionCloser = (SessionCloser) context.getService().getServiceConfig()
						.getSessionCloserClass().newInstance();
				sessionCloser.setService(context.getService());
				sessionCloser.setSession(this);
				sessionCloser.execute();
			} catch (Throwable e) {
				logError(e);
			}
		}
	}
}
