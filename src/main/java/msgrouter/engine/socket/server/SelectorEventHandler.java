package msgrouter.engine.socket.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import elastic.util.util.TechException;
import msgrouter.engine.queue.QueueTimeoutException;

public interface SelectorEventHandler {

	/**
	 * 
	 * @param selKey
	 * @return 주어진 SelectionKey에 대한 송수신처리 완료여부. 모두 완료했을 경우 true, 미처리 송수신건이 있을 경우
	 *         false를 리턴한다.
	 * @throws IOException
	 * @throws TechException
	 * @throws QueueTimeoutException
	 */
	public boolean handleSelectorEvent(final SelectionKey selKey)
			throws IOException, TechException, QueueTimeoutException;
}
