package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogInfoHandler implements HttpHandler, Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(LogInfoHandler.class);

    private HttpExchange exchange;

    public void handle(HttpExchange exchange) throws IOException {
        LogInfoHandler logsHandler = new LogInfoHandler();
        logsHandler.setExchange(exchange);
        (new Thread(logsHandler)).start();
    }

    public void setExchange(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void run() {

        try {

            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=UTF-8"));
            Map<String, String> params = parseQuery(exchange);

            String type = params.get("type");
            if(type == null) type = "predprod";

            Integer month = params.get("month") != null ? Integer.parseInt(params.get("month")) : null;
            Integer year = params.get("year") != null ? Integer.parseInt(params.get("year")) : null;
            if(year == null) year = Calendar.getInstance().get(Calendar.YEAR);

            OutputStream os = exchange.getResponseBody();
            byte[] content = getInfo(type, year, month).toString().getBytes("UTF-8");

            exchange.sendResponseHeaders(200, content.length);
            os.write(content);
            os.flush();
            os.close();

        } catch (Exception e) {
            HashSeekConstants.outPrintLine(e.getMessage());
            LOGGER.error("LOGInfo", e);
            sendError(exchange, e);
        }
    }

    private StringBuilder getInfo(String type, Integer year, Integer month) throws IOException {
        HashSeekConstants.outPrintLine("LogInfoHandler start.");
        return print(type, year, month);
    }

    public StringBuilder print(String type, Integer year, Integer month) {

        Calendar start = Calendar.getInstance();

        StringBuilder sb = new StringBuilder();

        printBodyBefore(sb, styles());

        String[] types = new String[] { "test", "predprod", "prod" };

        sb.append("<ul id=\"links\">");
        for(String t : types) {
            sb.append("<li><a id=\"link_").append(t).append("\" ").append(t.equals(type) ? "class=\"active\" " : " ").append("href=\"logFilesInfo?type=").append(t).append("\">").append(t.toUpperCase()).append("</a></li>");
        }
        sb.append("</ul>");

        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int lastYear = currentYear - 1;
        if(month == null) {
            if(year == currentYear) {
                month = Calendar.getInstance().get(Calendar.MONTH) + 1;
            } else {
                month = 12;
            }
        }

        if(year == currentYear && month > currentMonth) {
            month = currentMonth;
        }

        sb.append("<ul id=\"years\">\n");
        sb.append("   <li><a href=\"logFilesInfo?type="+ type +"&year="+ lastYear +"\" "+ (year == lastYear ? "class=\"active\" " : "") +"id=\"link_year_"+ lastYear +"\">"+ lastYear +"</a></li>\n");
        sb.append("   <li><a href=\"logFilesInfo?type="+ type +"&year="+ currentYear +"\" "+ (year == currentYear ? "class=\"active\" " : "") +"id=\"link_year_"+ currentYear +"\">"+ currentYear +"</a></li>\n");
        sb.append("</ul>\n");

        sb.append("<ul id=\"months\">\n");
        printMonthLink(sb,  1, "Leden", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  2, "Únor", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  3, "Březen", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  4, "Duben", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  5, "Květen", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  6, "Červen", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  7, "Červenec", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  8, "Srpen", type, year, month, currentYear, currentMonth);
        printMonthLink(sb,  9, "Září", type, year, month, currentYear, currentMonth);
        printMonthLink(sb, 10, "Říjen", type, year, month, currentYear, currentMonth);
        printMonthLink(sb, 11, "Listopad", type, year, month, currentYear, currentMonth);
        printMonthLink(sb, 12, "Prosinec", type, year, month, currentYear, currentMonth);
        sb.append("</ul>\n");

        sb.append("<div id=\"tabs\">");

        String log_dir = AppProperties.getLogLocation(type);
        String hash_dir = AppProperties.getHashLocation(type);

        HashSeekConstants.outPrintLine("searching in [" + log_dir + "] and [" + hash_dir + "]");

        sb.append("<div class=\"tab").append(" active").append("\" id=\"tab_").append(type).append("\">");
        printDaysTable(sb, year, month, type, log_dir, hash_dir);

        if(currentYear == year && month == currentMonth) {
            printHoursTable(sb, type, log_dir, hash_dir);
        }

        sb.append("</div>");

        sb.append("</div>");
        sb.append("<div class=\"footInfo\">");

        printDiskUsage(sb, log_dir);

        sb.append("<div class=\"footInfo\">Generováno: ").append((Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis()) / 1000).append("s</div><br />");
        sb.append("<input type=\"button\" onclick=\"window.location = 'hashSeek'\" value=\"Zpět\" />");

        printBodyAfter(sb);

        return sb;
    }

    public static void printMonthLink(StringBuilder sb, int m, String mName, String type, Integer year, Integer month, int currentYear, int currentMonth) {
        if(currentYear == year) {
            if(m <= currentMonth) {
                sb.append("   <li title=\""+ mName +"\"><a href=\"logFilesInfo?type=" + type + "&year=" + year + "&month=" + m + "\" " + (month == m ? "class=\"active\" " : "") + "id=\"link_month_" + m + "\">" + (m < 10 ? "0" : "") + m + "</a></li>\n");
            } else {
                sb.append("   <li title=\""+ mName +"\" class=\"future_month\"><span>"+ (m < 10 ? "0" : "") + m +"</span></li>\n");
            }
        } else {
            sb.append("   <li title=\"" + mName + "\"><a href=\"logFilesInfo?type=" + type + "&year=" + year + "&month=" + m + "\" " + (month == m ? "class=\"active\" " : "") + "id=\"link_month_" + m + "\">" + (m < 10 ? "0" : "") + m + "</a></li>\n");
        }
    }

    public static void printDiskUsage(StringBuilder sb, String dir) {
        sb.append("<h2>Diskový prostor</h2>");
        Runtime rt = Runtime.getRuntime();
        try {
            /*
                Filesystem            1024-blocks  Used       Available    Capacity  Mounted on
                /dev/mapper/appl_vg-appl_lv 4227557288 2584836436 1427973308      65% /CIPAPPL
            */
            Process proc = rt.exec("df -kP "+ dir);
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            proc.waitFor();

            br.readLine(); // read header row
            String[] data = br.readLine().split("\\s+");

//            String command = "/dev/mapper/appl_vg-appl_lv 4227557288 2584836436 1427973308      65% /CIPAPPL";
//            String[] data = command.split("\\s+");

            sb.append("<table class=\"diskInfo\">");
            sb.append("<tr><td>Size:</td><td class=\"val\">").append(Long.valueOf(data[1])/1024/1024).append("&nbsp;GB").append("</td></tr>");
            sb.append("<tr><td>Used:</td><td class=\"val\">").append(Long.valueOf(data[2])/1024/1024).append("&nbsp;GB").append("</td></tr>");
            sb.append("<tr><td>Free:</td><td class=\"val\">").append(Long.valueOf(data[3])/1024/1024).append("&nbsp;GB").append("&nbsp;(").append(100 - Integer.parseInt(data[4].replaceAll("[%\\s+]", ""))).append("%)</td></tr>");
            sb.append("</table>");

        } catch (Exception ignore) {
            LOGGER.error("LogInfo",ignore);
        }
    }

    public List<String> getClasses(String domain, String dayString, int num, String log_dir, String hash_dir) {
        List<String> classes = new ArrayList<String>();

        if(new File(log_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString).exists()) {
            classes.add("log");
        }

        if(new File(log_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".bgz").exists()) {
            classes.add("bgzLog");
        }

        if(new File(log_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".gz").exists()) {
            classes.add("gzLog");
        }

        if(!classes.contains("log") && !classes.contains("gzLog")) classes.add("noLog");
        if(!classes.contains("bgzLog")) classes.add("noBgzLog");

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".hash_v1").exists()) {
            classes.add("hash");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".bgz.hash_v1").exists()) {
            classes.add("bgzHash");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".bgz.hash_v1.bgz").exists()) {
            classes.add("bgzHashBgz");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".hash_v1.gz").exists()) {
            classes.add("gzHash");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + dayString + ".bgz.hash_v1.gz").exists()) {
            classes.add("gzBgzHash");
        }

        if(!classes.contains("hash") && !classes.contains("gzHash")) classes.add("noHash");
        if(!classes.contains("bgzHash") && !classes.contains("gzBgzHash") && !classes.contains("bgzHashBgz")) classes.add("noBgzHash");

        return classes;
    }

    public List<String> getClassesHour(String domain, String day, String hourString, int actualHour, int hour, int num, String log_dir, String hash_dir) {
        List<String> classes = new ArrayList<String>();

        if(new File(log_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + day + "." + hourString).exists()) {
            classes.add("log");
        } else {
            classes.add("noLog");
        }

        if(new File(log_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + day + "." + hourString + ".bgz").exists()) {
            classes.add("bgzLog");
        } else {
            classes.add("noBgzLog");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + day + "." + hourString + ".hash_v1").exists()) {
            classes.add("hash");
        } else {
            classes.add("noHash");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + day + "." + hourString + ".bgz.hash_v1").exists()) {
            classes.add("bgzHash");
        } else {
            classes.add("noBgzHash");
        }

        if(new File(hash_dir + "/" + domain + "_s"+ num +"_alsb_aspect.audit." + day + "." + hourString + ".bgz.hash_v1.bgz").exists()) {
            classes.add("bgzHashBgz");
        } else {
            classes.add("noBgzHashBgz");
        }

        if(hour > actualHour - 1) {
            classes.add("future");
        }

        return classes;
    }

    public List<String> getTooltip(List<String> classes, String domain) {

        List<String> tooltips = new ArrayList<String>();

        tooltips.add(domain);

        if(classes.contains("log")) tooltips.add("log");
        if(classes.contains("bgzLog")) tooltips.add("BGZ log");
        if(classes.contains("gzLog")) tooltips.add("GZ log");

        if(classes.contains("hash")) tooltips.add("hash");
        if(classes.contains("gzHash")) tooltips.add("GZ hash");
        if(classes.contains("bgzHash")) tooltips.add("BGZ hash");
        if(classes.contains("bgzHashBgz")) tooltips.add("BGZ hash BGZ");
        if(classes.contains("gzBgzHash")) tooltips.add("GZ BGZ hash");

//        if(classes.contains("noLog")) tooltips.add("No log");
//        if(classes.contains("noBgzLog")) tooltips.add("No BGZ log");
//        if(classes.contains("noHash")) tooltips.add("No hash");
//        if(classes.contains("noBgzHash")) tooltips.add("No BGZ hash");
//        if(classes.contains("noBgzHashBgz")) tooltips.add("No BGZ hash BGZ");

        return tooltips;
    }

    public void printDaysTable(StringBuilder sb, Integer year, Integer month, String type, String log_dir, String hash_dir) {

        sb.append("<table>\n"+
                  "  <thead>\n" +
                  "      <tr>\n" +
                  "          <th>Po</th>\n" +
                  "          <th>Út</th>\n" +
                  "          <th>St</th>\n" +
                  "          <th>Čt</th>\n" +
                  "          <th>Pá</th>\n" +
                  "          <th>So</th>\n" +
                  "          <th>Ne</th>\n" +
                  "      </tr>\n" +
                  "  </thead>\n" +
                  "<tbody>\n");

        Calendar today = Calendar.getInstance();

        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);

        if(year != null && month != null) {
            c.set(Calendar.DATE, 1);
            c.set(Calendar.MONTH, month - 1);
            c.set(Calendar.YEAR, year);
        } else {
            c.add(Calendar.DATE, -30);
        }

        sb.append("<tr>\n");

        // 1=Ne,2=Po,3=Ut,4=St,5=Ct,6=Pa,7=So
        for(int d = 1; d <= ((7-c.getFirstDayOfWeek()) + c.get(Calendar.DAY_OF_WEEK))%7; d++) {
            sb.append("<td />\n");
        }

        boolean firstDay = true;

        while((year != null && month != null && (c.get(Calendar.MONTH) + 1) == month && c.before(today)) || ((year == null || month == null) && c.before(today))) {

            if(!firstDay && c.get(Calendar.DAY_OF_WEEK) == c.getFirstDayOfWeek()) {
                sb.append("</tr>\n");
                sb.append("<tr>\n");
            }

            firstDay = false;

            printDay(sb, c, type, log_dir, hash_dir);

            c.add(Calendar.DATE, 1);
        }

        while(c.getFirstDayOfWeek() != c.get(Calendar.DAY_OF_WEEK)) {
            sb.append("<td />\n");
            c.add(Calendar.DATE, 1);
        }

        sb.append("</tr>\n");
        sb.append("</tbody>\n");
        sb.append("</table>\n");
    }

    public void printDay(StringBuilder sb, Calendar c, String type, String log_dir, String hash_dir) {
        sb.append("<td>");
        sb.append("<span>").append(c.get(Calendar.DATE)).append("</span>");

        for(String domain : new String[] { "jms", "other" }) {
            sb.append("<div>");

            int maxServers = type.equals("prod") ? 4 : 2;

            for(int num = 1; num <= maxServers; num++) {

                String dayString = new SimpleDateFormat("yyyyMMdd").format(c.getTime());
                String link = "" + num;

                List<String> classes = getClasses(domain, dayString, num, log_dir, hash_dir);

                if(classes.contains("gzHash") || classes.contains("gzLog")) {
                    link = "<a href=\"gunzipForm?type="+ type +"&amp;day="+ dayString +"\">"+ num +"</a>";
                }

                sb.append("<span class=\"").append(join(classes, " ")).append("\" title=\"[").append(join(getTooltip(classes, domain), "][")).append("]\">").append(link).append("</span>");
            }

            sb.append("</div>");
        }

        sb.append("</td>");
    }

    public void printHoursTable(StringBuilder sb, String type, String log_dir, String hash_dir) {

        String todayString = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());

        sb.append("<div class=\"hourDiv\">\n");
        sb.append("<h2>Hodinové logy:</h2>\n");
        sb.append("<table id=\"hour_table\">\n" +
                  "<tbody>\n");
        sb.append("<tr>\n");

        for(int h = 0; h <= 23; h++) {

            printHour(sb, h, todayString, type, log_dir, hash_dir);
        }

        sb.append("</tr>\n");
        sb.append("</tbody>\n");
        sb.append("</table>\n");
        sb.append("</div>\n");
    }

    public void printHour(StringBuilder sb, int hour, String dayString, String type, String log_dir, String hash_dir) {

        String hourString = (hour < 10 ? "0" : "") + hour;

        sb.append("<td>");
        sb.append("<span>").append(hourString).append("</span>");

        for(String domain : new String[] { "jms", "other" }) {
            sb.append("<div>");

            int maxServers = type.equals("prod") ? 4 : 2;
            int actualHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            for(int num = 1; num <= maxServers; num++) {

                List<String> classes = getClassesHour(domain, dayString, hourString, actualHour, hour, num, log_dir, hash_dir);

                sb.append("<span class=\"").append(join(classes, " ")).append("\" title=\"[").append(join(getTooltip(classes, domain), "][")).append("]\">").append(num).append("</span>");
            }

            sb.append("</div>");
        }

        sb.append("</td>");
    }

    private String styles() {
        return  "        * { font: 12px 'Arial'; padding: 0; margin: 0; }\n" +
                "        body { padding: 30px; }\n" +
                "        h2 { font-size: 14px; font-weight: bold; margin-bottom: 5px; color: #333; }\n" +
                "\n" +
                "        table { border-collapse: collapse; }\n" +
                "        table thead th { padding-bottom: 3px; font-weight: bold; }\n" +
                "        table td { padding: 0; margin: 0; padding-bottom: 6px; border: 1px solid #aaa; min-width: 30px; }\n" +
                "        table td > span { display: block; color: #666; padding: 0 3px 1px 3px; }\n" +
                "        table td div { margin-top: 3px; margin-left: 6px; margin-right: 3px; }\n" +
                "        table td div span { padding: 0 3px; margin-right: 2px; border: 1px solid white; margin-bottom: 2px; color: #444; }\n" +
                "        table td div span a { color: #666; text-decoration: none; }\n" +
                "\n" +
                "        table td div span.noLog.noBgzLog { background: rgb(255, 149, 161); }\n" +
                "        table td div span.bgzLog.noBgzHash { background: #fdd; }\n" +
                "        table td div span.gzBgzHash, table td div span.gzHash { border: 1px solid #ffca33; background: #ffd284; }\n" +
                "        table td div span.bgzHash, table td div span.hash { border: 1px solid #cc9e38; background: #fff29f; }\n" +
                "        table td div span.bgzLog.bgzHash, table td div span.bgzLog.bgzHashBgz { border: 1px solid #cc9e38; background: rgba(0, 255, 0, 0.39); }\n" +
                "        table td div span.log.hash { border: 1px solid rgba(0, 255, 0, 0.39); background: rgba(0, 255, 0, 0.39); }\n" +
                "\n" +
                "\n" +
                "        table#hour_table td span.noLog.noHash.future { background: white; color: #ccc; }\n" +
                "        table td div span.gzLog.log { background: lightblue; }\n" +
                "        table td div span.gzHash.hash, table td div span.gzBgzHash.bgzHash { background: lightblue; }\n" +
                "\n" +
                "        div.hourDiv { margin-top: 15px; }\n" +
                "\n" +
                "        div.tab { float: left; margin-right: 10px; display: none; }\n" +
                "        div.tab.active { display: block; }\n" +
                "\n" +
                "        ul#links { margin: 0; padding: 0; margin-bottom: 20px; border-bottom: 2px solid black; }\n" +
                "        ul#links li { display: inline-block; margin-right: 10px; color: black; }\n" +
                "        ul#links li a { color: #333; text-decoration: none; font-size: 16px; font-weight: bold; padding: 8px 10px 4px 10px; display: block; }\n" +
                "        ul#links li a:hover { color: #888; }\n" +
                "        ul#links li a.active { color: #f66; font-weight: bold; border-bottom: 3px solid #f66; padding: 8px 10px 2px 10px; background: white; position: relative; top: 3px; }\n" +
                "\n" +
                "        div.footInfo { clear: both; padding-top: 20px; }\n" +
                "        table.diskInfo { margin-top: 10px; }\n" +
                "        table.diskInfo td { border: none; padding-right: 10px; }\n" +
                "\n" +
                "        #years { margin: 10px; }\n" +
                "        #months { margin: 10px; margin-bottom: 20px; }\n" +
                "        #years li, #months li { display: inline; }\n" +
                "        #years li a, #months li a { color: black; text-decoration: none; font-weight: bold; font-size: 14px; margin-right: 6px; }\n" +
                "        #years li a.active, #months li a.active { color: #f66; border-bottom: 3px solid #f66; }\n" +
                "" +
                "        #months li.future_month { margin-left: 6px; }\n" +
                "        #months li.future_month span { color: #ccc; font-size: 14px; font-weight: bold; }\n" +
                "" +
                "        #tab_predprod td, #tab_test td { min-width: 45px; }\n" +
                "        #tab_prod td { min-width: 80px; }\n" +
                "";
    }

    public static void printBodyBefore(StringBuilder sb, String styles) {
        sb.append("<!DOCTYPE html>\n")
          .append("<html>\n")
          .append("<head>\n")
          .append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n")
          .append("    <title></title>\n")
          .append("    <style>\n")
          .append(styles)
          .append("\n    </style>\n")
          .append("</head>\n")
          .append("<body>\n")
          .append("    <div>\n");
    }

    public static void printBodyAfter(StringBuilder sb) {
        sb.append("</body>\n"+
                  "</html>\n");
    }

    public static StringBuilder join(Collection<?> c,String separator) {

        StringBuilder sb = new StringBuilder();

        if(c!=null) {
            int i = 0;
            for(Object o : c) {
                sb.append(o);
                if(++i < c.size()) sb.append(separator);
            }
        }
        return sb;
    }

    public static StringBuilder getWebException(Exception e) {
        return getExceptionStackWeb(e, null, 3);
    }

    public static StringBuilder getExceptionStackWeb(Throwable t, String filter) {
        StringBuilder str = new StringBuilder();

        str.append("<strong>Caused by:&nbsp;&nbsp;").append(t.getCause()).append("</strong><br />\n");
        str.append("<strong>Message:&nbsp;&nbsp;").append(t.getMessage()).append("</strong><br />\n");

        for (StackTraceElement se : t.getStackTrace()) {
            str.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<span style=\"left-margin: 30px;")
                    .append(filter != null ? (se.toString().contains(filter) ? " color: red;" : "") : "")
                    .append("\">")
                    .append(se)
                    .append("</span>")
                    .append("<br />\n");
        }

        return str;
    }

    public static StringBuilder getExceptionStackWeb(Throwable t, String filter, int c) {
        StringBuilder str = new StringBuilder();

        for(int cc=0; cc<c; cc++) {
            if(t!=null) {
                str.append(getExceptionStackWeb(t, filter));
                t = t.getCause();
            }
        }

        return str;
    }

    public static void sendError(HttpExchange exchange, Exception ex) {
        sendError(exchange, getWebException(ex));
    }

    public static void sendError(HttpExchange exchange, StringBuilder text) {
        try {
            String message = "<h1>Oooooops...</h1><br><pre>";
            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=UTF-8"));
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write((message + text).getBytes());
            os.flush();
            os.close();
        } catch (IOException e) {
            LOGGER.error("sendError",e);
        }

    }

    public static Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> params = new HashMap<String, String>();

        String query = exchange.getRequestURI().getQuery();
        if(query != null) {
            for(String param : query.split("&")) {
                String[] parts = param.split("=");
                params.put(parts[0], parts[1]);
            }
        }
        return params;
    }

    public static Map<String, Object> parsePostParams(HttpExchange exchange) throws IOException {

        BufferedInputStream httpBin = new BufferedInputStream(exchange.getRequestBody());
        StringBuilder buf = new StringBuilder();
        while (httpBin.available() > 0 && buf.length() < 4096) {
            buf.append((char)httpBin.read());
        }

        Map<String, Object> params = new HashMap<String, Object>();

        for(String param : buf.toString().split("&")) {
            String[] parts = param.split("=");
            if(params.containsKey(parts[0])) {
                Object o = params.get(parts[0]);
                if(!(params.get(parts[0]) instanceof List)) {
                    List<String> list = new ArrayList<String>();
                    list.add((String)o);
                    list.add(parts[1]);
                    params.put(parts[0], list);
                } else {
                    //noinspection unchecked
                    ((List<String>)o).add(parts[1]);
                }
            } else {
                params.put(parts[0], parts[1]);
            }
        }

        return params;
    }
}
