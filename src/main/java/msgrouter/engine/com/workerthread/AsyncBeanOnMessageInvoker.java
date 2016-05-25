package msgrouter.engine.com.workerthread;

import msgrouter.api.MessageUtil;
import msgrouter.api.QueueEntry;
import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.bean.AsyncBean;
import msgrouter.engine.NoSession;
import msgrouter.engine.Router;
import msgrouter.engine.Service;
import msgrouter.engine.Session;
import msgrouter.engine.config.rule.InvokeRule;
import msgrouter.engine.config.rule.RoutingRules;
import msgrouter.engine.queue.QueueTimeoutException;

import org.apache.log4j.Logger;

import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.util.TechException;

public class AsyncBeanOnMessageInvoker {
	private static final Logger LOG = Logger.getLogger(AsyncBeanOnMessageInvoker.class);
	
	private final Service svc;
	private final LifeCycleObject lco;
	private final InvokeRule invokeRule;
	private final QueueEntry iqe;
	private final AsyncBean asyncBean;

	public AsyncBeanOnMessageInvoker(Service svc, LifeCycleObject lco,
			InvokeRule invokeRule, QueueEntry iqe, AsyncBean asyncBean) {
		this.svc = svc;
		this.lco = lco;
		this.invokeRule = invokeRule;
		this.iqe = iqe;
		this.asyncBean = asyncBean;
	}

	public boolean isRepeatable() {
		return asyncBean.isRepeatableOnMessage();
	}

	public void execute() throws TechException, QueueTimeoutException {
		if (lco instanceof Session) {
			if (((Session) lco).isClosed()) {
				asyncBean.stopRepeatableOnMessage();
				return;
			}
		}
		if (asyncBean.getLastTimeOnMessage() > 0
				&& System.currentTimeMillis()
						- asyncBean.getLastTimeOnMessage() < asyncBean
							.getRepeatIntervalOnMessage()) {
			return;
		}
		
		Message inMsg = iqe.getMessage(0);
		
		if (LOG.isDebugEnabled())
			lco.logDebug(asyncBean.getClass().getName() + ".onMessage(): IN=" + MessageUtil.qeStr(iqe));
		
		QueueEntry oqe = asyncBean._onMessage(inMsg);
		

		if (oqe == null) {
			if (LOG.isDebugEnabled())
				lco.logDebug(asyncBean.getClass().getName() + ".onMessage(): OUT=null");
			
		} else {
			if (LOG.isDebugEnabled())
				lco.logDebug(asyncBean.getClass().getName() + ".onMessage(): OUT=" + MessageUtil.qeStr(oqe));
			
			if (lco instanceof Session) {
				oqe.setReqres(QueueEntry.IVAL_RES_SESSION_BEAN_THR);
				oqe.setSrcId(((Session) lco).getLoginId());
				oqe.setSrcIp(((Session) lco).getRemoteIp());
				oqe.setSrcSessionName(((Session) lco).getAlias());
			} else if (lco instanceof NoSession) {
				oqe.setReqres(QueueEntry.IVAL_RES_NOSESSION_BEAN_THR);
			}

			RoutingRules routingRules = invokeRule.getRoutingRules(oqe
					.getMessageType());
			if (routingRules != null
					&& routingRules.getSentTriggerClass() != null) {
				oqe.setSentTriggerClass(routingRules.getSentTriggerClass());
			}

			if (lco instanceof Session) {
				Router.sendTo(oqe, svc, ((Session) lco));
				Router.switchTo(oqe, svc, ((Session) lco), routingRules);
			} else {
				Router.sendTo(oqe, svc, null);
				Router.switchTo(oqe, svc, null, routingRules);
			}
		}
	}
}
