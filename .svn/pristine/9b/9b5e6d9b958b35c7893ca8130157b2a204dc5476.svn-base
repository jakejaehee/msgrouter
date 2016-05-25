package msgrouter.adapter.json;

import java.io.IOException;
import java.util.Map;

import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.adapter.MessageRecver;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.SessionContext;

import org.apache.log4j.Logger;

public class JSONRecver implements MessageRecver {
	private static final Logger LOG = Logger.getLogger(JSONRecver.class);

	private SessionContext context = null;
	private SocketConnection conn = null;
	private String encoding = null;

	public Message recv() throws IOException {
		byte[] bytes = conn.readJson();
		if (bytes == null || bytes.length == 0) {
			return null;
		}

		Message msg = new JSONMessage();
		msg.setBytes(bytes, encoding);

		String msgType = msg.getMessageType();

		logMsg(msgType, msg);
		// logMsg(msg.getMessageType(), bytes, 0, bytes.length);

		return msg;
	}

	private void logMsg(String msgType, Message msg) {
		context.getMessageLogger().write(
				MessageLogger.msg(context, "recv", msgType, msg.toString()));
	}

	private void logMsg(String msgType, byte[] bytes, int offset, int length) {
		context.getMessageLogger().write(
				MessageLogger.msg(context, "recv", msgType, bytes, offset,
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
