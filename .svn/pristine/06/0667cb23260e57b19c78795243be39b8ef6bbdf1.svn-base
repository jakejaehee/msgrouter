package msgrouter.engine.socket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

public class ServerSocketConfiguration {
	private SocketAddress socketAddress;
	private int backlog;
	private int bufsize;
	private boolean reuseAddress;

	public void setSocketAddress(SocketAddress socketAddress) {
		if (socketAddress == null)
			throw new IllegalArgumentException("socketAddress == null");
		this.socketAddress = socketAddress;
	}

	public SocketAddress getSocketAddress() {
		return socketAddress;
	}

	public void setBacklog(int backlog) {
		if (backlog < 0)
			throw new IllegalArgumentException("backlog < 0");
		this.backlog = backlog;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setReceiveBufferSize(int bufsize) {
		if (bufsize < 0)
			throw new IllegalArgumentException("bufsize < 0");
		this.bufsize = bufsize;
	}

	public int getReceiveBufferSize() {
		return bufsize;
	}

	public void setReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

	public boolean getReuseAddress() {
		return reuseAddress;
	}

	ServerSocket createSocket() throws IOException {
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ServerSocket socket = ssc.socket();
		socket.setReuseAddress(reuseAddress);
		if (bufsize > 0)
			socket.setReceiveBufferSize(bufsize);
		socket.bind(socketAddress, backlog);
		return socket;
	}
}