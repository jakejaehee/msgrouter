package msgrouter.engine.queue;

import elastic.util.util.TechException;

/**
 * Non-Blocking Queue
 * 
 * @author Jake Lee
 * 
 * @param <E>
 */
public class SynchronizedQueue<E> implements Queue<E> {
	private volatile Node<E> head = null;
	private volatile Node<E> tail = null;
	private volatile int size = 0;
	private int maxEntries = -1;
	private long timeoutMillis = 30000;
	private boolean timeoutExceptionHappened = false;

	public SynchronizedQueue() {
	}

	/**
	 * 
	 * @param maxEntries
	 *            Maximum number of entries. If less than 0 no limit.
	 * @param timeoutMillis
	 *            Timeout milliseconds to wait for putting. If less than 0 never
	 *            timeout.
	 */
	public SynchronizedQueue(int maxEntries, long timeoutMillis) {
		this.maxEntries = maxEntries;
		this.timeoutMillis = timeoutMillis;
	}

	public boolean isBusy() {
		return timeoutExceptionHappened && size >= maxEntries;
	}

	public final void put(final E entry) throws TechException,
			QueueTimeoutException {
		if (entry == null) {
			return;
		}

		if (maxEntries > 0 && size >= maxEntries) {
			long start = System.currentTimeMillis();
			while (size >= maxEntries) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
				if (timeoutMillis > 0
						&& System.currentTimeMillis() - start > timeoutMillis) {
					timeoutExceptionHappened = true;
					throw new QueueTimeoutException(
							"Timeout to enqueue. Entries=" + size);
				}
			}
		}
		synchronized (this) {
			Node<E> newE = new Node<E>(entry);
			if (head == null) {
				head = tail = newE;
				size = 1;
			} else {
				if (tail != null) {
					try {
						tail.next = newE;
						size++;
					} catch (NullPointerException e) {
					}
				}
				if (tail == null) {
					size = 1;
				}
				tail = newE;
			}
		}
	}

	public final E poll() {
		timeoutExceptionHappened = false;

		if (head == null) {
			return null;
		}

		synchronized (this) {
			Node<E> tmp = head;
			if (tmp != null) {
				head = tmp.next;
			}
			if (head == null) {
				tail = null;
				size = 0;
			}
			if (tmp != null) {
				if (size > 0) {
					size--;
				}
				return tmp.entry;
			} else {
				return null;
			}
		}
	}

	public final void clear() {
		timeoutExceptionHappened = false;

		if (head == null) {
			return;
		}
		synchronized (this) {
			size = 0;
			head = tail = null;
		}
	}

	public final E peek() {
		return head != null ? head.entry : null;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return head == null;
	}

	class Node<E> {
		E entry = null;
		Node<E> next = null;

		public Node(E entry) {
			this.entry = entry;
		}
	}

	public final void printInfo() {
		if (head == null) {
			System.out.println("{size:0, head:null, tail:null}");
		} else {
			synchronized (this) {
				System.out.println("{size:" + size + ", head:" + head
						+ ", tail:" + tail + "}");
			}
		}
	}
}
