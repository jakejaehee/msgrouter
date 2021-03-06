package msgrouter.engine;

import msgrouter.api.QueueEntry;
import msgrouter.engine.queue.QueueTimeoutException;
import elastic.util.util.TechException;

public class LoginIdGroup extends KeyedList<String, Session> {
	private String loginId;

	public LoginIdGroup(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginId() {
		return loginId;
	}

	public void putAllSQ(QueueEntry qe) throws TechException,
			QueueTimeoutException {
		for (int l = 0; l < size(); l++) {
			Session ss = get(l);
			if (ss != null) {
				ss.putSQ(qe);
			}
		}
	}

	public String toString() {
		return "hash=" + hashCode() + ", loginId=" + loginId + ": "
				+ super.toString();
	}
}