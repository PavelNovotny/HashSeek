package com.o2.cz.cip.hashseek.html;

import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2013 EXNET s.r.o.
 * Chodska 1032/27, 120 00 - Praha 2, Czech Republic
 * http://www.exnet.cz
 * All Rights Reserved.
 *
 * @author Vladimir Zboril (zboril@exnet.cz)
 *         25.11.13 - 9:31
 * @version $Id$
 */
public class HTMLOutput {

	public static StringBuffer process(String filename) throws Exception {

		BufferedReader in = new BufferedReader(new FileReader(filename));

		boolean xml = false;
		boolean timelog = false;

		StringBuffer code = new StringBuffer();
		StringBuffer timelogCode = new StringBuffer();

		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html>\n");
		sb.append(HEAD + "\n");
		sb.append("<html>\n");
		sb.append(CSS + "\n");
		sb.append(JS + "\n");
		sb.append("<body>\n");

		while (in.ready()) {

			String s = in.readLine();

			//start xml
			if (s.startsWith("<?xml")) {
				xml = true;
				code = new StringBuffer();
				code.append(s).append("\n");
				continue;
			}

			if (xml) {
				//end xml
				if (s.length() == 0) {
					xml = false;
					//process xml
					sb.append(processXML(code.toString()));
					//clean
					code = null;
				} else {
					code.append(s).append("\n");
				}
				continue;
			}

			if (isTagLine(s)) {
				//tagline
				String tag = getTag(s);
				sb.append(String.format("<a id=\"%s_\"></a>\n", tag));
				sb.append(s.replace(tag, String.format("<a href=\"#%s\"title=\"Goto xml\">%s</a>", tag, tag))).append("<br/>\n");
				continue;
			}

			if (isTag(s)) {
				//tag
				String tag = getTag(s);
				sb.append(String.format("<a id=\"%s\"></a>\n", tag));
				sb.append(s.replace(tag, String.format("<a href=\"#%s_\" title=\"Back\">%s</a>", tag, tag))).append("<br/>\n");
				continue;
			}

			if (isTimeLine(s)) {
				timelog = true;
				timelogCode = new StringBuffer(s).append("\n");
				continue;
			}

			if (timelog) {
				if (s.length() == 0) {
					timelog = false;
					sb.append(processTimelog(timelogCode.toString()));
					//clean
				} else {
					timelogCode.append(s).append("\n");
				}
				continue;
			}
			sb.append(s).append("<br/>\n");

		}

		in.close();

		sb.append("</body>\n");
		sb.append("</html>");

		return sb;
	}

	/**
	 * @param xml
	 * @return
	 */
	private static String processXML(String xml) {
		StringBuffer sb = new StringBuffer();
		String id = System.currentTimeMillis() + "";
		sb.append(String.format("<a href=\"\" onclick=\"xselect('%s');return false;\" class=\"btn\">select-xml</a>\n", id));
		sb.append(String.format("<pre id=\"%s\">", id));
		String formatted = formatXML(xml, 2);
		formatted = formatted.replaceAll("<", "&lt;");
		sb.append(formatted);
		sb.append("</pre>");
		return sb.toString();
	}

	/**
	 * @param timelog
	 * @return
	 */
	private static String processTimelog(String timelog) {

		StringBuffer out = new StringBuffer();
		out.append("<pre>\n");
		out.append(timelog.toString().replaceAll("<", "&lt;"));
		out.append("</pre>\n");

		//highlight errors
		String highlight = out.toString().replaceAll("(.*FALSE.*\n)", "<span class=\"error\">$1</span>");
		//System.out.println(highlight);

		return highlight;
	}

	//tag line
	static private boolean isTagLine(String s) {
		return Pattern.compile("^[oj]\\d+\\s+.*").matcher(s).matches();
	}

	//timelog
	static private boolean isTimeLine(String s) {
		return s.contains("_alsb_aspect.time");
	}

	//extract tag
	private static String getTag(String s) {
		Pattern PATTERN = Pattern.compile("^[oj]\\d+");
		Matcher m = PATTERN.matcher(s);
		while (m.find()) {
			return m.group(0);
		}
		return null;
	}

	//tag
	static private boolean isTag(String s) {
		return Pattern.compile("^[oj]\\d+$").matcher(s).matches();
	}

	/**
	 * @param unformattedXml
	 * @param indent
	 * @return
	 */
	private static String formatXML(String unformattedXml, int indent) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(null);
			InputSource is = new InputSource(new StringReader(unformattedXml));
			final org.w3c.dom.Document document = db.parse(is);
			com.sun.org.apache.xml.internal.serialize.OutputFormat format = new com.sun.org.apache.xml.internal.serialize.OutputFormat(document);
			format.setLineWidth(0); //nowrap
			format.setIndenting(true);
			format.setIndent(indent);
			Writer out = new StringWriter();
			com.sun.org.apache.xml.internal.serialize.XMLSerializer serializer = new com.sun.org.apache.xml.internal.serialize.XMLSerializer(out, format);
			serializer.serialize(document);
			return out.toString();
		} catch (Exception e) {
			//TODO log
			return unformattedXml;
		}
	}

	public static String HEAD =
			"<head>\n" +
					"<title>Hashseek - output</title>\n" +
					"<meta charset=\"utf-8\">\n" +
					"</head>\n";

	public static String CSS = "<style type=\"text/css\">" +
			"body {font-family: Consolas, monospace; font-size: 12px;}" +
			"a {color: #1155AA; font-weight: bold;}" +
			"pre {margin: 0; color: #555}" +
			".error {color: red}" +
			"a.btn {\n" +
			"background-color: #C0C0C0;\n" +
			"color: #FFF;\n" +
			"padding: 0 3px;\n" +
			"margin: 2px 0 2px 0;\n" +
			"display: inline-block;\n" +
			"text-decoration: none;\n" +
			"}\n" +
			"a.btn:hover {\n" +
			"background-color: red;\n" +
			"}" +
			"</style>";

	public static String JS = "<script type=\"text/javascript\">\n" +
			"function xselect(id) {\n" +
			"var el = document.getElementById(id)\n" +
			"var range = document.createRange();\n" +
			"range.selectNodeContents(el);\n" +
			"var sel = window.getSelection();\n" +
			"sel.removeAllRanges();\n" +
			"sel.addRange(range);\n" +
			"}\n" +
			"</script>";
}
