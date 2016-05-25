package msgrouter.engine;

import java.lang.management.RuntimeMXBean;

import com.sun.management.OperatingSystemMXBean;

import elastic.util.ElasticConfigurator;
import elastic.util.lifecycle.LifeCycle;
import elastic.util.util.TechException;
import sun.management.ManagementFactory;

public class Main {
	public static void main(String[] args) throws TechException {
		String elasticXml = System.getProperty("elasticXml");
		ElasticConfigurator.init(elasticXml);

		MsgRouter msgrouter = MsgRouter.getInstance();
		new LifeCycle(msgrouter).start();
	}

	public static double getCpuUsage() {
		com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
		long prevUpTime = runtimeMXBean.getUptime();
		long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
		double cpuUsage;
		try {
			Thread.sleep(500);
		} catch (Exception ignored) {
		}

		operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		long upTime = runtimeMXBean.getUptime();
		long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
		long elapsedCpu = processCpuTime - prevProcessCpuTime;
		long elapsedTime = upTime - prevUpTime;

		cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
		System.out.println("Java CPU: " + cpuUsage);
		return cpuUsage;
	}
}
