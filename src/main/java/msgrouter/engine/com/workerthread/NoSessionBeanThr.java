package msgrouter.engine.com.workerthread;

import java.util.List;

import msgrouter.api.QueueEntry;
import msgrouter.api.interfaces.bean.AsyncBean;
import msgrouter.api.interfaces.bean.Bean;
import msgrouter.api.interfaces.bean.SyncBean;
import msgrouter.constant.Const;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.NoSession;
import msgrouter.engine.Router;
import msgrouter.engine.config.rule.InvokeRule;
import msgrouter.engine.config.rule.MessageRule;
import msgrouter.engine.config.rule.RoutingRules;
import msgrouter.engine.queue.NotSynchronizedQueue;
import msgrouter.engine.queue.QueueTimeoutException;

import org.apache.log4j.Logger;

import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.util.TechException;

public class NoSessionBeanThr extends LifeCycleObject implements Runnable {
	private static final Logger LOG = Logger.getLogger(NoSessionBeanThr.class);

	private final NoSession nosession;
	private final NotSynchronizedQueue<AsyncBeanOnMessageInvoker> pendingAsyncBeans = new NotSynchronizedQueue<AsyncBeanOnMessageInvoker>();

	private QueueEntry recvQE = null;

	public NoSessionBeanThr(NoSession nosession) {
		super(NoSessionBeanThr.class);

		this.nosession = nosession;
	}

	public void run() {
		try {
			nosession.getService().executeThreadInitializer();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		while (!currentLifeCycle().isShutdownMode()
				|| !currentLifeCycle().readyToShutdown()) {
			try {
				if (currentLifeCycle().sleep(Const.VAL_LOOP_SLEEP_MILLIS,
						Const.VAL_LOOP_SLEEP_NANOS)) {
					continue;
				}

				handlePendingAsyncBean();

				recvQE = (QueueEntry) nosession.pollSWQ();
				if (recvQE == null) {
					continue;
				}

				Router.sendTo(recvQE, nosession.getService(), null);

				// 주어진 메시지 타입에 해당하는 룰 획득
				MessageRule msgRule = nosession.getService().getServiceConfig()
						.getProcessMap()
						.getMessageRule(recvQE.getMessageType());
				if (msgRule == null) {
					logTrace("Message rule is not found for received message '"
							+ recvQE.getBriefing() + "'");
					recvQE = null;
					continue;
				}

				RoutingRules routingRules = msgRule.getRoutingRules();
				if (routingRules != null) {
					Router.switchTo(recvQE, nosession.getService(), null,
							routingRules);
				}

				List<InvokeRule> invokeRuleList = msgRule.getInvokeRuleList();
				if (invokeRuleList != null && invokeRuleList.size() > 0) {
					for (int irIdx = 0; irIdx < invokeRuleList.size(); irIdx++) {
						InvokeRule invokeRule = invokeRuleList.get(irIdx);
						try {
							invokeResBean(invokeRule, recvQE);
						} catch (Exception e) {
							logError(e);
						}
					}
				}// if
				recvQE = null;
			} catch (Throwable t) {
				logError(t);
			} finally {
			}
		} // while
	}

	private void handlePendingAsyncBean() throws TechException,
			QueueTimeoutException {
		AsyncBeanOnMessageInvoker asyncBeanInvoker = pendingAsyncBeans.poll();
		if (asyncBeanInvoker != null) {
			asyncBeanInvoker.execute();
			if (asyncBeanInvoker.isRepeatable()) {
				pendingAsyncBeans.put(asyncBeanInvoker);
			}
		}
	}

	private void invokeResBean(InvokeRule invokeRule, QueueEntry iqe)
			throws TechException, QueueTimeoutException {
		Class beanClass = invokeRule.getBeanClass();
		Bean bean = MsgRouter.getInstance().getBeanCache()
				.getBean(nosession.getService(), nosession, beanClass);

		if (bean instanceof AsyncBean) {
			AsyncBean asyncBean = (AsyncBean) bean;
			AsyncBeanOnMessageInvoker asyncBeanInvoker = new AsyncBeanOnMessageInvoker(
					nosession.getService(), nosession, invokeRule, iqe,
					asyncBean);
			asyncBeanInvoker.execute();
			if (asyncBeanInvoker.isRepeatable()) {
				pendingAsyncBeans.put(asyncBeanInvoker);
			}
		} else if (bean instanceof SyncBean) {
			throw new TechException(beanClass.getName() + ": "
					+ SyncBean.class.getSimpleName() + " can not run on a "
					+ NoSessionBeanThr.class.getSimpleName());
		}
	}

	public void killEventHandler() {
	}

	public boolean isBusy() {
		return recvQE != null;
	}
}
