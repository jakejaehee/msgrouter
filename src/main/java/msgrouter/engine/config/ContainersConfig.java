package msgrouter.engine.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import msgrouter.engine.MsgRouter;
import msgrouter.engine.Service;

import org.dom4j.Node;

import elastic.util.util.CommonUtil;
import elastic.util.util.TechException;
import elastic.util.xml.XmlReader;
import elastic.util.xml.XmlUtil;

public class ContainersConfig {
	private String currDir = null;
	private String srcConfText = null;
	private String encoding = null;
	private Map<String, ServiceBootstrapConfig> svcBootstrapConfMap = null;
	private Map<String, ServiceConfig> svcConfMap = null;

	public ContainersConfig(String currDir, String confText)
			throws TechException {
		this.currDir = currDir;
		this.srcConfText = confText;
		this.svcBootstrapConfMap = new HashMap<String, ServiceBootstrapConfig>();
		this.svcConfMap = new HashMap<String, ServiceConfig>();

		reset();
	}

	public void reset(String confText) throws TechException {
		this.srcConfText = confText;
		reset();
	}

	public void reset() throws TechException {
		this.svcBootstrapConfMap.clear();
		this.svcConfMap.clear();

		XmlReader xml = new XmlReader(srcConfText, null);
		encoding = xml.getEncoding();

		Node cntnrsNode = xml.getNode("/containers");
		if (cntnrsNode == null) {
			throw new TechException("There is no container configuration.");
		}

		List cntnrList = XmlUtil.getChildElements(cntnrsNode);
		for (int c = 0; cntnrList != null && c < cntnrList.size(); c++) {
			Node cntnrNode = (Node) cntnrList.get(c);
			if (!"container".equalsIgnoreCase(cntnrNode.getName())) {
				continue;
			}
			List svcList = XmlUtil.getChildElements(cntnrNode);
			for (int s = 0; svcList != null && s < svcList.size(); s++) {
				Node svcNode = (Node) svcList.get(s);
				if (!Service.class.getSimpleName().equalsIgnoreCase(
						svcNode.getName())) {
					continue;
				}

				ServiceBootstrapConfig sbc = new ServiceBootstrapConfig();
				sbc.setContainerType(XmlUtil.getAttributeStringValue(cntnrNode,
						"type"));
				sbc.setServiceTypeName(XmlUtil.getAttributeStringValue(svcNode,
						"type"));
				sbc.setServiceId(XmlUtil.getAttributeStringValue(svcNode, "id"));
				sbc.setDescription(XmlUtil.getAttributeStringValue(svcNode,
						"description"));
				sbc.setServicePath(XmlUtil.getAttributeStringValue(svcNode,
						"path"));
				sbc.setClasspath(XmlUtil.getAttributeStringValue(svcNode,
						"classpath"));

				svcBootstrapConfMap.put(sbc.getServiceId(), sbc);
			}
		}
	}

	public String getCurrentDir() {
		return currDir;
	}

	public List<String> getServiceIdList(String containerType) {
		return getServiceIdList(containerType, (short) -1);
	}

	public List<String> getServiceIdList(String containerType, short svcType) {
		ArrayList<String> list = new ArrayList<String>();
		Iterator<Entry<String, ServiceBootstrapConfig>> it = svcBootstrapConfMap
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ServiceBootstrapConfig> entry = it.next();
			ServiceBootstrapConfig sbc = entry.getValue();
			if (sbc.getContainerType().equals(containerType)
					&& sbc.getServiceType() == svcType) {
				list.add((String) entry.getKey());
			}
		}
		return list;
	}

	public List<ServiceBootstrapConfig> getServiceBootstrapConfigList(
			String containerType) {
		ArrayList<ServiceBootstrapConfig> list = new ArrayList<ServiceBootstrapConfig>();
		Iterator<Entry<String, ServiceBootstrapConfig>> it = svcBootstrapConfMap
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ServiceBootstrapConfig> entry = it.next();
			ServiceBootstrapConfig sbc = entry.getValue();
			if (sbc.getContainerType().equals(containerType)) {
				list.add(sbc);
			}
		}
		return list;
	}

	public ServiceConfig getServiceConfig(String svcId) throws TechException {
		return svcConfMap.get(svcId);
	}

	public void addServiceConfig(ServiceConfig svcConf) throws TechException {
		svcConfMap.put(svcConf.getServiceId(), svcConf);
	}

	public void deleteServiceConfig(String svcId) throws TechException {
		svcConfMap.remove(svcId);
	}

	public ServiceBootstrapConfig getServiceBootstrapConfig(String svcId) {
		return svcBootstrapConfMap.get(svcId);
	}

	public String getEncoding() {
		return encoding;
	}

	public String toText() {
		StringBuilder sb = new StringBuilder();

		sb.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>")
				.append(CommonUtil.NEW_LINE);
		sb.append("<container>").append(CommonUtil.NEW_LINE)
				.append(CommonUtil.NEW_LINE);

		sb.append("	<global>").append(CommonUtil.NEW_LINE);
		sb.append(
				ServiceConfig.toConfigText(MsgRouter.getInstance().getConfig()
						.getMainConfig().getGlobalQueueParams())).append(
				CommonUtil.NEW_LINE);
		sb.append("	</global>").append(CommonUtil.NEW_LINE)
				.append(CommonUtil.NEW_LINE);

		// socketContainer
		sb.append("\t<socketContainer>").append(CommonUtil.NEW_LINE);
		Iterator<Entry<String, ServiceConfig>> socIt = svcConfMap.entrySet()
				.iterator();
		while (socIt.hasNext()) {
			Entry<String, ServiceConfig> entry = socIt.next();
			String svcId = entry.getKey();
			ServiceConfig svcConf = entry.getValue();
			sb.append(svcConf.toText()).append(CommonUtil.NEW_LINE);
		}
		sb.append("\t</socketContainer>").append(CommonUtil.NEW_LINE)
				.append(CommonUtil.NEW_LINE);

		sb.append("</container>");
		return sb.toString();
	}

	public String toString() {
		return toText();
	}

	public class ServiceBootstrapConfig {
		private String containerType = null;
		private String serviceTypeName = null;
		private short serviceType = -1;
		private String serviceId = null;
		private String description = null;
		private String servicePath = null;
		private String classpath = null;

		public String getContainerType() {
			return containerType;
		}

		public void setContainerType(String containerType) {
			this.containerType = containerType;
		}

		public short getServiceType() {
			return serviceType;
		}

		public String getServiceTypeName() {
			return serviceTypeName;
		}

		public void setServiceTypeName(String serviceTypeName) {
			this.serviceTypeName = serviceTypeName;
			if (ServiceConfig.SVAL_SVC_TYPE_SERVER
					.equalsIgnoreCase(serviceTypeName)) {
				this.serviceType = Service.IVAL_SVC_TYPE_SERVER;
			} else if (ServiceConfig.SVAL_SVC_TYPE_CLIENT
					.equalsIgnoreCase(serviceTypeName)) {
				this.serviceType = Service.IVAL_SVC_TYPE_CLIENT;
			} else if (ServiceConfig.SVAL_SVC_TYPE_NOSESSION
					.equalsIgnoreCase(serviceTypeName)) {
				this.serviceType = Service.IVAL_SVC_TYPE_NOSESSION;
			} else {
				throw new RuntimeException("Unknown service type: "
						+ serviceTypeName);
			}
		}

		public String getServiceId() {
			return serviceId;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getServicePath() {
			return servicePath;
		}

		public void setServicePath(String configPath) {
			this.servicePath = currDir + File.separator + configPath;
		}

		public String getClasspath() {
			return classpath;
		}

		public void setClasspath(String classpath) {
			this.classpath = classpath;
		}
	}
}