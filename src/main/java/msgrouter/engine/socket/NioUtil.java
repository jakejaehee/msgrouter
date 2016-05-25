package msgrouter.engine.socket;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class NioUtil {
	public static String stateOf(SelectionKey selKey) {
		if (selKey.isValid()) {
			return "SelectionKey=" + selKey.hashCode() + ": "
					+ (selKey.isAcceptable() ? "OP_ACCEPT " : "")
					+ (selKey.isReadable() ? "OP_READ " : "")
					+ (selKey.isWritable() ? "OP_WRITE" : "");
		} else {
			return "SelectionKey=" + selKey.hashCode() + ": canceled";
		}
	}

	public static void closeAllChannels(Selector selector) {
		try {
			Iterator it = selector.keys().iterator();
			while (it.hasNext()) {
				SelectableChannel channel = ((SelectionKey) it.next())
						.channel();
				if (channel != null) {
					try {
						synchronized (channel) {
							if (channel.isOpen())
								channel.close();
						}
					} catch (IOException e) {
					}
				}
			}
		} finally {
			try {
				selector.close();
			} catch (Exception e) {
			}
		}
	}
}
