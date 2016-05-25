package msgrouter.engine;

import msgrouter.constant.Const;
import elastic.util.util.RollingLoggerParams;

public class MessageLoggerParams extends RollingLoggerParams {

	private short logPer = Const.IVAL_MSG_LOG_PER_OFF;
	private Class formatClass = null;

	public short getLogPer() {
		return logPer;
	}

	public void setLogPer(short logPer) {
		this.logPer = logPer;
	}

	public Class getFormatClass() {
		return formatClass;
	}

	public void setFormatClass(Class formatClass) {
		this.formatClass = formatClass;
	}

	public MessageLoggerParams clone() {
		MessageLoggerParams dst = new MessageLoggerParams();
		dst.setDatePattern(getDatePattern());
		dst.setEncoding(getEncoding());
		dst.setPath(getRoot(), getFilename());
		dst.setLayoutName(getLayoutName());
		dst.setLayoutParams(getLayoutParams());
		dst.setLevel(getLevel());
		dst.setFormatClass(getFormatClass());
		dst.setLogPer(getLogPer());
		return dst;
	}
}
