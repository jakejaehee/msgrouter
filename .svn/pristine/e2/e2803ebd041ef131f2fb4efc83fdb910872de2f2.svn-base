package msgrouter.engine.socket.client;

/**
 * Formula: interval += (1 + (interval * 2) / 5)
 * 
 * @author jakelee70
 */
public class IncrementalInterval {
	private long interval = 1L;
	private long initInterval = 1L;
	private long maxInterval = 1L;

	public IncrementalInterval() {
		this(1L, 1L);
	}

	public IncrementalInterval(long initInterval, long maxInterval) {
		this.interval = initInterval > 0 ? initInterval : 1L;
		this.initInterval = initInterval;
		this.maxInterval = maxInterval > initInterval ? maxInterval
				: initInterval;
	}

	public long nextValue() {
		if (interval < maxInterval) {
			interval += (1 + (interval * 2) / 5);
		}
		return interval;
	}

	public void clear() {
		interval = initInterval;
	}

	public static void main(String[] args) {
		int cnt = 0;
		IncrementalInterval incInt = new IncrementalInterval(10, 3000);
		while (true) {
			long time = incInt.nextValue();
			System.out.println(time);
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
			}
			cnt++;
			if (cnt > 17) {
				cnt = 0;
				incInt.clear();
			}
		}
	}
}
