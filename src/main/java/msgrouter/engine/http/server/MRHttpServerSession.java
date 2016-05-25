package msgrouter.engine.http.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import msgrouter.adapter.http.HttpReqMessage;
import msgrouter.adapter.http.HttpResMessage;
import msgrouter.api.QueueEntry;
import msgrouter.api.interfaces.bean.AsyncBean;
import msgrouter.api.interfaces.bean.Bean;
import msgrouter.api.interfaces.bean.Loginer;
import msgrouter.api.interfaces.bean.ServerLoginer;
import msgrouter.api.interfaces.bean.SyncBean;
import msgrouter.constant.Key;
import msgrouter.engine.MessageLogger;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.Router;
import msgrouter.engine.SessionContext;
import msgrouter.engine.config.rule.InvokeRule;
import msgrouter.engine.config.rule.MessageRule;
import msgrouter.engine.config.rule.RoutingRules;
import msgrouter.engine.queue.QueueTimeoutException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import elastic.util.util.TechException;

public class MRHttpServerSession extends msgrouter.engine.Session {
	public MRHttpServerSession(SessionContext ssContext, Loginer loginer) {
		super(ssContext, loginer, MRHttpServerSession.class);
	}

	public void run() {
	}

	public void handleRequest(final Request simpleReq,
			final Response simpleRes, final long reqTime) throws Exception {
		HttpReqMessage reqMsg = new HttpReqMessage();
		reqMsg.setParams(simpleReq.getForm());

		if (logDebugEnabled()) {
			String recvLog = MessageLogger.toDebugingLog("recv", this, reqMsg);
			if (recvLog != null)
				logDebug(recvLog);
		}

		QueueEntry recvQE = new QueueEntry();
		recvQE.setSrcId(getLoginId());
		recvQE.setSrcIp(getRemoteIp());
		recvQE.setSrcSessionName(getAlias());
		recvQE.addMessage(reqMsg);

		org.simpleframework.http.session.Session httpSession = simpleReq
				.getSession(false);
		if (httpSession == null) {
			ServerLoginer loginer = (ServerLoginer) getLoginer();
			if (loginer != null) {
				String loginId = loginer.onMessage(recvQE.getMessage(0),
						getSessionContext());
			}
		}

		// 주어진 메시지 타입에 해당하는 룰 획득
		MessageRule msgRule = getService().getServiceConfig().getProcessMap()
				.getMessageRule(reqMsg.getMessageType());
		if (msgRule == null) {
			if (logTraceEnabled())
				logTrace(MessageRule.class.getSimpleName()
						+ " is not found for received message '"
						+ recvQE.getBriefing() + "'");
			return;
		}

		RoutingRules routingRules = msgRule.getRoutingRules();
		if (routingRules != null) {
			Router.switchTo(recvQE, getService(), this, routingRules);
		}

		List<InvokeRule> invokeRuleList = msgRule.getInvokeRuleList();
		if (invokeRuleList != null && invokeRuleList.size() > 0) {
			for (int irIdx = 0; irIdx < invokeRuleList.size(); irIdx++) {
				InvokeRule invokeRule = invokeRuleList.get(irIdx);
				try {
					invokeResBizBean(simpleReq, simpleRes, invokeRule, recvQE,
							reqTime);
				} catch (Exception e) {
					logError(e);
				}
			}
		}// if
	}

	private void invokeResBizBean(Request simpleReq, Response simpleRes,
			InvokeRule invokeRule, QueueEntry iqe, long reqTime)
			throws TechException, QueueTimeoutException {

		Class beanClass = invokeRule.getBeanClass();
		Bean bean = MsgRouter.getInstance().getBeanCache()
				.getBean(getService(), this, beanClass);

		if (bean instanceof SyncBean) {
			throw new TechException(beanClass.getName() + ": "
					+ SyncBean.class.getSimpleName() + " can not run on a "
					+ MRHttpServerSession.class.getSimpleName());
		}

		AsyncBean asyncBean = (AsyncBean) bean;
		if (logTraceEnabled())
			logTrace(beanClass.getName() + ".onMessage()");

		QueueEntry oQE = asyncBean._onMessage(iqe.getMessage(0));
		if (oQE != null) {
			oQE.setReqres(QueueEntry.IVAL_RES_SESSION_BEAN_THR);
			oQE.setSrcId(getLoginId());
			oQE.setSrcIp(getRemoteIp());
			oQE.setSrcSessionName(getAlias());
		}

		try {
			org.simpleframework.http.session.Session httpSession = simpleReq
					.getSession(false);
			if (httpSession == null) {
				httpSession = simpleReq.getSession(true);
				httpSession.put(Key.KEY_MR_SESSION, this);
				if (logTraceEnabled())
					logTrace("new httpSession: " + httpSession.toString());
			}
		} catch (Exception e) {
			logError(e);
		}

		response(simpleReq, simpleRes, oQE);

		if (oQE != null) {
			RoutingRules routingRules = invokeRule.getRoutingRules(oQE
					.getMessageType());
			Router.switchTo(oQE, getService(), this, routingRules);
		}
	}

	private void response(Request simpleReq, Response simpleRes, QueueEntry oQE) {
		PrintStream ps = null;
		try {
			HttpResMessage msg = null;
			if (oQE != null) {
				msg = (HttpResMessage) oQE.getMessage(0);
			}
			ps = simpleRes.getPrintStream();
			if (msg != null) {
				ps.print(msg.getContent());
			} else {
				ps.println("");
			}
			if (logDebugEnabled()) {
				String log = MessageLogger.toDebugingLog("sent", this, msg);
				if (log != null)
					logDebug(log);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	public boolean isBusy() {
		return false;
	}

	private volatile boolean kill = false;

	public void killEventHandler() {
		if (kill) {
			return;
		}
		kill = true;
		super.killEventHandler();
	}
}
