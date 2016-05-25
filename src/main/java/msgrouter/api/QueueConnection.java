package msgrouter.api;

import java.io.IOException;

import msgrouter.engine.Session;
import msgrouter.engine.queue.QueueTimeoutException;

import org.apache.log4j.Logger;

import elastic.util.util.TechException;

public class QueueConnection {
	private static final Logger LOG = Logger.getLogger(QueueConnection.class);

	private Session ss = null;

	/**
	 * This QueueConnection connects to a service in the same msgrouter
	 * instance. It is constructed in instance of msgrouter and it is looked up
	 * by calling MsgClient's instance method lookupQueueConnection(svcId).
	 * Therefore in order to use it please starts up MsgClient in advance.
	 * 
	 * @param ss
	 */
	public QueueConnection(Session ss) {
		this.ss = ss;
	}

	/**
	 * If connecting to a service in the same msgrouter instance then tring to
	 * put a data into send-queue otherwise sending through OutputStream.<BR>
	 * 
	 * @param qe
	 * @throws IOException
	 * @throws QueueTimeoutException
	 */
	public final void send(QueueEntry qe) throws IOException,
			QueueTimeoutException {
		try {
			if (ss != null) {
				ss.putSQ(qe);
			}
		} catch (TechException e) {
			throw new IOException(e.getMessage());
		}
	}

	public final QueueEntry recv() throws IOException {
		try {
			if (ss != null) {
				return ss.pollRQ();
			}
		} catch (TechException e) {
			throw new IOException(e.getMessage());
		}
		return null;
	}
}
