package msgrouter.engine;

import elastic.util.util.TechException;

public class MessageParsingException extends TechException {
	private static final long serialVersionUID = 1L;

	public MessageParsingException() {
		super();
	}

	public MessageParsingException(final String message) {
		super(message);
	}

	public MessageParsingException(final int code, final String message) {
		super(code, message);
	}

	public MessageParsingException(final Throwable cause) {
		super(cause);
	}
}
