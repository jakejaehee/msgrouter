package msgrouter.engine.socket.server;

import elastic.util.net.SocketUtil;
import elastic.util.util.TechException;
import msgrouter.api.SocketConnection;
import msgrouter.api.interfaces.bean.ServerLoginer;
import msgrouter.engine.SessionContext;

public class ServerSessionFactory {
	/**
	 * Called whenever a connection is accepted on a server socket for which
	 * this factory was registered.
	 * 
	 * @param svr
	 * @param su
	 * @return a protocol handler instance (either new or pooled) that will
	 *         service the read and write operations on the socket. Returning
	 *         null will cause the server to refuse the connection.
	 */
	public ServerSession createSession(Server svr, SocketUtil su) throws TechException {
		String localIp = su.getLocalIp();
		int localPort = su.getLocalPort();
		String remoteIp = su.getRemoteIp();
		int remotePort = su.getRemotePort();

		SessionContext context = new SessionContext(svr, remoteIp, remotePort);

		ServerLoginer loginer = (ServerLoginer) svr.newLoginer();

		SocketConnection conn = new SocketConnection(context, svr.getServiceConfig().getConnectionProps(),
				svr.getServiceConfig().getRecverClass(), svr.getServiceConfig().getSenderClass(), su);

		ServerSession ss = new ServerSession(context, loginer, conn);
		context.setSession(ss);

		return ss;
	}
}
