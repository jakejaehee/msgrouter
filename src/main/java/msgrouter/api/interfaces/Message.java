package msgrouter.api.interfaces;

import java.io.Serializable;
import java.util.Map;

public interface Message extends Serializable {
	public static final String COL_msgType = "_msgType";
	public static final String COL_dstId = "_dstId";
	public static final String COL_dstIp = "_dstIp";
	public static final String COL_dstSessionName = "_dstSessionName";
	
	public void setMessageType(String msgType);

	public String getMessageType();

	public void setDstId(String dstId);

	public String getDstId();

	public void setDstIp(String dstIp);

	public String getDstIp();

	public byte[] getBytes(String encoding);

	public void setBytes(byte[] bytes, String encoding);

	public void set(String key, Object value);

	public void setAll(Map map);

	public Map toMap();

	public Object get(String key);
}
