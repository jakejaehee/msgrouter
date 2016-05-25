package msgrouter.engine.http.server;

import java.net.InetSocketAddress;

import msgrouter.api.interfaces.bean.ServerLoginer;
import msgrouter.constant.Key;
import msgrouter.engine.Container;
import msgrouter.engine.Service;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.ContainersConfig.ServiceBootstrapConfig;
import msgrouter.engine.event.Event;
import msgrouter.engine.event.EventInitProcMap;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.session.Session;
import org.simpleframework.transport.connect.SocketConnection;

public class MRHttpServer extends Service implements
		org.simpleframework.http.core.Container {
	private final ServiceBootstrapConfig sbc;
	private final ClassLoader cl;

	private SocketConnection httpSockConn = null;

	public MRHttpServer(ServiceBootstrapConfig sbc, ClassLoader cl,
			Container container) {
		super(MRHttpServer.class, sbc, cl, container);

		this.sbc = sbc;
		this.cl = cl;
	}

	public void run() {
		try {
			super.run();

			this.httpSockConn = new SocketConnection(this);
			this.httpSockConn.connect(new InetSocketAddress(getServiceConfig()
					.getServerPort()));

			ServiceEventHandler svcEventHandler = new ServiceEventHandler();
			new Thread(svcEventHandler).start();
		} catch (RuntimeException e) {
			logError(e);
			throw e;
		} catch (Throwable e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	class ServiceEventHandler implements Runnable {
		public void run() {
			try {
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
	}

	public int getPort() {
		return getServiceConfig().getServerPort();
	}

	public void handle(Request simpleReq, Response simpleRes) {
		try {
			final long reqTime = System.currentTimeMillis();

			MRHttpServerSession ss = null;
			Session httpSession = simpleReq.getSession(false);
			if (httpSession == null) {
				String ip = simpleReq.getClientAddress().getHostName();
				SessionContext context = new SessionContext(this, ip, getPort());
				ServerLoginer loginer = (ServerLoginer) newLoginer();
				ss = new MRHttpServerSession(context, loginer);
				context.setSession(ss);
				ss.handleRequest(simpleReq, simpleRes, reqTime);
			} else {
				ss = (MRHttpServerSession) httpSession.get(Key.KEY_MR_SESSION);
				if (ss != null) {
					ss.handleRequest(simpleReq, simpleRes, reqTime);
				}
			}
		} catch (Exception e) {
			logError(e);
		}
	}

	public boolean isBusy() {
		return false;
	}

	public void killEventHandler() {
		super.killEventHandler();

		if (httpSockConn != null) {
			try {
				httpSockConn.close();
			} catch (Exception e) {
				logError(e);
			}
		}
	}
}
