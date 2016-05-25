package msgrouter.engine.config.rule;

import java.util.ArrayList;
import java.util.List;

public class MessageRule {
	private String msgType = null;
	private String svcId = null;

	private List<InvokeRule> invokeRuleList = null;
	private RoutingRules routingRules = null;

	public MessageRule(String msgType) {
		this.msgType = msgType;
		this.invokeRuleList = new ArrayList<InvokeRule>();
	}

	public void setServiceId(String svcId) {
		this.svcId = svcId;
	}

	public String getServiceId() {
		return svcId;
	}

	public void addInvokeRule(InvokeRule invokeRule) {
		invokeRuleList.add(invokeRule);
	}

	public List<InvokeRule> getInvokeRuleList() {
		return invokeRuleList;
	}

	public String getMessageType() {
		return msgType;
	}

	public void setRoutingRules(RoutingRules routingRules) {
		this.routingRules = routingRules;
	}

	public RoutingRules getRoutingRules() {
		return routingRules;
	}

	public String toString() {
		return "service=" + svcId + ", routingRules=" + routingRules
				+ ", invokeRuleList=" + invokeRuleList;
	}

	public void clear() {
		msgType = null;
		svcId = null;

		if (invokeRuleList != null) {
			int size = invokeRuleList.size();
			for (int i = 0; i < size; i++) {
				InvokeRule invokeRule = invokeRuleList.get(i);
				invokeRule.clear();
			}
			invokeRuleList.clear();
		}

		if (routingRules != null) {
			routingRules.clear();
			routingRules = null;
		}
	}
}
