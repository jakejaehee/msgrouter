package msgrouter.engine;

import org.apache.log4j.Logger;

public class MiscUtil {
	public static final Logger LOG = Logger.getLogger(MiscUtil.class);

	private static volatile Object SNLock = new Object();
	private static volatile short sn = 0;

	public static String nextSN() {
		synchronized (SNLock) {
			String s = System.currentTimeMillis() + String.valueOf(sn);
			if (sn < Short.MAX_VALUE)
				sn++;
			else
				sn = 0;
			return s;
		}
	}
}
