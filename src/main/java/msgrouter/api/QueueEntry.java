package msgrouter.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import msgrouter.api.interfaces.Message;
import msgrouter.engine.MiscUtil;

public class QueueEntry implements Serializable {
	private static final long serialVersionUID = -8804731401324238917L;

	transient public static final short IVAL_REQ_SESSION_CRONJOB = 1;
	transient public static final short IVAL_REQ_NOSESSION_CRONJOB = 2;
	transient public static final short IVAL_REQ_SESSION_IN = 3;
	transient public static final short IVAL_RES_SESSION_BEAN_THR = 4;
	transient public static final short IVAL_RES_NOSESSION_BEAN_THR = 5;

	transient public static final String MAP_MSG_KEY_createdTime = "_createdTime";
	transient public static final String MAP_MSG_KEY_sn = "_sn";

	private final long createdTime;
	private final String sn;
	private String srcId = null;
	private String srcIp = null;
	private String srcSessionName = null;
	private String dstId = null;
	private String dstIp = null;
	private String dstSessionName = null;
	private String exception = null;

	private short reqres = 0;
	private Class sentTriggerClass = null;

	private List<Message> msgList = null;

	public QueueEntry() {
		this.createdTime = System.currentTimeMillis();
		this.sn = MiscUtil.nextSN();
	}

	public String getSN() {
		return this.sn;
	}

	public static QueueEntry copyMetaInfoFrom(QueueEntry src) {
		if (src != null) {
			QueueEntry qe = new QueueEntry();
			qe.setException(src.getException());
			qe.setSrcId(src.getSrcId());
			qe.setSrcSessionName(src.getSrcSessionName());
			qe.setDstId(src.getDstId());
			qe.setDstSessionName(src.getDstSessionName());
			qe.setReqres(src.getReqres());
			qe.setSentTriggerClass(src.getSentTriggerClass());
			return qe;
		}
		return null;
	}

	public void addMessage(Message msg) {
		if (msg != null) {
			if (msgList == null) {
				msgList = new ArrayList<Message>();
			}
			msgList.add(msg);

			setDstId(msg.getDstId());
			setDstIp(msg.getDstIp());
		}
	}

	public Message getMessage(int idx) {
		if (msgList == null)
			return null;
		return idx >= 0 && idx < msgList.size() ? msgList.get(idx) : null;
	}

	public Serializable removeMessage(int idx) {
		if (msgList == null)
			return null;
		return idx >= 0 && idx < msgList.size() ? msgList.remove(idx) : null;
	}

	public int getMessageCount() {
		return msgList != null ? msgList.size() : 0;
	}

	public String getMessageType() {
		Message msg = getMessage(0);
		return msg != null ? msg.getMessageType() : null;
	}

	public void setException(Exception exception) {
		if (exception != null) {
			this.exception = exception.getMessage();
			if (this.exception == null) {
				this.exception = exception.toString();
			}
		}
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public String getException() {
		return exception;
	}

	public void setReqres(short reqres) {
		this.reqres = reqres;
	}

	public short getReqres() {
		return reqres;
	}

	public void setSrcId(String srcId) {
		this.srcId = srcId;
	}

	public String getSrcId() {
		return srcId;
	}

	public void setSrcIp(String srcIp) {
		this.srcIp = srcIp;
	}

	public String getSrcIp() {
		return srcIp;
	}

	public void setSrcSessionName(String srcSessionName) {
		this.srcSessionName = srcSessionName;
	}

	public String getSrcSessionName() {
		return srcSessionName;
	}

	public void setDstId(String dstId) {
		this.dstId = dstId;
	}

	public String getDstId() {
		return dstId;
	}

	public void setDstIp(String dstIp) {
		this.dstIp = dstIp;
	}

	public String getDstIp() {
		return dstIp;
	}

	public void setDstSessionName(String dstSessionName) {
		this.dstSessionName = dstSessionName;
	}

	public String getDstSessionName() {
		return dstSessionName;
	}

	/**
	 * 생성일시
	 * 
	 * @return
	 */
	public long getCreatedTime() {
		return createdTime;
	}

	public void setSentTriggerClass(Class sentTriggerClass) {
		this.sentTriggerClass = sentTriggerClass;
	}

	public Class getSentTriggerClass() {
		return sentTriggerClass;
	}

	/**
	 * 간략 정보
	 * 
	 * @return
	 */
	public String getBriefing() {
		return MessageUtil.getBriefing(this);
	}

	public String getMessageString() {
		if (msgList == null || msgList.size() == 0) {
			return "";
		} else if (msgList.size() == 1) {
			return msgList.get(0).toString();
		} else {
			return msgList.toString();
		}
	}

	public String toString() {
		return MessageUtil.toString(this);
	}
}
