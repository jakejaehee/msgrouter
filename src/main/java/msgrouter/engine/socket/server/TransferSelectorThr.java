package msgrouter.engine.socket.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.util.TechException;
import msgrouter.constant.Const;
import msgrouter.engine.Service;
import msgrouter.engine.queue.NotSynchronizedQueue;
import msgrouter.engine.queue.QueueTimeoutException;
import msgrouter.engine.socket.NioUtil;

public final class TransferSelectorThr extends LifeCycleObject implements Runnable {
	private final Server svr;
	private final Selector sel;
	private final TransferSelectorEventHandler eventHandler;

	private final NotSynchronizedQueue<Registration> pendingRegistrations = new NotSynchronizedQueue<Registration>();

	TransferSelectorThr(Service svc, TransferSelectorEventHandler eventHandler) throws IOException {
		super(TransferSelectorThr.class);

		this.svr = (Server) svc;
		this.sel = Selector.open();
		this.eventHandler = eventHandler;

		setLogger(svc.getLogger());
	}

	public void run() {
		/**
		 * pendingSelKeys: Transfer용 SelectorPool 전용
		 */
		List<SelectionKey> pendingSelKeys = null;

		while (!currentLifeCycle().isShutdownMode() || !currentLifeCycle().readyToShutdown()) {
			try {
				if (currentLifeCycle().sleep(Const.VAL_LOOP_SLEEP_MILLIS, Const.VAL_LOOP_SLEEP_NANOS))
					continue;

				interestWriteOpForPendingWriteEvents();

				sel.select();
				Set<SelectionKey> selectedSelKeys = sel.selectedKeys();

				// selected된 SelectionKey가 있거나 이전에 송수신처리가 완료되지 않은 pending 건이
				// 있을 경우 해당 SelectionKey들을 처리한다.
				if (selectedSelKeys.size() > 0 || pendingSelKeys != null) {
					pendingSelKeys = handleSelectorEvent(selectedSelKeys, pendingSelKeys);
					if (pendingSelKeys != null)
						sel.wakeup();
				}

				registerPendingChannelsToSelector();
			} catch (Throwable e) {
				logError(e);
			}
		}
	}

	private void interestWriteOpForPendingWriteEvents() {
		Iterator<SelectionKey> it = sel.keys().iterator();
		while (it.hasNext()) {
			SelectionKey selKey = (SelectionKey) it.next();
			ServerSession session = (ServerSession) selKey.attachment();
			if (session != null) {
				if (session.sizeOfSQ() > 0) {
					if (session.getSelectionKey().isValid()) {
						session.getSelectionKey().interestOps(SelectionKey.OP_WRITE);
					}
				}
			}
		}
	}

	/**
	 * selected된 SelectionKey들과 이전에 송수신처리가 완료되지 않고 pending되었던 SelectionKey들에 대한
	 * 송수신처리를 한다. 만일 하나의 SelectionKey에 대한 송수신처리에서 정해진 건수를 초과할 경우에는 다시 pending 큐에
	 * 해당 SelectionKey를 넣어후 리턴하여 다음 번에 처리할 수 있도록 한다.
	 * 
	 * @param selectedSelKeys
	 * @param pendingSelKeys
	 * @return 송수신처리건수가 일정한 건수를 초과할 경우 해당 SelectionKey를 pending 큐에 넣어서 리턴한다.
	 */
	private List<SelectionKey> handleSelectorEvent(final Set<SelectionKey> selectedSelKeys,
			final List<SelectionKey> pendingSelKeys) {

		List<SelectionKey> newPendingSK = handleSelectorEvent(selectedSelKeys.iterator());

		/*
		 * 이전에 송수신처리를 완료하지 않고 pending하였던 SelectionKey들을 추가적으로 처리하도록 한다.
		 * selected된 SelectionKey들을 먼저 처리한 후 pending 건들을 처리해야 한다. 그래야 특정한
		 * SelectionKey만 처리하지 않고 다른 SelectionKey들도 처리할 수 있다. 즉 특정 세션에 의한 독점을 막을
		 * 수 있다.
		 */
		if (pendingSelKeys != null) {
			final List<SelectionKey> tmp = handleSelectorEvent(pendingSelKeys.iterator());
			if (tmp != null && tmp.size() > 0) {
				if (newPendingSK != null)
					newPendingSK.addAll(tmp);
				else
					newPendingSK = tmp;
			}
		}

		return newPendingSK;
	}

	private List<SelectionKey> handleSelectorEvent(final Iterator<SelectionKey> selKeyIt) {

		List<SelectionKey> newPendingSQ = null;

		while (selKeyIt.hasNext()) {
			SelectionKey selKey = selKeyIt.next();
			// it.remove();
			if (!selKey.isValid())
				continue;

			boolean completed = false;
			try {
				completed = eventHandler.handleSelectorEvent(selKey);
			} catch (QueueTimeoutException e) {
				logWarn(e.getMessage());
			} catch (Throwable e) {
				if (e instanceof TechException || e instanceof IOException) {
					String ip = ((SocketChannel) selKey.channel()).socket().getInetAddress().getHostAddress();
					BlackList bl = ((Server) svr).getBlackList();
					Score score = bl.getScore(ip);
					if (score.white) {
						if (System.currentTimeMillis() - score.lastTime < bl.getWhiteInterval()) {
							score.value--;
						}
						if (score.value <= bl.getBlackScore()) {
							score.white = false;
						}
					}
					score.lastTime = System.currentTimeMillis();

					svr.cancelSelectionKey(selKey, score.white);

					if (score.white) {
						if (logTraceEnabled()) {
							logError(e);
						} else {
							logError(e.getMessage());
						}
					}
				} else {
					logError(e);
				}
			} finally {
				/*
				 * 송수신처리 건이 남은 경우 pending queue에 SelectionKey를 넣어서 다음 select()
				 * 호출 시 blocking되지 않고 다시 serviceSelectedKeys()가 호출되어 처리할 수 있도록
				 * 한다.
				 */
				if (!completed) {
					if (newPendingSQ == null)
						newPendingSQ = new ArrayList<SelectionKey>();
					newPendingSQ.add(selKey);
				}

				if (selKey.isValid())
					selKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			}
		}

		return newPendingSQ;
	}

	/**
	 * Called by other thread such as AcceptSelector
	 * 
	 * @param sc
	 * @param ss
	 * @return
	 * @throws ClosedChannelException
	 * @throws TechException
	 * @throws QueueTimeoutException
	 */
	boolean register(SelectableChannel sc, ServerSession ss)
			throws ClosedChannelException, TechException, QueueTimeoutException {
		synchronized (pendingRegistrations) {
			if (currentLifeCycle().isShutdownMode())
				return false;

			if (sel.keys().size() + pendingRegistrations.size() == svr.getServiceConfig()
					.getMaxSessionsPerTransferThr())
				return false;

			Registration reg = new Registration(svr, this, sc, ss);
			pendingRegistrations.put(reg);
			sel.wakeup();
			return true;
		}
	}

	private void registerPendingChannelsToSelector() {
		synchronized (pendingRegistrations) {
			int maxRegistrable = svr.getServiceConfig().getMaxSessionsPerTransferThr() - sel.keys().size();
			while (maxRegistrable > 0 && !pendingRegistrations.isEmpty()) {
				Registration reg = (Registration) pendingRegistrations.poll();
				boolean success = false;
				try {
					success = reg.register(sel);
				} catch (Throwable t) {
					logError(t);
				}
				if (!success) {
					--maxRegistrable;
				}
			}
			pendingRegistrations.clear();
		}
	}

	public boolean isBusy() {
		return false;
	}

	public void killEventHandler() {
		sel.wakeup();
		NioUtil.closeAllChannels(sel);
	}

	int getThreadLoad() {
		return sel.keys().size() + pendingRegistrations.size();
	}

	private static final class Registration {
		private Server svr;
		private TransferSelectorThr st;
		private SelectableChannel channel;
		private ServerSession ss;

		Registration(Server svr, TransferSelectorThr st, SelectableChannel channel, ServerSession ss) {
			this.svr = svr;
			this.st = st;
			this.channel = channel;
			this.ss = ss;
		}

		boolean register(Selector sel) throws IOException {
			try {
				SelectionKey selKey = channel.register(sel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, ss);
				ss.setSelectorInfo(st, selKey);
				if (st.logTraceEnabled())
					st.logTrace("registered a session into Selector. selKey=" + NioUtil.stateOf(selKey));
				svr.increaseNrOfSessions();
				return true;
			} catch (IOException e) {
				throw e;
			} catch (Throwable e) {
				return false;
			}
		}
	}
}
