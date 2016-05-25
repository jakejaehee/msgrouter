package msgrouter.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import elastic.web.dataset.WebRow;
import msgrouter.engine.config.ServiceConfig;

public class ServiceContext {

	private final Service svc;

	private Map<String, Object> map = null;

	public ServiceContext(Service svc) {
		this.svc = svc;
		this.map = new WebRow<String, Object>();
	}

	public Service getService() {
		return svc;
	}

	public int getNrOfSessions() {
		return svc.getNrOfSessions();
	}

	public int getNrOfConnections() {
		return svc.getNrOfConnections();
	}

	public ServiceConfig getServiceConfig() {
		return svc.getServiceConfig();
	}

	public Object put(String key, Object value) {
		return map.put(key, value);
	}

	public void putAll(Map<String, Object> m) {
		map.putAll(m);
	}

	public Object get(String key) {
		return map.get(key);
	}

	public Object remove(String key) {
		return map.remove(key);
	}

	public void clear() {
		map.clear();
	}

	public final Map<String, Object> toMap() {
		Map<String, Object> m = new HashMap<String, Object>();
		Iterator<Entry<String, Object>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> e = (Entry<String, Object>) it.next();
			m.put(e.getKey(), e.getValue());
		}
		return m;
	}
}
