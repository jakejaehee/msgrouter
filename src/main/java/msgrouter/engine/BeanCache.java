package msgrouter.engine;

import elastic.stateful.Bucket.Entry;
import elastic.stateful.Node;
import elastic.stateful.Stateful;
import elastic.util.util.TechException;
import msgrouter.api.interfaces.bean.Bean;

public class BeanCache {
	private final Stateful stateful;

	public BeanCache() {
		this.stateful = new Stateful();
	}

	public synchronized Bean getBean(Service svc, Session ss, Class<Bean> beanClass) throws TechException {
		String cachePath = path(svc, ss, beanClass);
		Bean bean = getBean(cachePath, beanClass);
		if (bean == null) {
			ss.logError("Bean '" + beanClass + "' is not found.");
		} else {
			bean.setService(svc);
			bean.setSessionContext(ss.getSessionContext());
		}
		return bean;
	}

	public synchronized Bean getBean(Service svc, NoSession nosession, Class<Bean> beanClass) throws TechException {
		String cachePath = path(svc, nosession, beanClass);
		Bean bean = getBean(cachePath, beanClass);
		if (bean == null) {
			svc.logError("Bean '" + beanClass + "' is not found.");
		} else {
			bean.setService(svc);
		}
		return bean;
	}

	private Bean getBean(String path, Class<Bean> beanClass) {
		Bean bean = null;
		Node node = stateful.get(path);
		if (node instanceof Entry) {
			bean = (Bean) ((Entry) node).getValue();
		}
		if (bean == null) {
			bean = BeanFactory.getInstance(beanClass);
			if (bean != null) {
				try {
					stateful.insert(path, bean);
				} catch (Exception e) {
					node = stateful.get(path);
					return (Bean) ((Entry) node).getValue();
				}
			}
		}
		return bean;
	}

	public synchronized boolean deleteBeans(Service svc, Session ss) {
		String path = path(svc, ss, null);
		return stateful.delete(path);
	}

	public synchronized boolean deleteBean(Service svc, Session ss, Class<Bean> beanClass) {
		String path = path(svc, ss, beanClass);
		return stateful.delete(path);
	}

	public synchronized boolean deleteBeans(Service svc, NoSession nosession) {
		String path = path(svc, nosession, null);
		return stateful.delete(path);
	}

	public synchronized boolean deleteBean(Service svc, NoSession nosession, Class<Bean> beanClass) {
		String path = path(svc, nosession, beanClass);
		return stateful.delete(path);
	}

	private String path(Service svc, Session ss, Class<Bean> beanClass) {
		// if (ss.getLoginId() != null) {
		// return _path(svc, ss.getLoginId(), beanClass);
		// }
		return _path(svc, ss.getAlias(), beanClass);
	}

	private String path(Service svc, NoSession nosession, Class<Bean> beanClass) {
		return _path(svc, null, beanClass);
	}

	private String _path(Service svc, String ssName, Class<Bean> beanClass) {
		StringBuilder sb = new StringBuilder();
		sb.append('/').append(svc.getServiceId());
		if (ssName != null) {
			sb.append('/').append(ssName);
		} else {
			sb.append('/').append("_noSs_");
		}
		if (beanClass != null) {
			sb.append('/').append(beanClass.getName());
		}
		return sb.toString();
	}
}
