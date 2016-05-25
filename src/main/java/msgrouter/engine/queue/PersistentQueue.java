package msgrouter.engine.queue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import elastic.util.util.DateUtil;
import elastic.util.util.FileUtil;
import elastic.util.util.ObjectInputStream;
import elastic.util.util.TechException;

public class PersistentQueue<E> extends AbstractFileQueue<E> {
	private static final byte D = 0;

	private volatile int qType = Queue.QTYPE_FILE;
	private volatile QFileInfo qfInfo = null;

	/**
	 * @param queueDir
	 * @param queueKey
	 *            filename the file to use for keeping the persistent state
	 */
	public PersistentQueue(String queueKey, QueueParams queueParams) {
		super(queueKey, queueParams);
		this.qfInfo = new QFileInfo();
		if (queueParams.getBaseDir() != null && qType == Queue.QTYPE_FILE) {
			qType = Queue.QTYPE_FILE;
		} else {
			qType = Queue.QTYPE_MEMORY;
		}
	}

	public void run() {
		try {
			if (!getQueueParams().isPersistent()) {
				String path = qDirPath();
				File dir = new File(path);
				if (dir.exists()) {
					int cnt = 0;
					do {
						logInfo("deletes qfile " + path);
						boolean deleted = FileUtil.removeDirectory(path);
						cnt++;
						if (!new File(path).exists() || cnt >= 3) {
							break;
						}
						try {
							Thread.sleep(500);
						} catch (Exception e) {
						}
					} while (true);
				}
			}
			if (qType == Queue.QTYPE_FILE) {
				File baseDir = new File(qDirPath());

				logInfo("starts queue: " + baseDir.getAbsolutePath());

				if (!baseDir.exists()) {
					baseDir.mkdirs();
				}
				activate();
			}
		} catch (Exception e) {
			if (getQueueParams().isFileErrorFlexible()) {
				getQueueParams().setQueueType(Queue.QTYPE_MEMORY);
				logError(e);
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	void activate() throws IOException, QueueTimeoutException {
		if (currentLifeCycle() == null) {
			return;
		}
		long time = System.currentTimeMillis();
		synchronized (this) {
			if (qType == Queue.QTYPE_FILE) {
				setQFileInfo();
				if (setQFile_NOLOCK()) {
					refillQueue_NOLOCK();
				}
				openQFile_NOLOCK();
				openDFile_NOLOCK();
			}
		}
		currentLifeCycle().setActiveMode();
		logTrace("activated: " + mkLog(time));
	}

	private void setQFileInfo() throws IOException {
		int block = getMaxBlockAndFix();
		qfInfo.deleted = countDel_NOLOCK();
		qfInfo.usefull = getQueueParams().getFileBlockEntries() * block
				+ countEntries_NOLOCK(block) - qfInfo.deleted;
	}

	public void clear() throws TechException {
		try {
			synchronized (this) {
				logInfo("clearing");
				close_NOLOCK();
				if (currentLifeCycle() != null
						&& qType == Queue.QTYPE_FILE) {
					if (qfInfo.file.exists()) {
						deleteFile(qfInfo.file);
					}
					if (qfInfo.dfile.exists()) {
						deleteFile(qfInfo.dfile);
					}
					qfInfo.deleted = 0;
					qfInfo.usefull = 0;
					openQFile_NOLOCK();
					openDFile_NOLOCK();
				}
			}
		} catch (Throwable t) {
			throw (t instanceof TechException) ? (TechException) t
					: new TechException(t);
		}
	}

	private void close_NOLOCK() throws TechException {
		if (currentLifeCycle() != null && qType == Queue.QTYPE_FILE) {
			closeQFile_NOLOCK();
			closeDFile_NOLOCK();
			mqClear();
		} else {
			mqClear();
		}
		logInfo("close");
	}

	private void openQFile_NOLOCK() throws FileNotFoundException {
		qfInfo.ch = new FileOutputStream(qfInfo.file, true).getChannel();
		qfInfo.out = new BufferedOutputStream(
				Channels.newOutputStream(qfInfo.ch));
	}

	private void openDFile_NOLOCK() throws FileNotFoundException {
		qfInfo.dch = new FileOutputStream(qfInfo.dfile, true).getChannel();
		qfInfo.dout = new BufferedOutputStream(
				Channels.newOutputStream(qfInfo.dch));
	}

	private void closeQFile_NOLOCK() {
		close(qfInfo.out);
		close(qfInfo.ch);
	}

	private void closeDFile_NOLOCK() {
		close(qfInfo.dout);
		close(qfInfo.dch);
	}

	private void deleteFile(File file) throws IOException {
		int i = 0;
		int sleep = 10;
		logTrace("deleting qfile=" + file.getAbsolutePath());
		while (!file.delete()) {
			if (++i == 5) {
				throw new IOException("Unable to delete file="
						+ file.getAbsolutePath());
			}
			logInfo("fail to delete file=" + file.getAbsolutePath()
					+ " and retrying..");
			try {
				Thread.sleep(sleep);
				sleep *= 2;
			} catch (InterruptedException ie) {
			}
		}
	}

	private void defragQueueFile_NOLOCK() throws IOException {
		// System.gc();
		File qfile = getQFile_NOLOCK(0);
		deleteFile(qfile);
		deleteFile(qfInfo.dfile);
		rollupQFile_NOLOCK(1);
		qfInfo.deleted = 0;
		setDFile_NOLOCK();
		setQFile_NOLOCK();
	}

	/**
	 * 
	 * @param from
	 *            시작 BLOCK NO
	 * @return 최종 BLOCK NO
	 * @throws QueueException
	 */
	private int rollupQFile_NOLOCK(int from) throws IOException {
		int max = 0;
		for (int block = from; true; block++) {
			File file = getQFile_NOLOCK(block);
			if (!file.exists()) {
				return max;
			}
			if (block == 0) {
				deleteFile(file);
				max = 0;
			} else {
				File dst = getQFile_NOLOCK(block - 1);
				boolean renamed = false;
				for (int i = 0; i < 5; i++) {
					renamed = file.renameTo(dst);
					if (renamed) {
						break;
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				}
				if (!renamed) {
					throw new IOException("Unable to rename queue file "
							+ file.getAbsolutePath() + " to "
							+ dst.getAbsolutePath());
				}
				max = block - 1;
			}
		}
	}

	public boolean isBusy() {
		return false;
	}

	public void killEventHandler() {
		synchronized (this) {
			try {
				close_NOLOCK();
			} catch (TechException e) {
				e.printStackTrace();
			}
		}
	}

	private String mkLog(long startTime) {
		return getDetail()
				+ "\r\n\t elapsed="
				+ DateUtil.toElapsedTimeString(System.currentTimeMillis()
						- startTime);
	}

	public String getDetail() {
		if (currentLifeCycle() != null && qType == Queue.QTYPE_FILE) {
			return "file=" + qfInfo.file.getAbsolutePath() + "\r\n\t messages="
					+ qfInfo.usefull + ", gabages=" + qfInfo.deleted
					+ ", in memory=" + mqSize();
		} else {
			return "messages=" + mqSize();
		}
	}

	public String getBrief() {
		if (currentLifeCycle() != null && qType == Queue.QTYPE_FILE) {
			return "messages=" + qfInfo.usefull;
		} else {
			return "messages=" + mqSize();
		}
	}

	private boolean isActive() {
		if (currentLifeCycle() != null && currentLifeCycle().isActiveMode()
				&& qfInfo.ch != null && qfInfo.ch.isOpen()
				&& qfInfo.out != null && qfInfo.dch != null
				&& qfInfo.dch.isOpen() && qfInfo.dout != null) {
			return true;
		}
		return false;
	}

	public E poll() throws TechException {
		E entry = null;
		try {
			if (currentLifeCycle() != null && qType == Queue.QTYPE_FILE) {
				if (!isActive()) {
					if (size() != 0) {
						activate();
					}
				}
				if (qfInfo.usefull == 0) {
					return null;
				}
				synchronized (this) {
					if (qfInfo.usefull > 0) {
						if (mqSize() == 0) {
							refillQueue_NOLOCK();
						}
						entry = mqPoll();
						if (entry != null) {
							qfInfo.dout.write(D);
							qfInfo.dout.flush();
							qfInfo.usefull--;
							qfInfo.deleted++;
							if (qfInfo.deleted == getQueueParams()
									.getFileBlockEntries()) {
								closeQFile_NOLOCK();
								closeDFile_NOLOCK();
								defragQueueFile_NOLOCK();
								openQFile_NOLOCK();
								openDFile_NOLOCK();
							}
							return entry;
						}
					}
				}
			} else {
				entry = mqPoll();
			}
		} catch (IOException e) {
			if (getQueueParams().isFileErrorFlexible()) {
				getQueueParams().setQueueType(Queue.QTYPE_MEMORY);
				if (entry == null) {
					entry = mqPoll();
				}
				logError(e);
			} else {
				throw new TechException(e);
			}
		} catch (Throwable t) {
			throw (t instanceof TechException) ? (TechException) t
					: new TechException(t);
		}
		return entry;
	}

	public E peek() throws TechException {
		E entry = null;
		try {
			if (currentLifeCycle() != null && qType == Queue.QTYPE_FILE) {
				if (!isActive()) {
					if (size() != 0) {
						activate();
					}
				}
				if (qfInfo.usefull == 0) {
					return null;
				}
				synchronized (this) {
					if (qfInfo.usefull > 0) {
						if (mqSize() == 0) {
							refillQueue_NOLOCK();
						}
						return mqPeek();
					}
				}
			} else {
				return mqPeek();
			}
		} catch (IOException e) {
			if (getQueueParams().isFileErrorFlexible()) {
				getQueueParams().setQueueType(Queue.QTYPE_MEMORY);
				logError(e);
				if (entry == null) {
					return mqPeek();
				}
			} else {
				throw new TechException(e);
			}
		} catch (Throwable t) {
			throw (t instanceof TechException) ? (TechException) t
					: new TechException(t);
		}
		return entry;
	}

	private void refillQueue_NOLOCK() throws IOException, QueueTimeoutException {
		if (qfInfo.usefull == 0) {
			return;
		}
		long time = System.currentTimeMillis();
		boolean closed = false;
		int block = 0;
		int skip = qfInfo.deleted;
		File file = null;
		FileChannel qch = null;
		InputStream qin = null;
		while (mqSize() < getQueueParams().getMemoryLoadEntries()
				&& block <= qfInfo.block()) {
			try {
				if (qfInfo.block() == block) {
					closeQFile_NOLOCK();
					closed = true;
				}
				file = getQFile_NOLOCK(block);
				qch = new FileInputStream(file).getChannel();
				qin = new BufferedInputStream(Channels.newInputStream(qch));
				if (skip > 0) {
					skipQFile_NOLOCK(qin, skip);
				}
				refillQueue_NOLOCK_(qin);
			} finally {
				close(qin);
				close(qch);
			}
			block++;
			skip = 0;
		}
		if (closed) {
			openQFile_NOLOCK();
		}
		// logTrace("refilled memory queue: " + mkLog(time));
	}

	public void put(E entry) throws TechException, QueueTimeoutException {
		boolean put = false;
		try {
			if (!isActive()) {
				activate();
			}
			synchronized (this) {
				if (currentLifeCycle() != null
						&& qType == Queue.QTYPE_FILE) {
					ObjectOutputStream oos = new ObjectOutputStream(qfInfo.out);
					oos.writeObject(entry);
					oos.flush();
					if (mqSize() < getQueueParams().getMemoryLoadEntries()
							&& mqSize() == qfInfo.usefull) {
						mqPut(entry);
						put = true;
					}
					qfInfo.usefull++;
					if (qfInfo.total() % getQueueParams().getFileBlockEntries() == 0) {
						closeQFile_NOLOCK();
						setQFile_NOLOCK();
						openQFile_NOLOCK();
					}
				} else {
					mqPut(entry);
					put = true;
				}
			}
		} catch (NullPointerException e) {
			try {
				activate();
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
			throw e;
		} catch (IOException e) {
			if (getQueueParams().isFileErrorFlexible()) {
				getQueueParams().setQueueType(Queue.QTYPE_MEMORY);
				if (!put) {
					mqPut(entry);
				}
				logError(e);
			} else {
				throw new TechException(e);
			}
		}
	}

	int countDel_NOLOCK() throws IOException {
		long deleted = 0;
		File dfile = getDFile_NOLOCK();
		FileChannel dch = null;
		try {
			deleted = dfile.length();
			if (dfile.exists() && deleted == 0) {
				dch = new FileInputStream(dfile).getChannel();
				deleted = dch.size();
			}
		} finally {
			close(dch);
		}
		return (int) deleted;
	}

	private int getMaxBlockAndFix() throws IOException {
		int max = 0;
		File qfile = null;
		for (int block = 0; true; block++) {
			qfile = getQFile_NOLOCK(block);
			if (!qfile.exists()) {
				if (block == 0) {
					File dfile = getDFile_NOLOCK();
					if (dfile.exists()) {
						deleteFile(dfile);
					}
				}
				qfile = getQFile_NOLOCK(block + 1);
				if (!qfile.exists()) {
					break;
				} else {
					max = rollupQFile_NOLOCK(block + 1);
				}
				break;
			}
			max = block;
		}
		return max;
	}

	private void refillQueue_NOLOCK_(InputStream in) throws IOException,
			QueueTimeoutException {
		while (mqSize() < getQueueParams().getMemoryLoadEntries()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(in);
				E entry = (E) ois.readObject();
				if (entry == null)
					break;
				mqPut(entry);
			} catch (TechException e) {
				logError(e);
			} catch (ClassNotFoundException e) {
				logError(e);
			} catch (ClassCastException e) {
				logError(e);
			} catch (EOFException e) {
				break;
			}
		}
	}

	private void setDFile_NOLOCK() throws IOException {
		qfInfo.dfile = getDFile_NOLOCK();
		if (!qfInfo.dfile.exists()) {
			createEmptyFile_NOLOCK(qfInfo.dfile);
		}
	}

	/**
	 * 
	 * @return 원래 파일이 존재했다면 true를 리턴한다.
	 * @throws IOException
	 */
	private boolean setQFile_NOLOCK() throws IOException {
		qfInfo.file = getQFile_NOLOCK(qfInfo.total()
				/ getQueueParams().getFileBlockEntries());
		boolean existed = false;
		if (!qfInfo.file.exists()) {
			createEmptyFile_NOLOCK(qfInfo.file);
			existed = false;
		} else {
			existed = true;
		}
		setDFile_NOLOCK();
		return existed;
	}

	private String qDirPath() {
		return getQueueParams().getBaseDir() + File.separator + queueKey;
	}

	private File getQFile_NOLOCK(int block) {
		return new File(qDirPath() + File.separator + "q" + block);
	}

	private File getDFile_NOLOCK() {
		return new File(qDirPath() + File.separator + "d");
	}

	public int size() {
		if (currentLifeCycle() != null && qType == Queue.QTYPE_FILE) {
			return qfInfo.usefull;
		} else {
			return mqSize();
		}
	}

	/**
	 * 주어진 갯수 만큼 건너뛰기
	 * 
	 * @param in
	 */
	private void skipQFile_NOLOCK(InputStream in, int entries)
			throws IOException {
		int cnt = 0;
		while (cnt < entries) {
			try {
				ObjectInputStream ois = new ObjectInputStream(in);
				E entry = (E) ois.readObject();
				if (entry == null)
					break;
				cnt++;
			} catch (ClassNotFoundException e) {
				logError(e);
			} catch (ClassCastException e) {
				logError(e);
			} catch (EOFException e) {
				break;
			}
		}
	}

	private int countEntries_NOLOCK(int block) throws IOException {
		File file = getQFile_NOLOCK(block);
		if (!file.exists()) {
			return 0;
		}
		int cnt = 0;
		FileChannel ch = null;
		InputStream in = null;
		try {
			ch = new FileInputStream(file).getChannel();
			in = new BufferedInputStream(Channels.newInputStream(ch));
			while (true) {
				try {
					ObjectInputStream ois = new ObjectInputStream(in);
					E entry = (E) ois.readObject();
					if (entry == null)
						break;
					cnt++;
				} catch (ClassNotFoundException e) {
					logError(e);
				} catch (ClassCastException e) {
					logError(e);
				} catch (EOFException e) {
					break;
				}
			}
		} finally {
			close(in);
			close(ch);
		}
		return cnt;
	}

	void sleep_NOLOCK() {
		if (currentLifeCycle() == null) {
			return;
		}
		currentLifeCycle().setSleepMode();
		if (qType == Queue.QTYPE_FILE) {
			closeQFile_NOLOCK();
			closeDFile_NOLOCK();
			try {
				mqClear();
			} catch (TechException e) {
				e.printStackTrace();
			}
		}
		logTrace("sleep");
	}

	public boolean isEmpty() {
		return size() <= 0;
	}

	class QFileInfo {
		volatile int usefull = 0;
		volatile int deleted = 0;

		public int total() {
			return usefull + deleted;
		}

		public int block() {
			return (usefull + deleted) / getQueueParams().getFileBlockEntries();
		}

		volatile File file = null;
		volatile FileChannel ch = null;
		volatile OutputStream out = null;

		volatile File dfile = null;
		volatile FileChannel dch = null;
		volatile OutputStream dout = null;

		public String toString() {
			return "{file:" + (file != null ? file.getPath() : "")
					+ ", usefull:" + usefull + ", deleted:" + deleted
					+ ", total:" + total() + ", block:" + block() + "}";
		}
	}
}