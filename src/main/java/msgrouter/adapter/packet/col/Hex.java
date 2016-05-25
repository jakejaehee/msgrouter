package msgrouter.adapter.packet.col;

import msgrouter.adapter.packet.PacketCol;
import elastic.util.util.DataUtil;

public class Hex implements PacketCol {

	private static final long serialVersionUID = 1100209334985885581L;

	private byte[] bytesValue = null;
	private String hexString = null;

	public Hex() {
	}

	public Hex(String hexString) {
		this.hexString = hexString;
	}

	public void setString(String hexString) {
		this.hexString = hexString;
	}

	public void setBytes(byte[] bytes, int offset, int length) {
		bytesValue = new byte[length];
		System.arraycopy(bytes, offset, bytesValue, 0, length);
	}

	public byte[] getBytes() {
		if (bytesValue == null) {
			bytesValue = DataUtil.hexStringToBytes(hexString);
		}
		return bytesValue;
	}

	public String toString() {
		if (hexString == null) {
			hexString = DataUtil.bytesToHexString(bytesValue);
		}
		return hexString;
	}
}
