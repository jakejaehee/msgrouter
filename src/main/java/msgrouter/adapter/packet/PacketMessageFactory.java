package msgrouter.adapter.packet;

import msgrouter.api.interfaces.Message;
import msgrouter.api.interfaces.MessageFactory;

public class PacketMessageFactory implements MessageFactory {

	public Message createMessage() {
		return new PacketMessage();
	}

}
