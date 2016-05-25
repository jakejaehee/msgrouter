package msgrouter.engine.socket.server;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import elastic.util.lifecycle.LifeCycle;
import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.util.TechException;
import msgrouter.engine.queue.QueueTimeoutException;

final class TransferSelectorPool extends LifeCycleObject {
	private final Server svr;
	private final TransferSelectorEventHandler eventHandler;
	private final int minThrs;
	private final List<TransferSelectorThr> selThrsList = new ArrayList<TransferSelectorThr>();

	private volatile int nextIdx;

	TransferSelectorPool(Server svr, TransferSelectorEventHandler eventHandler, int minThrs) throws IOException {
		super(TransferSelectorPool.class);

		this.svr = svr;
		this.eventHandler = eventHandler;
		this.minThrs = minThrs;
		this.nextIdx = 0;

		setLogger(svr.getLogger());
	}

	public void run() {
		for (int i = 0; i < minThrs; ++i) {
			try {
				createSelectorThr();
			} catch (Exception e) {
				logError(e);
			}
		}
		this.nextIdx = 0;
	}

	private TransferSelectorThr createSelectorThr() throws IOException, TechException {
		TransferSelectorThr st = new TransferSelectorThr(svr, eventHandler);
		selThrsList.add(st);
		new LifeCycle(st, "transferSelThr", this).start();
		logInfo("created a TransferSelectorThr.");
		return st;
	}

	/**
	 * Called by other thread such as AcceptSelector
	 * 
	 * @param sc
	 * @param ss
	 * @return
	 * @throws IOException
	 * @throws TechException
	 * @throws QueueTimeoutException
	 */
	boolean register(SelectableChannel sc, ServerSession ss)
			throws IOException, TechException, QueueTimeoutException {
		synchronized (selThrsList) {
			int lastIdx = (nextIdx == 0 ? selThrsList.size() : nextIdx) - 1;
			for (;;) {
				TransferSelectorThr st = selThrsList.get(nextIdx);
				if (st.register(sc, ss)) {
					if (++nextIdx == selThrsList.size())
						nextIdx = 0;
					return true;
				} else {
					// key == null designates that the thread was unable to
					// register the channel. It is either dead, or just its
					// selector is saturated.
					if (st.currentLifeCycle().isShutdownMode()) {
						selThrsList.remove(nextIdx);
						--nextIdx;
						if (nextIdx == -1) {
							nextIdx = selThrsList.size() - 1;
							if (nextIdx == -1) {
								// last alive thread has died, which should
								// not happen normally - create new thread.
								nextIdx = 0;
								return createSelectorThr().register(sc, ss);
							}
						}
					}
					if (nextIdx == lastIdx) {
						// This is a small optimization - if all threads are
						// currently saturated, assume the newly created thread
						// is
						// optimal selection for next registration.
						nextIdx = selThrsList.size();
						return createSelectorThr().register(sc, ss);
					} else {
						if (++nextIdx == selThrsList.size())
							nextIdx = 0;
					}
				}
			}
		}
	}

	int[] getThreadLoad() {
		int[] retval = new int[selThrsList.size()];
		for (int i = 0; i < retval.length; ++i) {
			retval[i] = ((TransferSelectorThr) selThrsList.get(i)).getThreadLoad();
		}
		return retval;
	}

	public boolean isBusy() {
		return false;
	}

	public void killEventHandler() {
		synchronized (selThrsList) {
			Iterator it = selThrsList.iterator();
			while (it.hasNext()) {
				((TransferSelectorThr) it.next()).currentLifeCycle().kill();
			}
		}
	}
}
