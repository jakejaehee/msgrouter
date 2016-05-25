package msgrouter.adapter.packet.parser.mgr;

import msgrouter.adapter.packet.parser.PacketFormat;
import elastic.util.util.TechException;

public interface PacketFormatFactory {
	public PacketFormat newInstance(String filepath, String encoding,
			String columnClassPackage) throws TechException;
}
