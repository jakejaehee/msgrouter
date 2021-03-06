package msgrouter.adapter.packet.parser;

import java.io.UnsupportedEncodingException;
import java.util.List;

import msgrouter.adapter.packet.PacketCol;
import msgrouter.adapter.packet.PacketMessage;
import msgrouter.engine.MessageParsingException;

import org.apache.log4j.Logger;

import elastic.util.util.DataUtil;

public class PacketMessageParser {
	static final Logger LOG = Logger.getLogger(PacketMessageParser.class);

	private PacketMessage msg = null;

	public PacketMessageParser(PacketMessage msg) {
		this.msg = msg;
	}

	public byte[] getBytes() throws MessageParsingException {
		byte[] pktBytes = new byte[msg.getPacketLength()];
		int offset = 0;

		List<ColFormat> list = msg.getPacketFormat().getAllColFormatList();
		for (int c = 0; c < list.size(); c++) {
			ColFormat colFormat = list.get(c);
			Object value = msg.get(colFormat.getId());
			int length = msg.getColLength(value, colFormat);
			if (value != null) {
				byte[] bytes = toBytes(value, colFormat.getAttribute(), length);
				if (bytes != null) {
					int minLen = Math.min(length, bytes.length);
					System.arraycopy(bytes, 0, pktBytes, offset, minLen);
				}
			}
			/*
			if (LOG.isTraceEnabled()) {
				LOG.trace("PacketMessage(" + msg.getMessageType() + ")->bytes["
						+ pktBytes.length + "]: "
						+ colFormat.toString(offset, length, value));
			}
			*/

			offset += length;
		}
		return pktBytes;
	}

	public static byte[] toBytes(Object value, String colAttribute) {
		return toBytes(value, colAttribute, -1);
	}

	public static byte[] toBytes(Object value, String colAttribute, int returnLen) {
		if (value == null) {
			return returnLen > 0 ? new byte[returnLen] : null;
		}
		if (value instanceof byte[]) {
			if (returnLen <= 0) {
				return ((byte[]) value);
			} else {
				byte[] bytes = (byte[]) value;
				return PacketBytesParser.extractBytes(bytes, 0, bytes.length,
						returnLen);
			}
		} else if (value instanceof String) {
			byte[] bytes = null;
			try {
				bytes = ((String) value).getBytes(colAttribute);
			} catch (UnsupportedEncodingException e) {
				bytes = ((String) value).getBytes();
			}
			if (returnLen <= 0 || returnLen == bytes.length) {
				return bytes;
			} else {
				byte[] returnBytes = new byte[returnLen];
				int minLen = Math.min(bytes.length, returnLen);
				if (bytes.length < returnLen) {
					for (int i = 0; i < returnBytes.length; i++) {
						returnBytes[i] = ' ';
					}
				}
				System.arraycopy(bytes, 0, returnBytes, 0, minLen);
				return returnBytes;
			}
		} else if (value instanceof Integer) {
			boolean littleEndian = PacketCol.FORMAT_littleEndian
					.equalsIgnoreCase(colAttribute);
			if (returnLen <= 0) {
				return DataUtil.intToBytes((Integer) value, !littleEndian);
			} else {
				return DataUtil.intToBytes((Integer) value, returnLen,
						!littleEndian);
			}
		} else if (value instanceof Long) {
			boolean littleEndian = PacketCol.FORMAT_littleEndian
					.equalsIgnoreCase(colAttribute);
			if (returnLen <= 0) {
				return DataUtil.longToBytes((Long) value, !littleEndian);
			} else {
				return DataUtil.longToBytes((Long) value, returnLen,
						!littleEndian);
			}
		} else if (value instanceof PacketCol) {
			return ((PacketCol) value).getBytes();
		}
		return null;
	}
}