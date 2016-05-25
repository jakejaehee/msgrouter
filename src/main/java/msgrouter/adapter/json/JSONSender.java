package msgrouter.adapter.json;

import java.io.IOException;
import java.util.Map;

import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.adapter.MessageSender;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.SessionContext;

public class JSONSender implements MessageSender {
	private SessionContext context = null;
	private SocketConnection conn = null;
	private String encoding = null;

	public int send(Message msg) throws IOException {
		byte[] bytes = msg.getBytes(encoding);
		if (bytes != null) {
			int len = conn.write(bytes, 0, bytes.length);
			if (len <= 0) {
				return len;
			}

			logMsg(msg.getMessageType(), msg);
			// logMsg(msg.getMessageType(), bytes, 0, bytes.length);
			return len;
		} else {
			return 0;
		}
	}

	private void logMsg(String msgType, Message msg) {
		context.getMessageLogger().write(
				MessageLogger.msg(context, "sent", msgType, msg.toString()));
	}

	private void logMsg(String msgType, byte[] bytes, int offset, int length) {
		context.getMessageLogger().write(
				MessageLogger.msg(context, "sent", msgType, bytes, offset,
						length));
	}

	public void setSessionContext(SessionContext context) {
		this.context = context;
	}

	public void setSocketConnection(SocketConnection conn) {
		this.conn = conn;
	}

	public void setProperties(Map props) {
		this.encoding = (String) props.get("encoding");
	}
}
