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
 *         27.11.13 - 15:36
 * @version $Id$
 */
public class HTMLOutput5 {

	public static StringBuffer process(String filename, String title) throws Exception {

		BufferedReader in = new BufferedReader(new FileReader(filename));

		boolean xml = false;
		boolean timelog = false;

		StringBuffer code = new StringBuffer();
		StringBuffer timelogCode = new StringBuffer();

		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html>\n");
		sb.append("<head>\n");
		sb.append(String.format("<title>%s - HashSeek</title>\n", title));
		sb.append("<meta charset=\"utf-8\">\n");
		sb.append("</head>\n");
		sb.append("<html>\n");
		sb.append(CSS + "\n");
		sb.append(JS + "\n");
		sb.append("<body onload=\"resetSelection()\">\n");

		sb.append("<a id=\"top\"></a>\n");
		sb.append("<div class=\"menu\">\n");
		sb.append("<a id=\"t1-tab\" href=\"#\" onclick=\"tabs.click(this)\">HashSeek</a>|");
		sb.append("<a id=\"t2-tab\" href=\"#\" onclick=\"tabs.click(this)\">clipboard</a>");
		sb.append("<span id=\"count\">(0)</span>");
		sb.append("</div>\n");


		//tab2
		sb.append("<div id=\"t2\">");
		sb.append("<form action=\"clipboard\" method=\"post\" onsubmit=\"fill()\" accept-charset=\"utf-8\">");
		sb.append("<input type=\"submit\" value=\"Download clipboard\" style=\"margin-bottom:8px\">");
		sb.append(String.format("<input type=\"hidden\" id=\"filename\" name=\"filename\" value=\"%s\">", new File(filename).getName()));
		sb.append("<textarea style=\"display:none\" id=\"data\" name=\"data\"></textarea>");
		sb.append("</form>");
		sb.append("<div id=\"clipboard\"></div>");
		sb.append("</div>\n");


		//tab1
		sb.append("<div id=\"t1\">");
        boolean isOpenDivForTag=false;
		while (in.ready()) {
            if(sb.length()> 30*1024*1024){
                sb.append("BIG RESPONSE!!!!");
                break;
            }
			String s = in.readLine();

			//start xml
			if (s.contains("<?xml version")) {
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
                    if(isOpenDivForTag){
                        sb.append("</div>");
                        isOpenDivForTag=false;
                    }else{
					    sb.append("</div>");
                    }
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
				sb.append(String.format("<a id=\"%sa\"></a>\n", tag));
				sb.append(String.format("<input type=\"checkbox\" name=\"cb\" id=\"%sd\" onclick=\"xselect('%sc', this.checked)\" class='cb'>", tag, tag));
				sb.append(s.replaceAll("<", "&lt;").replace(tag, String.format("<a href=\"#%s\">%s</a>", tag, tag))).append("<br/>\n");
				continue;
			}

			if (isTag(s)) {
                if(isOpenDivForTag){
                    sb.append("</div>");
                    isOpenDivForTag=false;
                }
				//tag
				String tag = getTag(s);
				sb.append(String.format("<a id=\"%s\"></a>\n", tag));
				sb.append(s.replace(tag, String.format("<a href=\"#%sa\">%s</a>", tag, tag))).append("|");
				sb.append("<a href=\"#top\">top</a>\n");
				sb.append(String.format("<div id=\"%sc\">\n", tag));
                isOpenDivForTag=true;
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
			sb.append(s.replaceAll("<", "&lt;").replaceAll(">","&gt")).append("<br/>\n");

		}

		in.close();


		sb.append("</div>\n");

		sb.append("<script language=\"JavaScript\">var tabs=new Tabpane('tabs', new Array('t1','t2'), 't1')</script>");

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
        String[] splitted = xml.split("<\\?xml version");
        xml = "<?xml version" + splitted[splitted.length-1];
        for (int i =0; i<splitted.length-1; i++) {
            sb.append(splitted[i]);
        }
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

		return highlight;
	}

	//tag line
	static private boolean isTagLine(String s) {
		return Pattern.compile("^[ojcbn]\\d+\\s+.*").matcher(s).matches();
	}

	//timelog
	static private boolean isTimeLine(String s) {
		return s.contains("_alsb_aspect.time");
	}

	//extract tag
	private static String getTag(String s) {
		Pattern PATTERN = Pattern.compile("^[ojcbn]\\d+");
		Matcher m = PATTERN.matcher(s);
		while (m.find()) {
			return m.group(0);
		}
		return null;
	}

	//tag
	static private boolean isTag(String s) {
		return Pattern.compile("^[ojcbn]\\d+$").matcher(s).matches();
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

	public static String CSS = "<style type=\"text/css\">\n" +
			"body {font-family: Consolas, monospace; font-size: 12px;}\n" +
			"a {color: #1155AA; font-weight: normal;}\n" +
			"pre {margin: 0; color: #555;}\n" +
			".error {color: red;}\n" +
			".highlight {background-color: yellow;}\n" +
			"#clipboard .highlight {background-color: white;}\n" +
			".cb {margin: 0 4px 0 0;}\n" +
			".menu {margin-bottom: 10px;}\n" +
			".menu a {font-weight: bold;text-shadow:1px 1px 1px #888;}\n" +
			"</style>\n";

	public static String JS = "<script type=\"text/javascript\">\n" +
			"function Tabpane(id, divs, active){\n" +
			"this.id=id\n" +
			"this.divs=divs\n" +
			"this.click=function(src){\n" +
			"for(i=0;i<this.divs.length;i++) {\n" +
			"var e=document.getElementById(this.divs[i])\n" +
			"e.style.display='none'\n" +
			"}\n" +
			"var d=document.getElementById(src.id.replace('-tab',''))\n" +
			"d.style.display=''\n" +
			"}\n" +
			"this.click(document.getElementById(active + '-tab')) //init\n" +
			"}\n" +
			"function xselect(id, flag) {\n" +
			"var el = document.getElementById(id)\n" +
			"if (flag) {\n" +
			"el.className='highlight'\n" +
			"} else {\n" +
			"el.className=''\n" +
			"}\n" +
			"updateClipboard()\n" +
			"}\n" +
			"function resetSelection(){\n" +
			"var checkboxes = document.getElementsByName('cb');\n" +
			"for (var i=0; i<checkboxes.length; i++) {\n" +
			"checkboxes[i].checked = false\n" +
			"}\n" +
			"}\n" +
			"function updateClipboard(){\n" +
			"var clipboard = document.getElementById('clipboard')\n" +
			"clipboard.innerHTML = \"\"\n" +
			"var checkboxes = document.getElementsByName('cb')\n" +
			"var count = 0\n" +
			"for (var i=0; i<checkboxes.length; i++) {\n" +
			"if (checkboxes[i].checked) {\n" +
			"var id = checkboxes[i].id\n" +
			"var res = id.substr(0, id.length-1) \n" +
			"var el = document.getElementById(res+'c')\n" +
			"clipboard.innerHTML = clipboard.innerHTML + el.innerHTML + '<br/>'\n" +
			"count++\n" +
			"}\n" +
			"}" +
			"document.getElementById('count').innerHTML = '(' + count + ')'" +
			"\n" +
			"}\n" +
			"function fill(){\n" +
			"var c=document.getElementById('clipboard')\n" +
			"var d=document.getElementById('data')\n" +
			"d.value=c.textContent\n" +
//			"alert('submit')\n" +
			"}\n" +
			"</script>";
}
