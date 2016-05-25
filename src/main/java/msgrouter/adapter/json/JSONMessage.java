package msgrouter.adapter.json;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import msgrouter.api.interfaces.Message;
import elastic.util.json.JSONObject;
import elastic.util.json.JSONUtil;

public class JSONMessage implements Message {

	private static final long serialVersionUID = -5899863405097515811L;

	private JSONObject json = null;

	public JSONMessage() {
		this.json = new JSONObject();
	}

	public JSONMessage(String jsonStr) {
		setAll((JSONObject) JSONUtil.toJSON(jsonStr));
	}

	public void setMessageType(String msgType) {
		set(Message.COL_msgType, msgType);
	}

	public void setJSON(byte[] bytes, String encoding) {
		try {
			JSONObject tmp = (JSONObject) JSONUtil.toJSON(new String(bytes,
					encoding));
			setAll(tmp);
			// Comment out by Jake Lee, 2015-08-17
			// String msgType = getMessageType();
			// setMessageType(msgType);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(encoding);
		}
	}

	public void setJSON(String jsonStr) {
		JSONObject tmp = (JSONObject) JSONUtil.toJSON(jsonStr);
		setAll(tmp);
		// Comment out by Jake Lee, 2015-08-17
		// String msgType = getMessageType();
		// setMessageType(msgType);
	}

	public String getMessageType() {
		return (String) get(Message.COL_msgType);
	}

	public void setDstId(String dstId) {
		set(Message.COL_dstId, dstId);
	}

	public String getDstId() {
		return (String) get(Message.COL_dstId);
	}

	public void setDstIp(String dstIp) {
		set(Message.COL_dstIp, dstIp);
	}

	public String getDstIp() {
		return (String) get(Message.COL_dstIp);
	}

	public byte[] getBytes(String encoding) {
		try {
			return json.toString().getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public void setBytes(byte[] bytes, String encoding) {
		try {
			setAll((JSONObject) JSONUtil.toJSON(new String(bytes, encoding)));
			// Comment out by Jake Lee, 2015-08-17
			// String msgType = getMessageType();
			// setMessageType(msgType);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(encoding);
		}
	}

	public void set(String colId, Object value) {
		json.put(adjustKey(colId), value);
	}

	private String adjustKey(String key) {
		return key;
//		return key.toUpperCase();
	}

	public void setAll(Map map) {
		if (map != null) {
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry en = (Entry) it.next();
				set((String) en.getKey(), en.getValue());
			}
			// Comment out by Jake Lee, 2015-08-17
			// String msgType = getMessageType();
			// setMessageType(msgType);
		}
	}

	public Map toMap() {
		Map map = new HashMap();
		Iterator it = json.entrySet().iterator();
		while (it.hasNext()) {
			Entry e = (Entry) it.next();
			map.put(e.getKey(), e.getValue());
		}
		return map;
	}

	public Object get(String colId) {
		return json.get(adjustKey(colId));
	}

	public Integer getInteger(String colId) {
		return json.getInteger(adjustKey(colId));
	}

	public String toString() {
		return json.toString();
	}
}