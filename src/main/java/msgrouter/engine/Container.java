package msgrouter.engine;

import java.util.List;

import msgrouter.engine.config.ServiceConfig;
import elastic.util.lifecycle.LifeCycleObject;

public abstract class Container extends LifeCycleObject {

	public Container(Class subClass) {
		super(subClass);
	}

	public final void stopService(ServiceConfig svcConf) {
		LifeCycleObject child = getChild(svcConf.getServiceId());
		if (child != null) {
			child.currentLifeCycle().kill();
		}
		// svcConf.setService(null);
	}

	public final Service getService(String svcId) {
		return (Service) getChild(svcId);
	}

	public final List<LifeCycleObject> getServiceList() {
		return childList();
	}
}
