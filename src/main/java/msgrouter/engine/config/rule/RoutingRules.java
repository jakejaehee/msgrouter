package msgrouter.engine.config.rule;

import java.util.ArrayList;
import java.util.List;

public class RoutingRules {
	private String msgType = null;
	private Class sentTriggerClass = null;
	private List<SwitchTo> switchToList = new ArrayList<SwitchTo>();

	public RoutingRules(String msgType) {
		this.msgType = msgType;
	}

	public String getMessageType() {
		return msgType;
	}

	public Class getSentTriggerClass() {
		return sentTriggerClass;
	}

	public void setSentTriggerClass(Class sentTriggerClass) {
		this.sentTriggerClass = sentTriggerClass;
	}

	public void addSwitchTo(SwitchTo switchTo) {
		switchToList.add(switchTo);
	}

	/**
	 * 
	 * @return never return null
	 */
	public List<SwitchTo> getSwitchToList() {
		return switchToList;
	}

	public int getSwitchToCount() {
		return switchToList.size();
	}

	public String toString() {
		return "switchTo:" + switchToList;
	}

	public void clear() {
		switchToList.clear();
	}
}
