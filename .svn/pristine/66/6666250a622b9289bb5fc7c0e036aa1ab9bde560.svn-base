package msgrouter.api;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import msgrouter.admin.server.AuthEntryAdmin;
import msgrouter.api.interfaces.adapter.MessageRecver;
import msgrouter.api.interfaces.adapter.MessageSender;
import msgrouter.constant.Const;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.Service;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ServiceConfig;
import elastic.util.authmanager.AuthEntry;
import elastic.util.authmanager.AuthResult;
import elastic.util.java.ReflectionException;
import elastic.util.java.ReflectionUtil;
import elastic.util.net.SocketUtil;

public class SocketConnection {
	private SocketUtil su = null;
	private final SessionContext context;
	private final Map props;
	private final Class recverClass;
	private final Class senderClass;
	private MessageRecver recver = null;
	private MessageSender sender = null;

	private String encoding = Const.VAL_ENCODING_DEFAULT;
	private int recvTimeoutMillis = Const.VAL_SOC_RECV_TIMEOUT_MILLIS;

	public SocketConnection(SessionContext ssContext, Map props,
			Class recverClass, Class senderClass) {
		this.context = ssContext;
		this.props = props;
		this.recverClass = recverClass;
		this.senderClass = senderClass;
		setProperties(props);
	}

	public SocketConnection(SessionContext ssContext, Map props,
			Class recverClass, Class senderClass, SocketUtil su) {
		this.context = ssContext;
		this.props = props;
		this.recverClass = recverClass;
		this.senderClass = senderClass;
		this.su = su;
		setProperties(props);
	}

	private void setProperties(Map props) {
		if (props != null) {
			if (props.containsKey(ServiceConfig.CONN_PROP_RECV_TIMEOUT_MILLIS)) {
				this.recvTimeoutMillis = (Integer) props
						.get(ServiceConfig.CONN_PROP_RECV_TIMEOUT_MILLIS);
			}
			this.encoding = (String) props.get("encoding");
		}
		if (this.encoding == null || "".equals(this.encoding)) {
			this.encoding = Const.VAL_ENCODING_DEFAULT;
		}
	}

	/**
	 * Client side
	 * 
	 * @throws IOException
	 */
	public void connect(String loginId) throws IOException {
		try {
			Socket socket = new Socket(context.getRemoteIp(),
					context.getRemotePort());
			socket.setSoLinger(true, 0);
			socket.setReuseAddress(true);

			if (recvTimeoutMillis > 0) {
				socket.setSoTimeout(recvTimeoutMillis);
			}

			this.su = new SocketUtil(socket);

			if (context.getServiceConfig().getRoutingTarget() == Service.IVAL_ROUTING_TARGET_MSGROUTER_ID) {
				if (context.getServiceConfig().isClientService()) {
					AuthEntryAdmin aeAdmin = context.getService()
							.getAuthEntryAdmin();
					if (aeAdmin == null) {
						throw new IOException("There is no "
								+ AuthEntryAdmin.class.getSimpleName());
					}
					AuthEntry ae = aeAdmin.getAuthEntry(loginId);
					String password = ae.getPassword();
					byte[] bytes = new byte[100];
					byte[] loginIdBytes = loginId.getBytes("UTF-8");
					byte[] passwordBytes = password.getBytes("UTF-8");
					System.arraycopy(loginIdBytes, 0, bytes, 0,
							loginIdBytes.length);
					System.arraycopy(passwordBytes, 0, bytes, 50,
							passwordBytes.length);
					su.write(bytes, 0, bytes.length);
					context.setLoginId(loginId);

					logMsg("sent", "login-msg", bytes, 0, bytes.length);
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(t);
		}
	}

	public String getEncoding() {
		return encoding;
	}

	public Map getProperties() {
		return props;
	}

	public final void close() {
		if (su != null) {
			su.close();
		}
	}

	public final boolean isClosed() {
		if (su != null) {
			return su.isClosed();
		}
		return true;
	}

	public final String getRemoteIp() {
		return context.getRemoteIp();
	}

	public final int getRemotePort() {
		return context.getRemotePort();
	}

	public final String getLocalIp() {
		if (su != null) {
			return su.getLocalIp();
		}
		return null;
	}

	public final int getLocalPort() {
		if (su != null) {
			return su.getLocalPort();
		}
		return -1;
	}

	public byte[] readJson() throws IOException {
		return su.readJson();
	}

	public int read(byte[] bytes, int offset, int length) throws IOException {
		return su.read(bytes, offset, length);
	}

	public int write(byte[] bytes, int offset, int length) throws IOException {
		return su.write(bytes, offset, length);
	}

	private void logMsg(String action, String msgType, String msg) {
		context.getMessageLogger().write(
				MessageLogger.msg(context, action, msgType, msg));
	}

	private void logMsg(String action, String msgType, byte[] bytes,
			int offset, int length) {
		context.getMessageLogger().write(
				MessageLogger.msg(context, action, msgType, bytes, offset,
						length));
	}

	/**
	 * Server side
	 * 
	 * @return
	 * @throws IOException
	 */
	public MessageRecver getRecver() throws IOException {
		if (isClosed())
			throw new IOException("Connection is closed.");

		try {
			if (recver == null) {
				if (context.getServiceConfig().getRoutingTarget() == Service.IVAL_ROUTING_TARGET_MSGROUTER_ID) {
					if (context.getServiceConfig().isServerService()) {
						byte[] bytes = new byte[100];
						su.read(bytes, 0, bytes.length);
						logMsg("recv", "login-msg", bytes, 0, bytes.length);

						String loginId = new String(bytes, 0, 50, "UTF-8")
								.trim();
						String password = new String(bytes, 50, 50, "UTF-8")
								.trim();
						if ("".equals(loginId)) {
							throw new IOException("loginId is null.");
						}

						AuthEntryAdmin aeAdmin = context.getService()
								.getAuthEntryAdmin();
						if (aeAdmin == null) {
							throw new IOException(
									AuthEntryAdmin.class.getSimpleName()
											+ " is not defined");
						}

						AuthResult aeResult = aeAdmin
								.checkAndSetState(loginId, password,
										getRemoteIp() + ":" + getRemotePort());

						if (!AuthResult.CD_0_SUCCESS.equals((String) aeResult
								.get(AuthResult.KEY_CD))) {
							throw new IOException(
									(String) aeResult.get(AuthResult.KEY_MSG));
						}
						context.setLoginId(loginId);
					}
				}
				createRecver(recverClass);
			}
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(t);
		}

		return recver;
	}

	public MessageSender getSender() throws IOException {
		if (isClosed())
			throw new IOException("Connection is closed");
		if (sender == null) {
			createSender(senderClass);
		}
		return sender;
	}

	private void createRecver(Class recverClass) {
		try {
			recver = (MessageRecver) ReflectionUtil.newInstance(recverClass);
			recver.setSessionContext(context);
			recver.setProperties(props);
			recver.setSocketConnection(this);
		} catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}

	private void createSender(Class senderClass) {
		try {
			sender = (MessageSender) ReflectionUtil.newInstance(senderClass);
			sender.setSessionContext(context);
			sender.setProperties(props);
			sender.setSocketConnection(this);
		} catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}
}
