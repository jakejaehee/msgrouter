package msgrouter.engine.queue;

/**
 * <code><xmp>
 * 	<dir>C:/APP/queue/MTOMS/EXABUS</dir>
 * 	<fileBlockEntries>2000</fileBlockEntries>
 * 	<inactiveTimeoutMinute>30</inactiveTimeoutMinute>
 * 	<memoryLoadEntries>4000</memoryLoadEntries>
 * 	<timeoutMillisecond>30000</timeoutMillisecond>
 * 	<persistent>false</persistent>
 * </xmp></code>
 */
public class QueueParams {
	public Integer qType = null;
	private String baseDir = null;
	public Integer fileBlockEntries = null;
	public Integer inactiveTimeoutMinute = null;
	public Boolean fileErrorFlexible = null;
	public Integer memoryLoadEntries = null;
	public Long timeoutMillisecond = null;
	public Boolean persistent = null;

	public String toString() {
		return "{qType:" + qType + ", baseDir:" + baseDir
				+ ", fileBlockEntries:" + fileBlockEntries
				+ ", inactiveTimeoutMinute:" + inactiveTimeoutMinute
				+ ", fileErrorFlexible:" + fileErrorFlexible
				+ ", memoryLoadEntries:" + memoryLoadEntries
				+ ", timeoutMillisecond:" + timeoutMillisecond
				+ ", persistent:" + persistent + "}";
	}

	public void setUndefinedAttributes(QueueParams src) {
		if (qType == null) {
			qType = src.getQueueType();
		}
		if (qType == Queue.QTYPE_FILE) {
			if (baseDir == null) {
				baseDir = src.getBaseDir();
			}
			if (fileBlockEntries == null) {
				fileBlockEntries = src.getFileBlockEntries();
			}
			if (inactiveTimeoutMinute == null) {
				inactiveTimeoutMinute = src.getInactiveTimeoutMinute();
			}
			if (fileErrorFlexible == null) {
				fileErrorFlexible = src.isFileErrorFlexible();
			}
			if (persistent == null) {
				persistent = src.isPersistent();
			}
		}
		if (memoryLoadEntries == null) {
			memoryLoadEntries = src.getMemoryLoadEntries();
		}
		if (timeoutMillisecond == null) {
			timeoutMillisecond = src.getTimeoutMillisecond();
		}
	}

	public void setAll(QueueParams src) {
		qType = src.getQueueType();
		if (qType == Queue.QTYPE_FILE) {
			baseDir = src.getBaseDir();
			fileBlockEntries = src.getFileBlockEntries();
			inactiveTimeoutMinute = src.getInactiveTimeoutMinute();
			fileErrorFlexible = src.isFileErrorFlexible();
			persistent = src.isPersistent();
		}
		memoryLoadEntries = src.getMemoryLoadEntries();
		timeoutMillisecond = src.getTimeoutMillisecond();
	}

	public Boolean isFileErrorFlexible() {
		return fileErrorFlexible;
	}

	public void setFileErrorFlexible(Boolean flexible) {
		this.fileErrorFlexible = flexible;
	}

	public Integer getQueueType() {
		return qType;
	}

	public void setQueueType(Integer qType) {
		this.qType = qType;
	}

	public Integer getFileBlockEntries() {
		return fileBlockEntries;
	}

	public void setFileBlockEntries(Integer fileBlockEntries) {
		this.fileBlockEntries = fileBlockEntries;
	}

	public Integer getInactiveTimeoutMinute() {
		return inactiveTimeoutMinute;
	}

	public void setInactiveTimeoutMinute(Integer inactiveTimeoutMinute) {
		this.inactiveTimeoutMinute = inactiveTimeoutMinute;
	}

	public Integer getMemoryLoadEntries() {
		return memoryLoadEntries;
	}

	public void setMemoryLoadEntries(Integer mqCapacity) {
		this.memoryLoadEntries = mqCapacity;
	}

	public Long getTimeoutMillisecond() {
		return timeoutMillisecond;
	}

	public void setTimeoutMillisecond(long timeoutMillisecond) {
		this.timeoutMillisecond = timeoutMillisecond;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public void setPersistent(Boolean persistent) {
		this.persistent = persistent;
	}

	public Boolean isPersistent() {
		return persistent != null ? persistent : false;
	}
}
