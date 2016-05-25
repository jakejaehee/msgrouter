package msgrouter.adapter.packet.parser;

import java.io.Serializable;
import java.util.List;

import elastic.util.util.TechException;

public interface PacketFormat extends Serializable {

	public String getMessageType();

	public ColFormat[] getMessageTypeCols();

	public void setDescription(String desc);

	public String getDescription();

	public ColFormat getColFormat(String colId);

	public int getFixedLength();

	public int getLength(String partName);

	public List<ColFormat> getColFormatList(String partName);

	public List<ColFormat> getAllColFormatList();

	public void addColFormat(ColFormat colFormat);

	public void addColFormat(String partName, ColFormat colFormat)
			throws TechException;

	public void declarePacketPartFormat(String partName);

	public void setTypeAttriute(Class type, String attribute);

	public String getTypeAttribute(Class type);
}
