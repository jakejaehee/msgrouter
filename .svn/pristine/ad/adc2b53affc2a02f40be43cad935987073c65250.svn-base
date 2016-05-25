package msgrouter.engine;

import msgrouter.api.interfaces.MessageLoggerFormat;
import elastic.util.util.DataUtil;

public class DefaultMessageLoggerFormat implements MessageLoggerFormat {

	public String toLog(byte[] msgBytes, int offset, int length) {
		if (msgBytes == null || msgBytes.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (int b = offset; b < length; b++) {
			sb.append(DataUtil.byteToHexString(msgBytes[b])).append(" ");
			if ((b + 1) % 10 == 0) {
				sb.append("  ");
			}
		}
		return sb.toString();
	}

}
