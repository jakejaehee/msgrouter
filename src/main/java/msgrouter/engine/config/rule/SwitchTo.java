package msgrouter.engine.config.rule;

public class SwitchTo {
	private String svcId = null;
	private Class marshalClass = null;

	public void setServiceId(String svcId) {
		this.svcId = svcId;
	}

	public String getServiceId() {
		return svcId;
	}

	public void setMarshalClass(Class marshalClass) {
		this.marshalClass = marshalClass;
	}

	public Class getMarshalClass() {
		return marshalClass;
	}

	public String toString() {
		return "<switchTo service='" + svcId + "' marshalClass='"
				+ marshalClass.getName() + "'/>";
	}

	public void clear() {
		svcId = null;
		marshalClass = null;
	}
}
