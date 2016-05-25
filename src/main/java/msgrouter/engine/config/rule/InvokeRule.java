package msgrouter.engine.config.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import msgrouter.api.interfaces.bean.Bean;
import msgrouter.constant.Const;
import elastic.util.java.ReflectionUtil;
import elastic.util.scheduler.Schedule;
import elastic.util.util.TechException;

public class InvokeRule {
	private Class beanClass = null;
	private boolean isSynchronized = false;

	private RoutingRules wildcardRoutingRules = null;
	private Map<String, RoutingRules> routingRulesMap = new HashMap<String, RoutingRules>(
			10);
	private List<Schedule> schList = null;

	public InvokeRule(Class beanClass, boolean isSynchronized)
			throws TechException {
		if (!ReflectionUtil.isCastable(beanClass, Bean.class)) {
			throw new TechException("Class " + beanClass.getName()
					+ " is not Bean class.");
		}
		this.beanClass = beanClass;
		this.isSynchronized = isSynchronized;
	}

	public Class getBeanClass() {
		return beanClass;
	}

	public boolean isSynchronized() {
		return isSynchronized;
	}

	public void setRoutingRules(String msgType, RoutingRules routingRules) {
		if (Const.VAL_MSG_TYPE_WILDCARD.equals(msgType)) {
			wildcardRoutingRules = routingRules;
		} else {
			routingRulesMap.put(msgType, routingRules);
		}
	}

	public RoutingRules getRoutingRules(String msgType) {
		if (Const.VAL_MSG_TYPE_WILDCARD.equals(msgType)) {
			return wildcardRoutingRules;
		} else {
			RoutingRules rule = routingRulesMap.get(msgType);
			if (rule != null) {
				return rule;
			} else {
				return wildcardRoutingRules;
			}
		}
	}

	public void setScheduleList(List<Schedule> schList) {
		this.schList = schList;
	}

	public List<Schedule> getScheduleList() {
		return schList;
	}

	public String toString() {
		return "Bean=" + beanClass.getName() + ", routingRulesMap="
				+ routingRulesMap;
	}

	public void clear() {
		beanClass = null;
		routingRulesMap.clear();
	}
}
