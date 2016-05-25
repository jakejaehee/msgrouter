package test;


import java.util.HashMap;
import java.util.Map;

import elastic.util.util.TechException;

public class ReplaceableTextTest {
	
	public static void main(String[] args) throws TechException {
		test1();
	}
	
	public static void test2() throws TechException {
		String str = "abcd&efgh";
		System.out.println(str);
		
		String rep = str.replace("&", "&amp;");
		System.out.println(rep);
	}

	public static void test1() throws TechException {
		String src = "${current.dir}  <html>\n<a     ${ABC}a>\n     ${AB_C}\n<LOOP${loopA}>${aaa}, ${bbb} 하하  </LOOP>\n </html>";
		ReplaceableText text = new ReplaceableText(src);
		System.out.println(text.getSourceText());
		System.out.println("-----------");

		System.out.println(text);
		System.out.println("-----------");

		Map map = new HashMap();
		map.put("ABC", "하하호호히히히");
		map.put("bbb", 1234);
		text.setValues(map);
		
		text.setValue("AB_C", "ho");
		
		System.out.println(text);
		System.out.println("-----------");
	}
}
