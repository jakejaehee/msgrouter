package msgrouter.engine;

import msgrouter.api.interfaces.bean.Bean;
import elastic.util.java.ReflectionUtil;

public class BeanFactory {
	public static Bean getInstance(Class beanClass) {
		Bean bean = null;
		try {
			bean = (Bean) ReflectionUtil.newInstance(beanClass);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return bean;
	}
}
