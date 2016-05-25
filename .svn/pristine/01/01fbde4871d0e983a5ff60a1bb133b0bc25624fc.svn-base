package msgrouter.adapter.packet.parser;

import java.io.Serializable;

import org.apache.log4j.Logger;

import elastic.util.util.DataUtil;
import elastic.util.util.TechException;

/**
 * <code><xmp>
 * 0_ID | 1_DESCRIPTION | 2_TYPE | 3_LENGTH | 4_ATTRIBUTE | 5_Value Hex String
 * </xmp></code>
 */
public class ColFormat implements Serializable {
	private transient static final Logger LOG = Logger
			.getLogger(ColFormat.class);

	private static final long serialVersionUID = -1425961597210549853L;

	// 0_ID | 1_DESCRIPTION | 2_TYPE | 3_LENGTH | 4_ATTRIBUTE | 5_Value Hex
	// String
	private final String id;
	private final String desc;
	private final Class type;
	private int offset = -1;
	private int length = 0;
	private String attribute = null;
	private Object constValue = null;
	private byte[] constBytes = null;
	private final boolean isMsgType;

	public ColFormat(String id, String desc, Class type, int length,
			String attribute, String constStr, boolean isMsgType)
			throws TechException {
		this.id = id.toUpperCase();
		this.desc = desc;
		this.type = type;
		this.length = length;
		this.attribute = attribute;
		if (constStr != null) {
			this.constValue = PacketBytesParser.toObject(constStr, type,
					attribute);
			this.constBytes = PacketMessageParser.toBytes(constValue,
					attribute, length);
		}
		this.isMsgType = isMsgType;
	}

	public boolean isMessageType() {
		return isMsgType;
	}

	public String toString() {
		return toString(offset, length, null);
	}

	public String toString(int offset, int length, Object value) {
		if (LOG.isTraceEnabled()) {
			return "{id:"
					+ id
					+ ", desc:"
					+ desc
					+ ", type:"
					+ type.getSimpleName()
					+ ", offset:"
					+ offset
					+ ", length:"
					+ length
					+ (attribute != null ? ", attribute:" + attribute : "")
					+ (constValue != null ? ", const:" + constValue : "")
					+ (value != null ? ", value:"
							+ (value instanceof byte[] ? DataUtil
									.bytesToHexString((byte[]) value)
									: (value instanceof String ? value : String
											.valueOf(value))) : "") + "}";
		} else if (LOG.isDebugEnabled()) {
			return "{id:"
					+ id
					+ ", type:"
					+ type.getSimpleName()
					+ (value != null ? ", value:"
							+ (value instanceof byte[] ? DataUtil
									.bytesToHexString((byte[]) value)
									: (value instanceof String ? value : String
											.valueOf(value))) : "") + "}";
		} else {
			return "{id:"
					+ id
					+ (value != null ? ", value:"
							+ (value instanceof byte[] ? DataUtil
									.bytesToHexString((byte[]) value)
									: (value instanceof String ? value : String
											.valueOf(value))) : "") + "}";
		}
	}

	public final String getId() {
		return this.id;
	}

	public final String getDescription() {
		return desc;
	}

	public final Class getType() {
		return this.type;
	}

	public final int getLength() {
		return this.length;
	}

	public final String getAttribute() {
		return attribute;
	}

	public final void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public final Object getConstValue() {
		return constValue;
	}

	public final byte[] getConstBytes() {
		return constBytes;
	}

	public final void setOffset(int offset) {
		this.offset = offset;
	}

	public final int getOffset() {
		return offset;
	}
}
