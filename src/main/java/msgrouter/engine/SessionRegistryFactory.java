package msgrouter.engine;

import elastic.util.pool.loadbalancing.PoolEntry;
import elastic.util.pool.loadbalancing.PoolEntryFactory;

public class SessionRegistryFactory implements PoolEntryFactory {
	private Service svc = null;

	public SessionRegistryFactory(Service svc) {
		this.svc = svc;
	}

	public PoolEntry newInstance() {
		return new SessionRegistry(svc);
	}
}
