package msgrouter.adapter.packet.parser.mgr;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import msgrouter.adapter.packet.col.Hex;
import msgrouter.adapter.packet.parser.ColFormat;
import msgrouter.adapter.packet.parser.DefaultPacketFormat;
import msgrouter.adapter.packet.parser.PacketFormat;

import org.apache.log4j.Logger;

import elastic.util.util.FilePathUtil;
import elastic.util.util.FileUtil;
import elastic.util.util.TechException;

public class DefaultPacketFormatFactory implements PacketFormatFactory {
	private static final Logger LOG = Logger
			.getLogger(DefaultPacketFormatFactory.class);

	private String columnClassPackage;
	private String partName = null;
	private int offset = 0;

	public final static String DELIMITER_COLUMN = "|";

	public PacketFormat newInstance(String filepath, String encoding,
			String columnClassPackage) throws TechException {
		this.columnClassPackage = columnClassPackage;

		if (LOG.isDebugEnabled()) {
			LOG.debug("loading " + filepath);
		}

		offset = 0;
		PacketFormat pktFormat = parse_0(filepath, encoding);
		offset = 0;

		return pktFormat;
	}

	private PacketFormat parse_0(String fpath, String encoding)
			throws TechException {
		String msgType = null;
		String fName = new File(fpath).getName();
		int ext_idx = fName.lastIndexOf(PacketFormatManager.PKT_EXT);
		if (ext_idx > 0) {
			msgType = fName.substring(0, ext_idx);
		}

		PacketFormat pktFormat = new DefaultPacketFormat(msgType);

		parse_1_File(pktFormat, fpath, encoding);
		return pktFormat;
	}

	private void parse_1_File(PacketFormat pktFormat, String fpath,
			String encoding) throws TechException {
		try {
			File file = new File(fpath);
			String[] lines = FileUtil.fileToStrings(file, encoding);
			if (lines == null) {
				throw new TechException("The packet format file '"
						+ file.getName() + "' does not have any contents.");
			}

			String baseDir = FilePathUtil.getBasePath(file);
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i].trim();
				if (line == null || line.length() == 0
						|| "#\r\n\t;".indexOf(line.charAt(0)) >= 0)
					continue;

				if (line.startsWith("_desc=")) {
					pktFormat.setDescription(line.substring("_desc=".length(),
							line.length()));
					continue;
				} else if (line.startsWith("_attribute.")) {
					String typeFormat = line.substring("_attribute.".length(),
							line.length());
					int idx = typeFormat.indexOf('=');
					if (idx > 0) {
						String type = typeFormat.substring(0, idx);
						String format = typeFormat.substring(idx + 1);
						if (!"".equals(format)) {
							pktFormat
									.setTypeAttriute(getColClass(type), format);
						}
					}
					continue;
				} else if (line.startsWith("_include=")) {
					String includeFname = line.substring("_include=".length(),
							line.length());
					parse_1_File(pktFormat, baseDir + File.separator
							+ includeFname, encoding);
					continue;
				} else if (partName == null && line.startsWith("<")
						&& !line.startsWith("</")) {
					int idx = line.indexOf('>');
					if (idx < 0) {
						throw new TechException(
								"pkt file parsing error: can not find '>' in the line="
										+ line + ", file=" + fpath);
					}
					int idx2 = line.indexOf(' ');
					partName = line.substring(1, idx2 > 0 ? idx2 : idx);
					pktFormat.declarePacketPartFormat(partName);
					continue;
				} else if (partName != null
						&& line.startsWith("</" + partName + ">")) {
					partName = null;
					continue;
				}

				if (partName == null) {
					throw new TechException(
							"Columns should be declared in a Part. i.e. <HEADER>...</HEADER>. source line=" + line);
				}

				ColFormat colFormat = parse_2_ColLine(lines[i]);
				if (colFormat.getAttribute() == null) {
					String attribute = pktFormat.getTypeAttribute(colFormat
							.getType());
					if (attribute != null) {
						colFormat.setAttribute(attribute);
					}
				}

				colFormat.setOffset(offset);
				if (LOG.isTraceEnabled()) {
					LOG.trace("parsed part[" + partName + "]'s column: "
							+ colFormat);
				}

				pktFormat.addColFormat(partName, colFormat);

				if (offset != -1 && colFormat.getLength() > 0) {
					offset += colFormat.getLength();
				} else {
					offset = -1;
				}
			}
		} catch (IOException e) {
			throw new TechException(e);
		}
	}

	private ColFormat parse_2_ColLine(String line) throws TechException {
		String id = null;
		String desc = null;
		Class typeClass = null;
		int length = -1;
		String format = null;
		String constStr = null;
		boolean isMsgType = false;

		StringTokenizer st = new StringTokenizer(line,
				DefaultPacketFormatFactory.DELIMITER_COLUMN);
		for (int j = 0; st.hasMoreTokens(); j++) {
			String token = st.nextToken().trim();
			if (j == 0) {
				int idx0 = token.indexOf("(msgType)");
				if (idx0 > 0) {
					id = token.substring(0, idx0).trim();
					isMsgType = true;
				} else {
					id = token;
					isMsgType = false;
				}
			} else if (j == 1) {
				desc = "".equals(token) || "null".equalsIgnoreCase(token) ? null
						: token;
			} else if (j == 2) {
				typeClass = getColClass(token);
			} else if (j == 3) {
				length = "".equals(token) || "null".equalsIgnoreCase(token) ? 0
						: Integer.parseInt(token);
			} else if (j == 4) {
				format = "".equals(token) || "null".equalsIgnoreCase(token) ? null
						: token;
			} else if (j == 5) {
				constStr = "".equals(token) || "null".equalsIgnoreCase(token) ? null
						: token;
			}
		}

		return new ColFormat(id, desc, typeClass, length, format, constStr,
				isMsgType);
	}

	private Class getColClass(String colTypeName) throws TechException {
		if (byte[].class.getSimpleName().equals(colTypeName)) {
			return byte[].class;
		} else if (String.class.getSimpleName().equals(colTypeName)) {
			return String.class;
		} else if (Integer.class.getSimpleName().equals(colTypeName)
				|| int.class.getSimpleName().equals(colTypeName)) {
			return Integer.class;
		} else if (Long.class.getSimpleName().equals(colTypeName)
				|| long.class.getSimpleName().equals(colTypeName)) {
			return Long.class;
		} else if (Hex.class.getSimpleName().equals(colTypeName)) {
			return Hex.class;
		} else {
			try {
				return Class.forName(columnClassPackage + "." + colTypeName,
						true, Thread.currentThread().getContextClassLoader());
			} catch (ClassNotFoundException e) {
				throw new TechException("Column class not found: "
						+ columnClassPackage + "." + colTypeName);
			}
		}
	}
}
