package msgrouter.engine.queue;

import elastic.util.util.TechException;

public interface Queue<E> {

	public static final int QTYPE_MEMORY = 1;
	public static final int QTYPE_FILE = 2;

	/**
	 * Adds an element to the tail of the queue.
	 * 
	 * @param entry
	 *            the element to add
	 * @throws TechException
	 *             if an error occurs
	 */
	public void put(E entry) throws TechException, QueueTimeoutException;

	public E poll() throws TechException;

	public E peek() throws TechException;

	public void clear() throws TechException;

	public int size();

	public boolean isEmpty();

	/**
	 * QueueTimeoutException이 발생했고 엔트리 최대갯수에 다달은 경우 busy 상태이다. poll()과 clear()가
	 * 호출되면 busy 상태가 해제된다.
	 * 
	 * @return
	 */
	public boolean isBusy();
}
