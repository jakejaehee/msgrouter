package msgrouter.engine.event;


public class EventRestartSessionRequired implements Event {
	private final String loginId;

	public EventRestartSessionRequired(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginId() {
		return loginId;
	}
}
