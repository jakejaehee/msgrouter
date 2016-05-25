package msgrouter.engine.queue;

import elastic.util.util.TechException;

public class QueueTimeoutException extends TechException {

	private static final long serialVersionUID = -1939141499976483502L;

	public QueueTimeoutException(String msg) {
		super(msg);
	}
}
