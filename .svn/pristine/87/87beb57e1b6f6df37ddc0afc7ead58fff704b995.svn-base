package msgrouter.adapter.packet.parser.mgr;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import msgrouter.adapter.packet.parser.PacketFormat;

import org.apache.log4j.Logger;

import elastic.util.util.TechException;

public class PacketFormatManager {
	private static final Logger LOG = Logger
			.getLogger(PacketFormatManager.class);

	public static final String PKT_EXT = ".pkt";
	public static final FileFilter PKT_FILTER = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory()
					|| (file.isFile() && file.canRead() && file.getName()
							.endsWith(PKT_EXT));
		}
	};

	private String base = null;
	private String encoding = null;
	private PacketFormatFactory factory = null;
	private String columnClassPackage = null;
	private Map<String, Map<String, PacketFormat>> pktFormatMaps = null;

	public PacketFormatManager(String base, String encoding,
			String columnClassPackage, PacketFormatFactory factory)
			throws TechException {
		this.base = base;
		this.encoding = encoding;
		this.factory = factory;
		this.columnClassPackage = columnClassPackage;
		this.pktFormatMaps = new HashMap<String, Map<String, PacketFormat>>();

		loadPkt(new File(base), null);
	}

	public void loadPkt(File base, Map<String, PacketFormat> category)
			throws TechException {
		File[] files = base.listFiles(PKT_FILTER);
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (category == null) {
					if (files[i].isDirectory()) {
						Map<String, PacketFormat> _category = new HashMap<String, PacketFormat>();
						pktFormatMaps.put(files[i].getName(), _category);
						loadPkt(files[i], _category);
					}
				} else {
					if (files[i].isFile()) {
						PacketFormat pktFormat = factory.newInstance(
								files[i].getAbsolutePath(), encoding,
								columnClassPackage);
						category.put(pktFormat.getMessageType(), pktFormat);
					}
				}
			}
		}
	}

	public PacketFormat getPacketFormat(String category, String msgType) {
		Map<String, PacketFormat> map = pktFormatMaps.get(category);
		return map != null ? map.get(msgType) : null;
	}
}
