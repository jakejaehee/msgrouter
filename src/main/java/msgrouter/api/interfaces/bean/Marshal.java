package msgrouter.api.interfaces.bean;

import msgrouter.api.interfaces.Message;
import elastic.util.util.TechException;

public abstract class Marshal extends Bean {

	private static final long serialVersionUID = 4576922611048986440L;

	public abstract Message marshalling(Message srcMsg) throws TechException;

}
