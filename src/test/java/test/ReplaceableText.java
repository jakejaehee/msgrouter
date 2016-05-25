package test;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

public class ReplaceableText implements Serializable {
	private static final long serialVersionUID = 3499531328111451750L;

	private static String NEW_LINE = System.getProperty("line.separator") != null ? System
			.getProperty("line.separator") : "\n";

	private static final Integer CD_SUBSTITUTION = 1;

	private final String srcText;
	private Map<String, Object> values = new HashMap<String, Object>();
	private volatile TextCompiled compiled = null;
	private int ver = 3;

	public ReplaceableText(String srcText) {
		this.srcText = srcText;
	}

	public void setVersion(int ver) {
		this.ver = ver;
	}

	public void setValues(Map<String, Object> values) {
		Map map = keyToUpperCase(values);
		if (map != null) {
			this.values.putAll(map);
		}
	}

	public void setValue(String key, Object value) {
		this.values.put(key.toUpperCase(), value);
	}

	public String getSourceText() {
		return srcText;
	}

	public String toString() {
		if (compiled == null) {
			compileText();
		}
		return compiled.getText(values);
	}

	private static List<String> splitWithNewLine(String src) {
		List<String> list = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(src, NEW_LINE);
		while (st.hasMoreTokens()) {
			String line = st.nextToken();
			list.add(line);
		}
		return list;
	}

	public static final int indexOfNonSpace(String strObj) {
		if (strObj == null)
			return -1;
		int i = 0, length = strObj.length();
		for (; (i < length)
				&& ((strObj.charAt(i) <= ' ') || (strObj.charAt(i) == 65279)); i++)
			;
		return i != length ? i : -1;
	}

	private int indexOfEndOfVar_ver1(char ch, int b, String line) {
		if (ch == '$' && b + 2 < line.length()) {
			int e = line.indexOf('$', b + 1);
			if (e > 0) {
				return e;
			}
		}
		return -1;
	}

	private int indexOfEndOfVar_ver2(char ch, int b, String line) {
		if (ch == '$' && b + 3 < line.length()) {
			if (line.charAt(b + 1) == '{') {
				int e = line.indexOf('}', b + 1);
				if (e > 0) {
					return e;
				}
			}
		}
		return -1;
	}

	private void compileText() {
		List<Object> textList = new ArrayList<Object>();
		int subIdx = 0;
		String[] tmpSubs = new String[1000];

		List<String> lines = splitWithNewLine(srcText);
		for (int i = 0; i < lines.size(); i++) {
			String line = (String) lines.get(i);

			int c = indexOfNonSpace(line);
			if (c == -1) {
				textList.add(line);
				continue;
			} else if (c > 0) {
				textList.add(line.substring(0, c));
			}

			StringBuilder sb = new StringBuilder();
			for (int b = 0; b < line.length();) {
				char ch = line.charAt(b);
				if (ver == 1) {
					int e = indexOfEndOfVar_ver1(ch, b, line);
					if (e > 0) {
						if (validVariableName(line, b + 1, e - 1)) {
							tmpSubs[subIdx++] = line.substring(b + 1, e)
									.toUpperCase();
							if (sb.length() > 0) {
								textList.add(sb.toString());
								sb = new StringBuilder();
							}
							textList.add(CD_SUBSTITUTION);
							b = e + 1;
							continue;
						}
					}
				} else if (ver == 2) {
					int e = indexOfEndOfVar_ver2(ch, b, line);
					if (e > 0) {
						if (validVariableName(line, b + 2, e - 1)) {
							tmpSubs[subIdx++] = line.substring(b + 2, e)
									.toUpperCase();
							if (sb.length() > 0) {
								textList.add(sb.toString());
								sb = new StringBuilder();
							}
							textList.add(CD_SUBSTITUTION);
							b = e + 1;
							continue;
						}
					}
				} else {
					int e = indexOfEndOfVar_ver1(ch, b, line);
					if (e > 0) {
						if (validVariableName(line, b + 1, e - 1)) {
							tmpSubs[subIdx++] = line.substring(b + 1, e)
									.toUpperCase();
							if (sb.length() > 0) {
								textList.add(sb.toString());
								sb = new StringBuilder();
							}
							textList.add(CD_SUBSTITUTION);
							b = e + 1;
							continue;
						}
					} else {
						e = indexOfEndOfVar_ver2(ch, b, line);
						if (e > 0) {
							if (validVariableName(line, b + 2, e - 1)) {
								tmpSubs[subIdx++] = line.substring(b + 2, e)
										.toUpperCase();
								if (sb.length() > 0) {
									textList.add(sb.toString());
									sb = new StringBuilder();
								}
								textList.add(CD_SUBSTITUTION);
								b = e + 1;
								continue;
							}
						}
					}
				}
				sb.append(ch);
				b++;
			}
			if (sb.length() > 0) {
				textList.add(sb.toString());
			}
			if (i < lines.size() - 1)
				textList.add(NEW_LINE);
		}

		String[] alts = new String[subIdx];
		System.arraycopy(tmpSubs, 0, alts, 0, alts.length);

		compiled = new TextCompiled(textList, alts);
	}

	private static boolean validVariableName(String str, int from, int to) {
		for (int c = from; c <= to; c++) {
			if (!validVariableChar(str.charAt(c))) {
				return false;
			}
		}
		return true;
	}

	private static boolean validVariableChar(char ch) {
		if ((48 <= ch && ch <= 57) || (65 <= ch && ch <= 90) || ch == '_'
				|| ch == '[' || ch == ']' || ch == '.'
				|| (97 <= ch && ch <= 122)) {
			return true;
		}
		return false;
	}

	private static Map keyToUpperCase(Map row) {
		if (row == null) {
			return row;
		}
		Map ret = new HashMap();
		Iterator it = row.entrySet().iterator();
		while (it.hasNext()) {
			Entry en = (Entry) it.next();
			Object key = en.getKey();
			if (key instanceof String) {
				ret.put(((String) key).toUpperCase(), en.getValue());
			} else {
				ret.put(key, en.getValue());
			}
		}
		return ret;
	}

	class TextCompiled {
		private final List textList;
		private final String[] vars;

		public TextCompiled(List textList, String[] vars) {
			this.textList = textList;
			this.vars = vars;
		}

		public String getText(Map record) {
			StringBuilder sb = new StringBuilder();

			int cnt = textList.size();
			if (cnt == 1) {
				sb.append((String) textList.get(0));
			} else {
				int varIdx = 0;
				for (int i = 0; i < cnt; i++) {
					Object obj = textList.get(i);
					if (obj instanceof String) {
						sb.append((String) obj);
					} else if ((Integer) obj == CD_SUBSTITUTION) {
						String key = vars[varIdx++];
						Object val = record != null ? record.get(key) : null;
						if (val != null) {
							sb.append(val);
						} else {
							if (ver == 1) {
								sb.append("$" + key + "$");
							} else if (ver == 2) {
								sb.append("${" + key + "}");
							} else {
								sb.append("${" + key + "}");
							}
						}
					}
				}
			}
			return sb.toString();
		}

		public String toString() {
			return getText(null);
		}
	}
}
