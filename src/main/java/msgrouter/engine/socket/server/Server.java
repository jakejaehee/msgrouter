package msgrouter.engine.socket.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import elastic.util.lifecycle.LifeCycle;
import elastic.util.net.SocketUtil;
import elastic.util.util.TechException;
import msgrouter.engine.Container;
import msgrouter.engine.Service;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import msgrouter.engine.event.Event;
import msgrouter.engine.event.EventInitProcMap;
import msgrouter.engine.queue.QueueTimeoutException;

public class Server extends Service implements Runnable, SelectorEventHandler {
	private final ServiceBootstrapConfig sbc;
	private final ClassLoader cl;

	private AcceptSelectorThr acceptThr = null;
	private TransferSelectorPool transferPool = null;
	private BlackList blackList = null;
	private ServerSocketChannel ssc = null;
	private boolean enabledForConnExLog = true;
	private boolean enabledForSsExLog = true;
	private final ServerSessionFactory ssFactory = new ServerSessionFactory();

	public Server(ServiceBootstrapConfig sbc, ClassLoader cl, Container container) {
		super(Server.class, sbc, cl, container);

		this.sbc = sbc;
		this.cl = cl;
		this.blackList = new BlackList(60000L, 10000, -10);
	}

	public void run() {
		try {
			Thread.currentThread().setContextClassLoader(cl);
			super.run();

			if (getServiceConfig().getMinAcceptThrs() < 1)
				throw new IllegalArgumentException("minAcceptThrs < 1");

			if (getServiceConfig().getMinTransferThrs() < 1)
				throw new IllegalArgumentException("minTransferThrs < 1");

			ServerSocketConfiguration ssconf = new ServerSocketConfiguration();
			InetSocketAddress socketAddress = new InetSocketAddress((InetAddress) null,
					getServiceConfig().getServerPort());
			ssconf.setSocketAddress(socketAddress);
			ServerSocket ss = ssconf.createSocket();
			ssc = ss.getChannel();

			acceptThr = new AcceptSelectorThr(ssc, this, this);
			new LifeCycle(acceptThr, "acceptSelThr", this).start();

			transferPool = new TransferSelectorPool(this, new TransferSelectorEventHandler(this),
					getServiceConfig().getMinTransferThrs());
			new LifeCycle(transferPool, "tranferPL", this).start();

			logInfo("listening to the port " + getServiceConfig().getServerPort());

			while (true) {
				Event event = getEvent();
				if (event != null) {
					if (event instanceof EventInitProcMap) {
						restartCronjobs();
					}
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
			}
		} catch (RuntimeException e) {
			logError(e);
			throw e;
		} catch (Throwable e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public boolean isEnabledForConnExLog() {
		return enabledForConnExLog;
	}

	public void setEnabledForConnExLog(boolean enabled) {
		enabledForConnExLog = enabled;
	}

	public boolean isMaxConnections() {
		return getNrOfConnections() >= getServiceConfig().getMaxConnections();
	}

	public boolean isEnabledForSsExLog() {
		return enabledForSsExLog;
	}

	public void setEnabledForSsExLog(boolean enabled) {
		enabledForSsExLog = enabled;
	}

	public boolean isMaxSessions() {
		return getNrOfSessions() >= getServiceConfig().getMaxSessions();
	}

	public int getPort() {
		return getServiceConfig().getServerPort();
	}

	public static String address(String ip) {
		if (ip.charAt(0) == '/')
			return ip.substring(1);
		return ip;
	}

	public void killEventHandler() {
		super.killEventHandler();

		closeServerSocketChannel(ssc);
	}

	public boolean isBusy() {
		return false;
	}

	private void closeServerSocketChannel(ServerSocketChannel ssc) {
		if (ssc != null) {
			try {
				ServerSocketChannel tmp = ssc;
				ssc = null;
				tmp.close();

				logInfo("closed port " + getServiceConfig().getServerPort());
			} catch (Throwable e) {
				logError(e);
			}
		}
	}

	public BlackList getBlackList() {
		return blackList;
	}

	private static String clientAddr(SocketChannel sc) {
		return sc.socket().getInetAddress().getHostAddress() + ":" + sc.socket().getPort();
	}

	/**
	 * 
	 * @param selKey
	 * @return 주어진 SelectionKey에 대한 송수신처리 완료여부. 모두 완료했을 경우 true, 미처리 송수신건이 있을 경우
	 *         false를 리턴한다.
	 * @throws IOException
	 * @throws TechException
	 * @throws QueueTimeoutException
	 */
	public boolean handleSelectorEvent(final SelectionKey selKey)
			throws IOException, TechException, QueueTimeoutException {
		if (!currentLifeCycle().isActiveMode())
			return false;
		try {
			if (!isMaxConnections()) {
				setEnabledForConnExLog(true);
				SocketChannel sc = ((ServerSocketChannel) selKey.channel()).accept();
				logDebug("accepted the client: " + clientAddr(sc));
				increaseNrOfConnections();

				if (sc != null) {
					if (!isMaxSessions()) {
						setEnabledForSsExLog(true);

						sc.configureBlocking(false);
						// sc.socket().setTcpNoDelay(true);
						sc.socket().setReceiveBufferSize(256 * 1024);
						sc.socket().setSendBufferSize(256 * 1024);

						SocketUtil su = new SocketUtil(sc);

						ServerSession session = ssFactory.createSession(this, su);
						if (session != null) {
							boolean success = false;
							try {
								success = transferPool.register(sc, session);
							} catch (Exception e) {
								logError(e);
							}
							return success;
						}
					} else {
						if (isEnabledForSsExLog()) {
							setEnabledForSsExLog(false);
							logWarn("rejected the client session due to exceeding limit of sessions("
									+ getServiceConfig().getMaxSessions() + "): " + clientAddr(sc));
						}
					}
				}
			} else {
				if (isEnabledForConnExLog()) {
					setEnabledForConnExLog(false);
					logWarn("rejected the client connection due to exceeding limit of connections("
							+ getServiceConfig().getMaxConnections() + "): " + selKey);
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (Throwable e) {
			throw new IOException(e.getMessage());
		}
		return false;
	}

	public void cancelSelectionKey(SelectionKey selKey, boolean logEnabled) {
		if (selKey == null)
			return;

		// Close channel
		if (logEnabled) {
			if (logTraceEnabled())
				logTrace("close channel=" + selKey.channel());
		}
		try {
			selKey.channel().close();
		} catch (Throwable t) {
		}
		try {
			selKey.cancel();
		} catch (Throwable t) {
		}

		decreaseNrOfConnections();

		// Kill LifeCycleObject (ServerSession)
		ServerSession session = (ServerSession) selKey.attachment();
		if (session != null) {
			LifeCycle lc = session.currentLifeCycle();
			if (lc != null) {
				lc.killNow();
			}
		}
	}
}
