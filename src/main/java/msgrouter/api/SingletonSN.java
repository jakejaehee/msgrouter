package msgrouter.api;

public class SingletonSN {
	private static volatile SingletonSN instance = null;

	private int sn = 0;
	private final int MIN;
	private final int MAX;

	private SingletonSN() {
		this.MIN = 0;
		this.MAX = Integer.MAX_VALUE;
		this.sn = MIN;
	}

	public static SingletonSN getInstance() {
		if (instance == null) {
			synchronized (Object.class) {
				if (instance == null) {
					instance = new SingletonSN();
				}
			}
		}
		return instance;
	}

	public void init() {
		sn = MIN;
	}

	public synchronized int next() {
		if (sn < MAX) {
			return sn++;
		} else {
			sn = MIN;
			return MAX;
		}
	}

	public int current() {
		return sn;
	}
}
