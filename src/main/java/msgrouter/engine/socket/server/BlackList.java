package msgrouter.engine.socket.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class BlackList {
	private static final Logger LOG = Logger.getLogger(BlackList.class);

	private int whiteInterval = 10000;
	private int blackScore = -10;

	private Map<String, Score> map = new HashMap<String, Score>();

	public BlackList(final long checkerSleep, final int whiteInterval,
			final int blackScore) {
		this.whiteInterval = whiteInterval;
		this.blackScore = blackScore;

		Thread thr = new Thread(new Runnable() {
			public void run() {
				while (true) {
					synchronized (map) {
						Iterator it = map.entrySet().iterator();
						while (it.hasNext()) {
							Entry e = (Entry) it.next();
							Score score = (Score) e.getValue();
							if (LOG.isTraceEnabled()) {
								LOG.trace("checks " + score);
							}
							long time = System.currentTimeMillis()
									- score.lastTime;
							if (time > getWhiteInterval()) {
								if (score.white) {
									it.remove();
									if (LOG.isTraceEnabled()) {
										LOG.trace("removed " + score);
									}
								} else {
									score.value += time / getWhiteInterval();
									if (score.value > getBlackScore()) {
										score.white = true;
									}
								}
							}
						}
					}
					try {
						Thread.sleep(checkerSleep);
					} catch (Throwable t) {
					}
				}
			}
		});
		thr.setDaemon(true);
		thr.start();
	}

	public int getWhiteInterval() {
		return whiteInterval;
	}

	public int getBlackScore() {
		return blackScore;
	}

	public Score getScore(String addr) {
		Score c = map.get(addr);
		if (c == null) {
			synchronized (map) {
				c = map.get(addr);
				if (c == null) {
					c = new Score(addr);
					map.put(addr, c);
				}
			}
		}
		return c;
	}
}
