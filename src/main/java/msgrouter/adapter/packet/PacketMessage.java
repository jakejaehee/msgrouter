package msgrouter.adapter.packet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import msgrouter.adapter.packet.parser.ColFormat;
import msgrouter.adapter.packet.parser.PacketBytesParser;
import msgrouter.adapter.packet.parser.PacketFormat;
import msgrouter.adapter.packet.parser.PacketMessageParser;
import msgrouter.api.interfaces.Message;

import org.apache.log4j.Logger;

import elastic.util.util.CommonUtil;
import elastic.util.util.TechException;

public class PacketMessage implements Message {
	private transient static final Logger LOG = Logger
			.getLogger(PacketMessage.class);

	private static final long serialVersionUID = -7301579106852960308L;

	private transient PacketMessageParser pktMsgParser = null;
	private PacketFormat pktFormat = null;

	private Map<String, Object> columns = null;

	public PacketMessage() {
		this.columns = new LinkedHashMap<String, Object>();
	}

	public PacketMessage(PacketFormat pktFormat) throws TechException {
		this();
		setPacketFormat(pktFormat);
	}

	public void setPacketFormat(PacketFormat pktFormat) throws TechException {
		if (pktFormat == null) {
			throw new TechException(PacketFormat.class.getSimpleName()
					+ " is undefined for a "
					+ PacketMessage.class.getSimpleName());
		}
		this.pktFormat = pktFormat;

		setMessageType(pktFormat.getMessageType());
	}

	public int getPacketLength() {
		int total = 0;
		List<ColFormat> list = pktFormat.getAllColFormatList();
		for (int c = 0; c < list.size(); c++) {
			ColFormat colFormat = list.get(c);
			total += getColLength(colFormat);
		}
		return total;
	}

	public int getColLength(ColFormat colFormat) {
		int length = colFormat.getLength();
		if (length <= 0) {
			Object value = get(colFormat.getId());
			byte[] bytes = PacketMessageParser.toBytes(value,
					colFormat.getAttribute());
			length = bytes != null ? bytes.length : 0;
		}
		return length;
	}

	public int getColLength(Object value, ColFormat colFormat) {
		int length = colFormat.getLength();
		if (length <= 0) {
			byte[] bytes = PacketMessageParser.toBytes(value,
					colFormat.getAttribute());
			length = bytes != null ? bytes.length : 0;
		}
		return length;
	}

	public void set(String colId, byte[] value, int offset, int length) {
		int minLen = Math.min(value.length, offset + length);
		byte[] tmp = new byte[minLen - offset];
		System.arraycopy(value, 0, tmp, 0, tmp.length);
		set(colId, tmp);
	}

	public void set(String colId, Object value) {
		if (value != null) {
			columns.put(colId.toUpperCase(), value);
		}
	}

	public void setAll(Map map) {
		if (map != null) {
			String msgType = getMessageType();
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry en = (Entry) it.next();
				String colId = ((String) en.getKey()).toUpperCase();
				columns.put(colId, en.getValue());
			}
			setMessageType(msgType);
		}
	}

	public Map toMap() {
		Map map = new HashMap();
		Iterator it = columns.entrySet().iterator();
		while (it.hasNext()) {
			Entry e = (Entry) it.next();
			map.put(e.getKey(), e.getValue());
		}
		return map;
	}

	public Object get(String colId) {
		colId = colId.toUpperCase();
		ColFormat colFormat = pktFormat.getColFormat(colId);
		if (colFormat != null && colFormat.getConstValue() != null) {
			return colFormat.getConstValue();
		}
		return columns.get(colId);
	}

	public void setMessageType(String msgType) {
		ColFormat[] colFormats = pktFormat.getMessageTypeCols();
		StringTokenizer st = new StringTokenizer(msgType, "+");
		for (int c = 0; c < colFormats.length; c++) {
			String value = st.nextToken();
			try {
				Object obj = PacketBytesParser.toObject(value,
						colFormats[c].getType(), colFormats[c].getAttribute());
				set(colFormats[c].getId(), obj);
			} catch (TechException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public String getMessageType() {
		ColFormat[] colFormats = pktFormat.getMessageTypeCols();
		String msgType = get(colFormats[0].getId()).toString();
		for (int c = 1; c < colFormats.length; c++) {
			msgType += "+" + get(colFormats[c].getId()).toString();
		}
		return msgType;
	}

	public void setDstId(String dstId) {
		set(Message.COL_dstId, dstId);
	}

	public String getDstId() {
		return (String) get(Message.COL_dstId);
	}

	public void setDstIp(String dstIp) {
		set(Message.COL_dstIp, dstIp);
	}

	public String getDstIp() {
		return (String) get(Message.COL_dstIp);
	}

	public byte[] getBytes(String encoding) {
		try {
			PacketMessageParser pp = getPacketMessageParser();
			return pp.getBytes();
		} catch (TechException e) {
			throw new RuntimeException(e);
		}
	}

	public void setBytes(byte[] bytes, String encoding) {
		try {
			String msgType = getMessageType();
			PacketBytesParser pp = new PacketBytesParser(bytes, pktFormat);
			pp.toPacketMessage(this);
			setMessageType(msgType);
		} catch (TechException e) {
			throw new RuntimeException(e);
		}
	}

	public PacketFormat getPacketFormat() {
		return pktFormat;
	}

	private PacketMessageParser getPacketMessageParser() {
		if (pktMsgParser == null) {
			pktMsgParser = new PacketMessageParser(this);
		}
		return pktMsgParser;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<ColFormat> list = pktFormat.getAllColFormatList();
		int offset = 0;
		for (int c = 0; c < list.size(); c++) {
			ColFormat colFormat = list.get(c);
			Object value = get(colFormat.getId());
			int length = colFormat.getLength();
			if (LOG.isTraceEnabled()) {
				if (length <= 0) {
					byte[] bytes = PacketMessageParser.toBytes(value,
							colFormat.getAttribute());
					length = bytes != null ? bytes.length : 0;
				}
			}
			sb.append(colFormat.toString(offset, length, value)).append(
					CommonUtil.NEW_LINE);
			offset += length;
		}
		return sb.toString();
	}
}