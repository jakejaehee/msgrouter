package msgrouter.engine.config.rule;

import java.util.ArrayList;
import java.util.List;

public class CronjobRules {
	private final Class cronjobClass;
	private String svcId = null;
	private final List<InvokeRule> invokeRuleList;

	public CronjobRules(Class cronjobClass) {
		this.cronjobClass = cronjobClass;
		this.invokeRuleList = new ArrayList<InvokeRule>();
	}

	public Class getCronjobClass() {
		return cronjobClass;
	}

	public void setServiceId(String daemonId) {
		this.svcId = daemonId;
	}

	public String getDaemonId() {
		return svcId;
	}

	public void addInvokeRules(InvokeRule invokeRule) {
		invokeRuleList.add(invokeRule);
	}

	public int countInvokeRules() {
		return invokeRuleList.size();
	}

	public List<InvokeRule> getInvokeRuleList() {
		return invokeRuleList;
	}

	public String toString() {
		return "service=" + svcId + ", invokeRule=" + invokeRuleList.toString();
	}

	public void clear() {
		svcId = null;
		for (int i = 0; i < invokeRuleList.size(); i++) {
			InvokeRule invokeRule = invokeRuleList.get(i);
			if (invokeRule != null) {
				invokeRule.clear();
			}
		}
		invokeRuleList.clear();
	}
}
