package com.jiandan.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
	/**
	 * 根据正则表达式匹配，根据index返回结果
	 * 
	 * @param regex
	 * @param content
	 * @param index
	 * @return
	 */
	public static List<String> getMatchedList(String regex, String content, int index) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		List<String> list = new ArrayList<String>();
		while (m.find()) {
			list.add(m.group(index));
		}
		return list;
	}

	/**
	 * 根据正则表达式匹配，根据index返回结果
	 * 
	 * @param regex
	 * @param content
	 * @param index
	 * @return
	 */
	public static String getMatchedString(String regex, String content, int index) {
		List<String> list = getMatchedList(regex, content, index);
		return list.size() > 0 ? list.get(0) : "";
	}
}