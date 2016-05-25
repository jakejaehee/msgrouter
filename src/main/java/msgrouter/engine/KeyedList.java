package msgrouter.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyedList<K, E> {
	private Map<K, E> map = null;
	private List<E> list = null;

	public String toString() {
		return list != null ? list.toString() : "";
	}

	public synchronized void put(K key, E value) {
		if (map == null) {
			map = new HashMap<K, E>();
			list = new ArrayList<E>();
		} else {
			E v = map.remove(key);
			if (v != null) {
				list.remove(v);
			}
		}
		map.put(key, value);
		list.add(value);
	}

	public synchronized void remove(K key) {
		if (map != null) {
			E v = map.remove(key);
			if (v != null) {
				list.remove(v);
			}
		}
	}

	public E get(K key) {
		return map != null ? map.get(key) : null;
	}

	public int size() {
		return list != null ? list.size() : 0;
	}

	public E getFirst() {
		return get(0);
	}

	public E get(int idx) {
		return size() > idx && idx >= 0 ? list.get(idx) : null;
	}

	public List<E> getList() {
		if (map == null) {
			map = new HashMap<K, E>();
			list = new ArrayList<E>();
		}
		return list;
	}

	public void clear() {
		if (map != null) {
			map.clear();
		}
		if (list != null) {
			list.clear();
		}
	}
}