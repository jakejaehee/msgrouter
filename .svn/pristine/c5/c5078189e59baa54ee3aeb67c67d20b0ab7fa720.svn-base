package msgrouter.adapter.http;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import msgrouter.api.interfaces.Message;
import msgrouter.constant.Const;
import elastic.util.web.URLUtil;

public class HttpResMessage implements Message {

	private static final long serialVersionUID = 975009817398012028L;

	private Map params = null;
	private String resStr = null;

	public HttpResMessage() {
		params = new HashMap();
		set(Message.COL_msgType, Const.VAL_MSG_TYPE_DEFAULT);
	}

	public void setMessageType(String msgType) {
		set(Message.COL_msgType, msgType);
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
			return URLUtil.toQueryString(params, encoding).getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public void setBytes(byte[] bytes, String encoding) {
		try {
			String str = new String(bytes, encoding);
			params = URLUtil.queryStringToMap(str, encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(encoding);
		}
	}

	public void set(String key, Object value) {
		if (value != null) {
			params.put(key, value);
		}
	}

	public void setAll(Map map) {
		if (map != null) {
			params.putAll(map);
		}
	}

	public Map toMap() {
		Map map = new HashMap();
		Iterator it = params.entrySet().iterator();
		while (it.hasNext()) {
			Entry en = (Entry) it.next();
			Object key = en.getKey();
			Object val = en.getValue();
			map.put(key, val);
		}
		return map;
	}

	public Object get(String key) {
		return params.get(key);
	}

	public String toString() {
		return params.toString() + ", content=" + resStr;
	}

	public void setContent(String content) {
		this.resStr = content;
	}

	public String getContent() {
		return resStr;
	}

	public void setParams(Map params) {
		this.params = params;
	}
}
