package msgrouter.engine.socket.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import elastic.util.util.TechException;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.bean.ServerLoginer;
import msgrouter.constant.Const;
import msgrouter.engine.Service;
import msgrouter.engine.queue.QueueTimeoutException;

public final class TransferSelectorEventHandler implements SelectorEventHandler {

	private final Server svr;

	public TransferSelectorEventHandler(Server svr) {
		this.svr = svr;
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

		ServerSession ss = (ServerSession) selKey.attachment();

		if (selKey.isValid() && selKey.isWritable()) {
			ss.send();
		}
		if (selKey.isValid() && selKey.isReadable()) {
			long begin = System.currentTimeMillis();
			int cnt = 0;
			Message msg = null;
			do {
				/*
				 * 수신 큐가 busy 상태인 경우에는 메시지 수신을 하지 않고 false(미처리 상태)를 리턴한다.
				 */
				if (ss.isRecvQueueBusy())
					return false;

				msg = ss.recv();

				if (ss.currentLifeCycle() == null) {
					if (ss.getService().getServiceConfig()
							.getRoutingTarget() == Service.IVAL_ROUTING_TARGET_MSGROUTER_ID) {
						String loginId = ss.getSessionContext().getLoginId();
						if (loginId != null && !"".equals(loginId)) {
							svr.startSession(ss);
						}
					} else if (ss.getService().getServiceConfig()
							.getRoutingTarget() == Service.IVAL_ROUTING_TARGET_CUSTOM_ID) {
						if (msg != null) {
							String loginId = ((ServerLoginer) ss.getLoginer()).onMessage(msg, ss.getSessionContext());
							if (loginId != null && !"".equals(loginId)) {
								ss.getSessionContext().setLoginId(loginId);
								svr.startSession(ss);
							}
						}
					} else {
						if (msg != null) {
							svr.startSession(ss);
						}
					}
				}
				if (msg != null) {
					ss.handleRecvMessage(msg);

					/*
					 * 수신 메시지를 처리하는데 1초이상이 소요된 경우에는 메시지 한 건씩만 수신하도록 하고 이후 수신할
					 * 메시지가 더 있을 것으로 가정하고 false(미처리 상태)를 리턴한다.
					 */
					if (System.currentTimeMillis() - begin >= 1000)
						return false;
					cnt++;
				}

				/*
				 * 수신한 메시지의 갯수가 QOS.MAX_RECV_IN_BULK에 도달했을 경우에는 이후 수신할 메시지가 더 있을
				 * 것으로 가정하고 false(미처리 상태)를 리턴한다.
				 */
				if (Const.VAL_MAX_RECV_IN_BULK <= cnt)
					return false;

			} while (msg != null); // 수신한 메시지가 없을 경우 종료한다.
		}

		/*
		 * 수신한 모든 메시지에 대한 처리를 완료했을 경우 true(처리완료 상태)를 리턴한다.
		 */
		return true;
	}
}
