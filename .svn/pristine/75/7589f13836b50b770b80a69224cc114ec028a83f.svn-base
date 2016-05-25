package msgrouter.engine.com.workerthread;

import java.util.List;

import msgrouter.api.interfaces.bean.AsyncBean;
import msgrouter.api.interfaces.bean.Bean;
import msgrouter.api.interfaces.bean.SyncBean;
import msgrouter.constant.Const;
import msgrouter.engine.MsgRouter;
import msgrouter.engine.NoSession;
import msgrouter.engine.config.rule.CronjobRules;
import msgrouter.engine.config.rule.InvokeRule;
import msgrouter.engine.config.rule.ProcessMap;
import msgrouter.engine.queue.NotSynchronizedQueue;
import msgrouter.engine.queue.QueueTimeoutException;
import elastic.util.lifecycle.LifeCycleObject;
import elastic.util.scheduler.Schedule;
import elastic.util.scheduler.ScheduleJob;
import elastic.util.scheduler.Scheduler;
import elastic.util.util.TechException;

public class NoSessionCronjob extends LifeCycleObject implements Runnable {

	private final NoSession nosession;

	private Scheduler scheduler = null;

	private final NotSynchronizedQueue<AsyncBeanOnCronjobInvoker> pendingAsyncBeans = new NotSynchronizedQueue<AsyncBeanOnCronjobInvoker>();

	public NoSessionCronjob(NoSession nosession) {
		super(NoSessionCronjob.class);

		this.nosession = nosession;
	}

	public void run() {
		try {
			nosession.getService().executeThreadInitializer();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		if (scheduler != null && scheduler.jobCount() > 0) {
			logInfo("cancels all " + ProcessMap.CONF_NOSESSION_CRONJOBS);
			scheduler.cancelAll();
		}

		List<CronjobRules> cronjobRulesList = nosession.getService()
				.getServiceConfig().getProcessMap().getCronjobRulesList();
		if (cronjobRulesList == null) {
			logDebug("There is no cronjob rule for service '"
					+ nosession.getService().getServiceId() + "'");
			return;
		}

		for (int i = 0; i < cronjobRulesList.size(); i++) {
			CronjobRules cronjobRules = (CronjobRules) cronjobRulesList.get(i);
			if (getClass() != cronjobRules.getCronjobClass()) {
				continue;
			}

			List<InvokeRule> invokeRuleList = cronjobRules.getInvokeRuleList();
			for (int irIdx = 0; irIdx < invokeRuleList.size(); irIdx++) {
				InvokeRule invokeRule = invokeRuleList.get(irIdx);
				if (invokeRule == null) {
					continue;
				}

				List<Schedule> schList = invokeRule.getScheduleList();
				if (schList != null && schList.size() > 0) {
					CronjobTask task = new CronjobTask(invokeRule);
					if (scheduler == null) {
						this.scheduler = new Scheduler();
					}
					scheduler.register(task, schList);
				}
			}
		}

		while (!currentLifeCycle().isShutdownMode()
				|| !currentLifeCycle().readyToShutdown()) {
			try {
				if (currentLifeCycle().sleep(Const.VAL_LOOP_SLEEP_MILLIS,
						Const.VAL_LOOP_SLEEP_NANOS)) {
					continue;
				}

				handlePendingAsyncBean();
			} catch (Throwable t) {
				logError(t);
			}
		} // while
	}

	private void handlePendingAsyncBean() throws TechException,
			QueueTimeoutException {
		AsyncBeanOnCronjobInvoker asyncBeanInvoker = pendingAsyncBeans.poll();
		if (asyncBeanInvoker != null) {
			asyncBeanInvoker.execute();
			if (asyncBeanInvoker.isRepeatable()) {
				pendingAsyncBeans.put(asyncBeanInvoker);
			}
		}
	}

	public void killEventHandler() {
		logInfo("cancels all " + ProcessMap.CONF_NOSESSION_CRONJOBS);
		if (scheduler != null) {
			scheduler.cancelAll();
		}
	}

	public boolean isBusy() {
		return false;
	}

	class CronjobTask extends ScheduleJob {
		InvokeRule invokeRule;
		Class beanClass = null;

		public CronjobTask(InvokeRule invokeRule) {
			super(nosession.getService().getServiceId() + "-"
					+ invokeRule.getBeanClass().getName());

			this.invokeRule = invokeRule;
			this.beanClass = invokeRule.getBeanClass();
		}

		public void run() {
			try {
				nosession.getService().executeThreadInitializer();

				Bean bean = MsgRouter.getInstance().getBeanCache()
						.getBean(nosession.getService(), nosession, beanClass);

				if (bean instanceof AsyncBean) {
					AsyncBean asyncBean = (AsyncBean) bean;
					AsyncBeanOnCronjobInvoker asyncBeanInvoker = new AsyncBeanOnCronjobInvoker(
							nosession.getService(), nosession, invokeRule,
							asyncBean);
					asyncBeanInvoker.execute();
					if (asyncBeanInvoker.isRepeatable()) {
						pendingAsyncBeans.put(asyncBeanInvoker);
					}
				} else if (bean instanceof SyncBean) {
					throw new RuntimeException(beanClass.getName() + ": "
							+ SyncBean.class.getSimpleName()
							+ " can not run as a "
							+ NoSessionCronjob.class.getSimpleName());
				}
			} catch (Throwable e) {
				logError(e);
			} finally {
			}
		}
	}
}
