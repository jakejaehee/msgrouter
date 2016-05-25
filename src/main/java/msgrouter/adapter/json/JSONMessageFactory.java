package msgrouter.adapter.json;

import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.MessageFactory;

public class JSONMessageFactory implements MessageFactory {

	public Message createMessage() {
		return new JSONMessage();
	}

}
