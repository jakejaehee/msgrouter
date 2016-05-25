package msgrouter.adapter.packet;

import java.io.Serializable;

public interface PacketCol extends Serializable {

	public static final String FORMAT_bigEndian = "bigEndian";
	public static final String FORMAT_littleEndian = "littleEndian";

	public void setBytes(byte[] bytes, int offset, int length);
	
	public void setString(String value);

	public byte[] getBytes();
}
