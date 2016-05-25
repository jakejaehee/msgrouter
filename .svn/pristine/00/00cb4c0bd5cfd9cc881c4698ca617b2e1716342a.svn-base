package msgrouter.engine.config.rule;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CronjobRulesTable {
	private Hashtable<String, List<CronjobRules>> cronjobRulesTab = new Hashtable<String, List<CronjobRules>>();

	public void addCronjobRules(String svcId, CronjobRules cronjobRules) {
		List<CronjobRules> list = (List<CronjobRules>) cronjobRulesTab
				.get(svcId);
		if (list == null) {
			list = new ArrayList<CronjobRules>();
			cronjobRulesTab.put(svcId, list);
		}
		list.add(cronjobRules);
	}

	public List<CronjobRules> getCronjobRulesList(String svcId) {
		return cronjobRulesTab.get(svcId);
	}

	public String toString() {
		return cronjobRulesTab.toString();
	}

	public void clear() {
		cronjobRulesTab.clear();
	}
}
