package msgrouter.adapter.packet.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import elastic.util.util.TechException;

public class DefaultPacketFormat implements PacketFormat {
	private static final long serialVersionUID = 8207911227300433892L;

	private transient static final String DEFAUL_PART_NAME = "_default part_";

	private final String msgType;
	private final ColFormat[] msgTypeCols;
	private final Map<String, ColFormat> allColFormatMap;
	private final List<ColFormat> allColFormatList;
	private final List<PacketPartFormat> partMap;
	private String desc = null;
	private Map<Class, String> typeAttributes = new HashMap<Class, String>();
	private int fixedLength = 0;

	public String toString() {
		return "{msgType:" + msgType + ", desc:" + desc + ", partMap:"
				+ partMap + "}";
	}

	public DefaultPacketFormat(String msgType) {
		this.msgType = msgType;
		this.allColFormatMap = new HashMap<String, ColFormat>();
		this.allColFormatList = new ArrayList<ColFormat>();
		this.partMap = new ArrayList<PacketPartFormat>();
		int msgTypeCnt = 0;
		StringTokenizer st = new StringTokenizer(msgType, "+");
		while (st.hasMoreTokens()) {
			st.nextToken();
			msgTypeCnt++;
		}
		this.msgTypeCols = new ColFormat[msgTypeCnt];
	}

	public String getMessageType() {
		return msgType;
	}

	public ColFormat[] getMessageTypeCols() {
		return msgTypeCols;
	}

	public void setDescription(String desc) {
		this.desc = desc;
	}

	public String getDescription() {
		return desc;
	}

	public final int getLength(String partName) {
		partName = partName != null ? partName : DEFAUL_PART_NAME;
		PacketPartFormat part = getPacketPartFormat(partName);
		return part != null ? part.getLength() : 0;
	}

	public void declarePacketPartFormat(String partName) {
		partName = partName != null ? partName : DEFAUL_PART_NAME;
		PacketPartFormat part = new PacketPartFormat(partName);
		partMap.add(part);
	}

	private PacketPartFormat getPacketPartFormat(String partName) {
		partName = partName != null ? partName : DEFAUL_PART_NAME;
		for (int p = 0; p < partMap.size(); p++) {
			PacketPartFormat ppd = partMap.get(p);
			if (ppd.getPartName().equals(partName)) {
				return ppd;
			}
		}
		if (DEFAUL_PART_NAME.equals(partName)) {
			PacketPartFormat ppf = new PacketPartFormat(DEFAUL_PART_NAME);
			partMap.add(ppf);
			return ppf;
		}
		return null;
	}

	public String[] getPartNames() {
		String[] pNames = new String[partMap.size()];
		for (int p = 0; p < partMap.size(); p++) {
			pNames[p] = partMap.get(p).getPartName();
		}
		return pNames;
	}

	public void addColFormat(ColFormat colFormat) {
		try {
			addColFormat(DEFAUL_PART_NAME, colFormat);
		} catch (TechException e) {
			throw new RuntimeException(e);
		}
	}

	public void addColFormat(String partName, ColFormat colFormat)
			throws TechException {
		PacketPartFormat part = getPacketPartFormat(partName);
		if (part == null) {
			throw new TechException(PacketPartFormat.class.getSimpleName()
					+ " '" + partName + "' should be declared first to add "
					+ ColFormat.class.getSimpleName() + ": " + colFormat);
		}
		part.addColFormat(colFormat);
		allColFormatMap.put(colFormat.getId(), colFormat);
		allColFormatList.add(colFormat);
		int colLen = colFormat.getLength();
		if (colLen > 0 && fixedLength != -1) {
			fixedLength += colLen;
		} else {
			fixedLength = -1;
		}
		if (colFormat.isMessageType()) {
			for (int c = 0; c < msgTypeCols.length; c++) {
				if (msgTypeCols[c] == null) {
					msgTypeCols[c] = colFormat;
					break;
				}
			}
		}
	}

	public int getFixedLength() {
		return fixedLength;
	}

	public void evaluate() throws TechException {
		int msgTypeColsCnt = 0;
		for (int c = 0; c < allColFormatList.size(); c++) {
			ColFormat colFormat = allColFormatList.get(c);
			if (colFormat.isMessageType()) {
				msgTypeColsCnt++;
			}
		}
		if (msgTypeColsCnt != msgTypeCols.length) {
			throw new TechException(
					"msgType column's number is not correct: msgType="
							+ msgType + ", actual msgType columns="
							+ msgTypeColsCnt + ". It should be "
							+ msgTypeCols.length);
		}
	}

	public final ColFormat getColFormat(String colId) {
		colId = colId.toUpperCase();
		return (ColFormat) allColFormatMap.get(colId);
	}

	public final List<ColFormat> getColFormatList(String partName) {
		PacketPartFormat part = getPacketPartFormat(partName);
		return part != null ? part.getColFormatList()
				: new ArrayList<ColFormat>();
	}

	public final List<ColFormat> getAllColFormatList() {
		return allColFormatList;
	}

	public final void setTypeAttriute(Class type, String attribute) {
		typeAttributes.put(type, attribute);
	}

	public final String getTypeAttribute(Class type) {
		return typeAttributes.get(type);
	}
}
