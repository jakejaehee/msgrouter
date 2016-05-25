package msgrouter.api.interfaces.adapter;

import java.io.IOException;
import java.util.Map;

import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.Message;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.SessionContext;

public interface MessageRecver {

	public void setSessionContext(SessionContext context);

	public void setSocketConnection(SocketConnection conn);

	public void setProperties(Map props);

	public Message recv() throws IOException;
}
