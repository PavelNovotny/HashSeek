package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class GunzipFormHandler implements HttpHandler, Runnable {

    private HttpExchange exchange;

    public void handle(HttpExchange exchange) throws IOException {
        GunzipFormHandler gunzipFormHandler = new GunzipFormHandler();
        gunzipFormHandler.setExchange(exchange);
        (new Thread(gunzipFormHandler)).start();
    }

    public void setExchange(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void run() {

        try {

            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=utf-8"));

            OutputStream os = exchange.getResponseBody();
            byte[] content = form().toString().getBytes("UTF-8");

            exchange.sendResponseHeaders(200, content.length);
            os.write(content);
            os.flush();
            os.close();

        } catch (Exception e) {
            HashSeekConstants.outPrintLine(e.getMessage());
            LogInfoHandler.sendError(exchange, e);
        }
    }

    private String styles() throws IOException {

        return "* { padding:0; margin: 0; font-family: Arial; font-size: 12px; }\n" +
                "body { padding: 10px; }\n" +
                "h2 { font-size: 14px; }\n" +
                "\n" +
                "table { margin-top: 20px; margin-bottom: 20px; border-collapse: collapse; }\n" +
                "table label { margin-left: 10px; }\n" +
                "table td { padding: 3px 5px; border: 1px solid #ccc; }\n" +
                "table td.mid { width: 50px; }";
    }

    private StringBuilder form() throws IOException {

        StringBuilder sb = new StringBuilder();

        Map<String, String> params = LogInfoHandler.parseQuery(exchange);

        String dayString = params.get("day");
        String type = params.get("type");
        String log_dir = AppProperties.getLogLocation(type);
        String hash_dir = AppProperties.getHashLocation(type);

        LogInfoHandler.printBodyBefore(sb, styles());
        LogInfoHandler.printDiskUsage(sb, log_dir);

        sb.append("<h2>Soubory k rozbalení</h2>");
        sb.append("<form action=\"gunzip\" method=\"post\">");

        sb.append("<input type=\"hidden\" name=\"type\" value=\"").append(type).append("\"/>");
        sb.append("<input type=\"hidden\" name=\"day\" value=\"").append(dayString).append("\"/>");

        sb.append("<table>");
        for(String domain : new String[] { "jms", "other" }) {

            int maxServers = type.equals("prod") ? 4 : 2;

            for(int num = 1; num <= maxServers; num++) {

                String file = domain + "_s"+ num +"_alsb_aspect.audit." + dayString;
                String logHtmlId =  (log_dir + file + ".gz").replaceAll("/", "").replaceAll(".", "_");
                String hashHtmlId =  (hash_dir + file + ".hash.gz").replaceAll("/", "").replaceAll(".", "_");

                sb.append("<tr>");
                sb.append("<td>");
                if(new File(log_dir + "/" + file + ".gz").exists()) {
                    sb.append("   <input type=\"checkbox\" id=\"").append(logHtmlId).append("\" name=\"files\" value=\"").append(file).append("\" />");
                } else {
                    if(new File(log_dir + "/" + file).exists()) {
                        sb.append("<span>log již rozbalen</span>");
                    } else {
                        sb.append("<span>neexistuje</span>");
                    }
                }
                sb.append("</td>");
                sb.append("<td>");
                sb.append("   <label for=\"").append(logHtmlId).append("\">").append(file).append(".gz").append("</label>");
                sb.append("</td>");
                sb.append("<td class=\"mid\"/>");

                sb.append("<td>");
                if(new File(hash_dir + "/" + file + ".hash.gz").exists()) {
                    sb.append("   <input type=\"checkbox\" id=\"").append(hashHtmlId).append("\" name=\"files\" value=\"").append(file).append(".hash\" />");
                } else {
                    if(new File(hash_dir + "/" + file + ".hash").exists()) {
                        sb.append("<span>hash již rozbalen</span>");
                    } else {
                        sb.append("<span>neexistuje</span>");
                    }
                }
                sb.append("</td>");
                sb.append("<td>");
                sb.append("   <label for=\"").append(hashHtmlId).append("\">").append(file).append(".hash.gz").append("</label>");
                sb.append("</td>");
                sb.append("</tr>");

            }

            if(domain.equals("jms")) {
                sb.append("<tr><td colspan=\"5\">&nbsp;</td></tr>");
            }
        }
        sb.append("</table>");
        sb.append("<input type=\"submit\" value=\"Rozbalit soubory\"/>&nbsp;");
        sb.append("<input type=\"button\" onclick=\"window.location = 'logFilesInfo?type=").append(type).append("'\" value=\"Zpět\" />");
        sb.append("</form>");

        LogInfoHandler.printBodyAfter(sb);

        return sb;
    }
}
