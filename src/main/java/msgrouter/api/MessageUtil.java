package msgrouter.api;

import msgrouter.api.interfaces.Message;

import org.apache.log4j.Logger;

import elastic.util.json.JSONStringUtil;

public class MessageUtil {

	private static final Logger LOG = Logger.getLogger(MessageUtil.class);

	static private final String MAP_MSG_KEY_srcId = "_srcId";
	static private final String MAP_MSG_KEY_srcIp = "_srcIp";
	static private final String MAP_MSG_KEY_srcSessionName = "_srcSessionName";
	static private final String MAP_MSG_KEY_exception = "_exception";

	private static String json(String key, Object value) {
		if (value instanceof String) {
			return "\"" + key + "\":\"" + value + "\"";
		} else if (value instanceof Integer) {
			return "\"" + key + "\":" + value;
		} else if (value instanceof Long) {
			return "\"" + key + "\":" + value;
		} else if (value instanceof Boolean) {
			return "\"" + key + "\":" + value;
		} else {
			return "\"" + key + "\":\"" + value + "\"";
		}
	}

	public static String toString(QueueEntry qe) {
		return "{"
				+ json(Message.COL_msgType, qe.getMessageType())
				+ ", "
				+ json(QueueEntry.MAP_MSG_KEY_sn, qe.getSN())
				+ ", "
				+ json(QueueEntry.MAP_MSG_KEY_createdTime, qe.getCreatedTime())
				+ ", "
				+ (qe.getSrcId() != null ? ", "
						+ json(MessageUtil.MAP_MSG_KEY_srcId, qe.getSrcId())
						: "")
				+ (qe.getSrcIp() != null ? ", "
						+ json(MessageUtil.MAP_MSG_KEY_srcIp, qe.getSrcIp())
						: "")
				+ (qe.getSrcSessionName() != null ? ", "
						+ json(MessageUtil.MAP_MSG_KEY_srcSessionName,
								qe.getSrcSessionName()) : "")
				+ (qe.getDstId() != null ? ", "
						+ json(Message.COL_dstId, qe.getDstId()) : "")
				+ (qe.getDstIp() != null ? ", "
						+ json(Message.COL_dstIp, qe.getDstIp()) : "")
				+ (qe.getDstSessionName() != null ? ", "
						+ json(Message.COL_dstSessionName,
								qe.getDstSessionName()) : "")
				+ (qe.getException() != null ? ", "
						+ json(MessageUtil.MAP_MSG_KEY_exception,
								JSONStringUtil.quote(qe.getException())) : "")
				+ ", "
				+ json("msg",
						JSONStringUtil.toJSONString(qe.getMessageString()))
				+ "}";
	}

	public static String getBriefing(Message msg) {
		return "{"
				+ json(Message.COL_msgType, msg.getMessageType())
				+ ", "
				+ (msg.getDstId() != null ? ", "
						+ json(Message.COL_dstId, msg.getDstId()) : "")
				+ (msg.getDstIp() != null ? ", "
						+ json(Message.COL_dstIp, msg.getDstIp()) : "") + "}";
	}

	public static String getBriefing(QueueEntry qe) {
		return "{" + json(Message.COL_msgType, qe.getMessageType()) + ", "
				+ json(QueueEntry.MAP_MSG_KEY_sn, qe.getSN()) + "}";
	}

	public static String qeStr(QueueEntry qe) {
		if (qe == null)
			return "";
		if (LOG.isTraceEnabled())
			return qe.toString();
		if (LOG.isDebugEnabled())
			return qe.getBriefing();
		return qe.getBriefing();
	}

	public static String msgStr(Message msg) {
		if (msg == null)
			return "";
		if (LOG.isTraceEnabled())
			return msg.toString();
		if (LOG.isDebugEnabled())
			return getBriefing(msg);
		return getBriefing(msg);
	}
}
