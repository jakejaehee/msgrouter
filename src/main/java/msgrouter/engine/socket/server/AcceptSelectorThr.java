package msgrouter.engine.socket.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

import elastic.util.lifecycle.LifeCycleObject;
import msgrouter.constant.Const;
import msgrouter.engine.socket.NioUtil;

public final class AcceptSelectorThr extends LifeCycleObject implements Runnable {
	private final Server svr;
	private final Selector sel;
	private final SelectorEventHandler eventHandler;

	AcceptSelectorThr(ServerSocketChannel ssc, Server svr, SelectorEventHandler eventHandler) throws IOException {
		super(AcceptSelectorThr.class);

		this.svr = svr;
		this.eventHandler = eventHandler;

		this.sel = Selector.open();
		ssc.register(sel, SelectionKey.OP_ACCEPT);

		setLogger(svr.getLogger());
	}

	public void run() {
		while (!currentLifeCycle().isShutdownMode() || !currentLifeCycle().readyToShutdown()) {
			try {
				if (currentLifeCycle().sleep(Const.VAL_LOOP_SLEEP_MILLIS, Const.VAL_LOOP_SLEEP_NANOS))
					continue;

				sel.select();
				Set<SelectionKey> selectedSelKeys = sel.selectedKeys();

				if (selectedSelKeys.size() > 0)
					handleSelectorEvent(selectedSelKeys.iterator());
			} catch (Throwable e) {
				logError(e);
			}
		}
	}

	private void handleSelectorEvent(final Iterator<SelectionKey> selKeyIt) {
		while (selKeyIt.hasNext()) {
			SelectionKey selKey = selKeyIt.next();
			if (!selKey.isValid())
				continue;

			try {
				eventHandler.handleSelectorEvent(selKey);
			} catch (Throwable e) {
				if (logTraceEnabled()) {
					logError(e);
				} else {
					logError(e.getMessage());
				}
			}
		}
	}

	public boolean isBusy() {
		return false;
	}

	public void killEventHandler() {
		NioUtil.closeAllChannels(sel);
	}

	int getThreadLoad() {
		return sel.keys().size();
	}
}
