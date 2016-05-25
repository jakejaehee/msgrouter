package msgrouter.api.interfaces.bean;

import msgrouter.api.QueueEntry;
import msgrouter.api.interfaces.Message;
import msgrouter.engine.Router;
import msgrouter.engine.queue.QueueTimeoutException;
import elastic.util.util.TechException;

public abstract class SentTrigger extends Bean {

	private static final long serialVersionUID = -70507048424981569L;

	public abstract void execute(Message sentMsg) throws TechException;

	protected final void swtichTo(String dstSvcId, QueueEntry qe)
			throws TechException, QueueTimeoutException {
		Router.switchTo(qe, dstSvcId);
	}
}
