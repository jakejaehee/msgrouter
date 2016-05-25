package msgrouter.engine;

import msgrouter.api.interfaces.Message;
import msgrouter.constant.Const;

import org.apache.log4j.Logger;

import elastic.util.util.RollingLogger;

public class MessageLogger {
	private static final Logger LOG = Logger.getLogger(MessageLogger.class);

	private RollingLogger logger = null;
	private final MessageLoggerParams params;

	public void close() {
		if (this.logger != null)
			this.logger.close();
	}

	public MessageLogger() {
		params = new MessageLoggerParams();
	}

	public MessageLogger(MessageLoggerParams params) {
		this.params = params;
		this.logger = RollingLogger.getLogger(params);
	}

	public static String msg(SessionContext context, String action,
			String msgType, byte[] bytes, int offset, int length) {
		return msg(context, action, msgType, context.getMessageLoggerFormat()
				.toLog(bytes, offset, length));
	}

	public static String msg(SessionContext context, String action,
			String msgType, String msg) {
		return "["
				+ context.getRemoteIp()
				+ ":"
				+ context.getRemotePort()
				+ (context.getLoginId() != null ? ":" + context.getLoginId()
						: "") + "] " + action + " "
				+ (msgType != null ? msgType : "") + ": " + msg;
	}

	public void write(String log) {
		if (logger != null && params.getLogPer() != Const.IVAL_MSG_LOG_PER_OFF) {
			logger.info(log);
		}
	}

	public MessageLoggerParams getMessageLoggerParams() {
		return params;
	}

	public static String toDebugingLog(String action, Session ss, Message msg) {
		if (LOG.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append(action).append(" ").append(msg.getMessageType());
			if (LOG.isTraceEnabled())
				sb.append(": ").append(msg.toString());
			return sb.toString();
		} else {
			return null;
		}
	}
}
