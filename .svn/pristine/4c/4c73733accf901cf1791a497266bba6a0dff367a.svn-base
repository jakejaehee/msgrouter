package msgrouter.adapter.packet.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PacketPartFormat implements Serializable {
	private static final long serialVersionUID = 4783276456022257239L;

	private String partName = null;
	private int length = 0;
	private final List<ColFormat> colFormatList;

	public String toString() {
		return "{partName:" + partName + ", length:" + length + "}";
	}

	public PacketPartFormat(String partName) {
		this.partName = partName;
		this.colFormatList = new ArrayList<ColFormat>();
	}

	public void addColFormat(ColFormat colFormat) {
		colFormatList.add(colFormat);
		if (colFormat.getLength() > 0) {
			length += colFormat.getLength();
		}
	}

	public String getPartName() {
		return partName;
	}

	public List<ColFormat> getColFormatList() {
		return colFormatList;
	}

	public int getLength() {
		return length;
	}
}
