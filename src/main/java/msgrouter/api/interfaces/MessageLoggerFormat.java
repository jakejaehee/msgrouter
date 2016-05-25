package msgrouter.api.interfaces;

public interface MessageLoggerFormat {
	public String toLog(byte[] msgBytes, int offset, int length);
}
