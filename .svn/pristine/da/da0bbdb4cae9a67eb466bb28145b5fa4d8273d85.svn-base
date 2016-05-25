package msgrouter.engine.queue;

import elastic.util.util.TechException;

/**
 * Non-blocking and non-synchronized queue. to make it thread-safe you should
 * use synchronized way outer of this queue.
 * 
 * @author jakelee70
 * 
 * @param <E>
 */
public class NotSynchronizedQueue<E> implements Queue<E> {
	private volatile Node<E> head = null;
	private volatile Node<E> tail = null;
	private volatile int size = 0;
	private int maxEntries = -1;
	private long timeoutMillis = 30000;
	private boolean timeoutExceptionHappened = false;

	public NotSynchronizedQueue() {
	}

	public NotSynchronizedQueue(int maxEntries, long timeoutMillis) {
		this.maxEntries = maxEntries;
		this.timeoutMillis = timeoutMillis;
	}

	public boolean isBusy() {
		return timeoutExceptionHappened && size >= maxEntries;
	}

	public void put(E entry) throws TechException, QueueTimeoutException {
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
					throw new QueueTimeoutException(
							"Timeout to enqueue. entries=" + size);
				}
			}
		}
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

	public E poll() {
		timeoutExceptionHappened = false;

		if (head == null) {
			return null;
		}
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

	public void clear() {
		timeoutExceptionHappened = false;

		size = 0;
		head = tail = null;
	}

	public E peek() {
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
			System.out.println("{size:" + size + ", head:" + head + ", tail:"
					+ tail + "}");
		}
	}
}
