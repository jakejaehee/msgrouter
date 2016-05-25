package msgrouter.engine.queue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;

import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.util.FileUtil;
import elastic.util.util.TechException;

public abstract class AbstractFileQueue<E> extends LifeCycleObject implements
		Queue<E> {
	final String queueKey;
	private final QueueParams qParams;
	private final Queue<E> mq;

	/**
	 * @param queueDir
	 * @param queueKey
	 *            filename the file to use for keeping the persistent state
	 */
	public AbstractFileQueue(String queueKey, QueueParams qParams) {
		super(AbstractFileQueue.class);

		this.queueKey = queueKey;
		this.qParams = qParams;
		this.mq = new NotSynchronizedQueue<E>();
	}

	public String getQueueKey() {
		return queueKey;
	}

	public QueueParams getQueueParams() {
		return qParams;
	}

	static void close(Channel ch) {
		if (ch != null) {
			try {
				ch.close();
			} catch (IOException e) {
			}
		}
	}

	static void close(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	static void close(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
	}

	static void createEmptyFile_NOLOCK(File emptyFile) throws IOException {
		int cnt = 0;
		File newF = null;
		do {
			if (cnt > 0) {
				try {
					Thread.sleep(500);
				} catch (Throwable t) {
				}
			}
			FileUtil.makeEmptyFile(emptyFile);
			newF = new File(emptyFile.getAbsolutePath());
			cnt++;
			if (cnt >= 3) {
				throw new IOException("Could not create new file: "
						+ emptyFile.getAbsolutePath());
			}
		} while (!newF.exists());
	}

	abstract void activate() throws IOException, QueueTimeoutException;

	void mqClear() throws TechException {
		mq.clear();
	}

	abstract int countDel_NOLOCK() throws IOException;

	protected E mqPeek() throws TechException {
		return mq.peek();
	}

	protected E mqPoll() throws TechException {
		return mq.poll();
	}

	void mqPut(E entry) throws TechException, QueueTimeoutException {
		mq.put(entry);
	}

	int mqSize() {
		return mq.size();
	}

	public Queue<E> getMQ() {
		return mq;
	}

	public abstract String getDetail();

	abstract void sleep_NOLOCK();
}