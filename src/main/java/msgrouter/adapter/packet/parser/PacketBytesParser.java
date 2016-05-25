package msgrouter.adapter.packet.parser;

import java.io.UnsupportedEncodingException;
import java.util.List;

import msgrouter.adapter.packet.PacketCol;
import msgrouter.adapter.packet.PacketMessage;
import msgrouter.engine.MessageParsingException;

import org.apache.log4j.Logger;

import elastic.util.java.ReflectionUtil;
import elastic.util.util.DataUtil;
import elastic.util.util.TechException;

public class PacketBytesParser {
	static final Logger LOG = Logger.getLogger(PacketBytesParser.class);

	private PacketFormat pktFormat = null;
	private byte[] pktBytes = null;

	public PacketBytesParser(byte[] pktBytes, PacketFormat pktFormat) {
		this.pktBytes = pktBytes;
		this.pktFormat = pktFormat;
	}

	public void toPacketMessage(PacketMessage dst)
			throws MessageParsingException, TechException {
		if (pktBytes != null) {
			int off = 0;
			List<ColFormat> list = pktFormat.getAllColFormatList();
			for (int c = 0; c < list.size(); c++) {
				ColFormat colFormat = list.get(c);
				int len = 0;
				if (colFormat.getLength() > 0) {
					len = colFormat.getLength();
				} else {
					len = pktBytes.length - dst.getPacketLength();
				}

				Object value = toObject(pktBytes, off, len,
						colFormat.getType(), colFormat.getAttribute());
				dst.set(colFormat.getId(), value);

				/**
				if (LOG.isTraceEnabled()) {
					LOG.trace("bytes[" + pktBytes.length + "]->PacketMessage("
							+ dst.getMessageType() + "): "
							+ colFormat.toString(off, len, value));
				}
				*/

				off += len;
			}
		}
	}

	public static byte[] extractBytes(byte[] src, int srcOffset, int srcLen,
			int returnLen) {
		if (returnLen > 0) {
			int minLength = Math.min(src.length, srcOffset + srcLen);
			byte[] ret = new byte[returnLen];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = 0;
			}
			System.arraycopy(src, srcOffset, ret, 0,
					Math.min(minLength - srcOffset, returnLen));
			return ret;
		} else {
			int minLength = Math.min(src.length, srcOffset + srcLen);
			byte[] ret = new byte[minLength - srcOffset];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = 0;
			}
			System.arraycopy(src, srcOffset, ret, 0, minLength - srcOffset);
			return ret;
		}
	}

	public static Object toObject(String src, Class colType, String colAttribute)
			throws TechException {
		try {
			if (colType == String.class) {
				return src;
			} else if (colType == Integer.class || colType == int.class) {
				return Integer.parseInt(src);
			} else if (colType == Long.class || colType == long.class) {
				return Long.parseLong(src);
			} else if (ReflectionUtil.isCastable(colType, PacketCol.class)) {
				PacketCol pc = (PacketCol) colType.newInstance();
				pc.setString(src);
				return pc;
			} else {
				return src;
			}
		} catch (Throwable e) {
			throw new TechException(e);
		}
	}

	public static Object toObject(byte[] src, int srcOffset, int srcLen,
			Class colType, String colAttribute) throws TechException {
		try {
			if (colType == String.class) {
				int idx = srcOffset + srcLen - 1;
				for (; srcOffset <= idx; idx--) {
					if (src[idx] != 0 && src[idx] != ' ') {
						break;
					}
				}
				if (idx < srcOffset) {
					return "";
				}
				try {
					return new String(src, srcOffset, idx + 1 - srcOffset,
							colAttribute);
				} catch (UnsupportedEncodingException e) {
					return new String(src, srcOffset, idx + 1 - srcOffset);
				}
			} else if (colType == Integer.class || colType == int.class) {
				boolean littleEndian = PacketCol.FORMAT_littleEndian
						.equalsIgnoreCase(colAttribute);
				return DataUtil.bytesToInt(src, srcOffset, srcLen,
						!littleEndian);
			} else if (colType == Long.class || colType == long.class) {
				boolean littleEndian = PacketCol.FORMAT_littleEndian
						.equalsIgnoreCase(colAttribute);
				return DataUtil.bytesToLong(src, srcOffset, srcLen,
						!littleEndian);
			} else if (ReflectionUtil.isCastable(colType, PacketCol.class)) {
				PacketCol pc = (PacketCol) colType.newInstance();
				pc.setBytes(src, srcOffset, srcLen);
				return pc;
			} else {
				byte[] ret = new byte[srcLen];
				System.arraycopy(src, srcOffset, ret, 0, srcLen);
				return ret;
			}
		} catch (Throwable e) {
			throw new TechException(e);
		}
	}

	public static String extractMessageType(byte[] src, int srcOffset,
			int srcLen, PacketFormat defaultFormat) throws TechException {
		String msgType = null;
		ColFormat[] msgTypeColFormats = defaultFormat.getMessageTypeCols();
		for (int c = 0; c < msgTypeColFormats.length; c++) {
			Object obj = PacketBytesParser.toObject(src,
					msgTypeColFormats[c].getOffset(),
					msgTypeColFormats[c].getLength(),
					msgTypeColFormats[c].getType(),
					msgTypeColFormats[c].getAttribute());
			if (msgType == null) {
				msgType = obj.toString();
			} else {
				msgType += obj.toString();
			}
		}
		return msgType;
	}
}