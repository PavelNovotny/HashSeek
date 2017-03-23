package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.util.CloseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 29.9.12 12:20
 */
class HashSeekParams implements HttpHandler, Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(HashSeekParams.class);
    private HttpExchange exchange;
    private String encoding="UTF-8";

    public void handle(HttpExchange exchange) throws IOException {
        HashSeekParams findFlowHandler = new HashSeekParams();
        findFlowHandler.setExchange(exchange);
        (new Thread(findFlowHandler)).start();
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public void setExchange(HttpExchange exchange) {
        this.exchange = exchange;
    }

    private String replaceFirstRegex(String pattern, String replacement, String replacedString) {
        if (replacement.contains("null")) {
            return replacedString;
        } else {
            return Pattern.compile(pattern, Pattern.MULTILINE).matcher(replacedString).replaceFirst(replacement);
        }
    }

    public void run() {
        OutputStream os = exchange.getResponseBody();
        try {
            BufferedInputStream httpBin = new BufferedInputStream(exchange.getRequestBody());
            StringBuffer buf = new StringBuffer();
            while (httpBin.available() > 0 && buf.length() < 4096) { //zpracujeme max 4KB
                buf.append((char)httpBin.read());
            }
            Session session = Session.parseParameters(buf);
            if (!session.isAuthenticated()) {
                AuthenticationHandler.authenticateRedirect(exchange, session);
                return;
            }

            BufferedReader htmlReader = new BufferedReader(new InputStreamReader(new FileInputStream("./FindFlow.html"), encoding));
            StringBuffer response = new StringBuffer();
            String htmlLine;
            while ((htmlLine = htmlReader.readLine()) != null) {
                response.append(htmlLine).append("\n");
            }
            String responseString = response.toString();
            responseString = replaceFirstRegex("\"predprod\"",String.format("\"predprod\" %s", session.isPredprod()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"produkce\"",String.format("\"produkce\" %s", session.isProd()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"test\"",String.format("\"test\" %s", session.isTest()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"runinfo\"",String.format("\"runinfo\" %s", session.isRuninfo()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"onlineconnectesb\"",String.format("\"onlineconnectesb\" %s", session.isOnlineEsbSeek()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"b2bseek\"",String.format("\"b2bseek\" %s", session.isB2bSeek()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"onlineconnectbpm\"",String.format("\"onlineconnectbpm\" %s", session.isOnlineBPMSeek()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"seekhashesonly\"",String.format("\"seekhashesonly\" %s", session.isHashFileSeekOnly()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"includetimelogs\"",String.format("\"includetimelogs\" %s", session.isIncludeTimeLogs()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"session\" value=\"[^\"]*\"",String.format("\"session\" value=\"%s\"", session.getSession()), responseString);
            responseString = replaceFirstRegex("processCommand", String.format("processCommand?%s", session.getSession()), responseString);
            responseString = replaceFirstRegex("name=\"defect\" value=\"[^\"]*\"", String.format("name=\"defect\" value=\"%s\"", session.getDefect()), responseString);
            responseString = replaceFirstRegex("name=\"seekTime\" value=\"[^\"]*\"", String.format("name=\"seekTime\" value=\"%s\"", session.getSeekTime()), responseString);
            responseString = replaceFirstRegex(String.format("option value=\"%s\"", session.getHoursToSeek()), String.format("option value=\"%s\" selected", session.getHoursToSeek()), responseString);
            if (session.getSeekDay()==null || session.getSeekDay().isEmpty()) {
							session.setSeekDay(new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
            }
            responseString = replaceFirstRegex("name=\"seekDay\" value=\"[^\"]*\"", String.format("name=\"seekDay\" value=\"%s\"", session.getSeekDay()), responseString);
            responseString = replaceFirstRegex("name=\"seekString0\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString0\" value=\"%s\"", session.getSeekString(0))), responseString);
            responseString = replaceFirstRegex("name=\"seekString1\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString1\" value=\"%s\"", session.getSeekString(1))), responseString);
            responseString = replaceFirstRegex("name=\"seekString2\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString2\" value=\"%s\"", session.getSeekString(2))), responseString);
            responseString = replaceFirstRegex("name=\"seekString3\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString3\" value=\"%s\"", session.getSeekString(3))), responseString);
            responseString = replaceFirstRegex("name=\"seekString4\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString4\" value=\"%s\"", session.getSeekString(4))), responseString);
            responseString = replaceFirstRegex("name=\"seekString5\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString5\" value=\"%s\"", session.getSeekString(5))), responseString);
            responseString = replaceFirstRegex("name=\"seekString6\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString6\" value=\"%s\"", session.getSeekString(6))), responseString);
            responseString = replaceFirstRegex("name=\"seekString7\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString7\" value=\"%s\"", session.getSeekString(7))), responseString);
            responseString = replaceFirstRegex("name=\"seekString8\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString8\" value=\"%s\"", session.getSeekString(8))), responseString);
            responseString = replaceFirstRegex("name=\"seekString9\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString9\" value=\"%s\"", session.getSeekString(9))), responseString);
            responseString = replaceFirstRegex("name=\"seekString10\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString10\" value=\"%s\"", session.getSeekString(10))), responseString);
            responseString = replaceFirstRegex("name=\"seekString11\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString11\" value=\"%s\"", session.getSeekString(11))), responseString);
            responseString = replaceFirstRegex("name=\"seekString12\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString12\" value=\"%s\"", session.getSeekString(12))), responseString);
            responseString = replaceFirstRegex("name=\"seekString13\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString13\" value=\"%s\"", session.getSeekString(13))), responseString);
            responseString = replaceFirstRegex("name=\"seekString14\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"seekString14\" value=\"%s\"", session.getSeekString(14))), responseString);
            responseString = replaceFirstRegex("name=\"filterString0\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"filterString0\" value=\"%s\"", session.getFilterString(0))), responseString);
            responseString = replaceFirstRegex("name=\"filterString1\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"filterString1\" value=\"%s\"", session.getFilterString(1))), responseString);
            responseString = replaceFirstRegex("name=\"filterString2\" value=\"[^\"]*\"", Matcher.quoteReplacement(String.format("name=\"filterString2\" value=\"%s\"", session.getFilterString(2))), responseString);

            responseString = replaceFirstRegex("name=\"csvService0\" value=\"[^\"]*\"", String.format("name=\"csvService0\" value=\"%s\"", session.getCsvParams().get(0).getService()), responseString);
            responseString = replaceFirstRegex("name=\"csvService1\" value=\"[^\"]*\"", String.format("name=\"csvService1\" value=\"%s\"", session.getCsvParams().get(1).getService()), responseString);
            responseString = replaceFirstRegex("name=\"csvService2\" value=\"[^\"]*\"", String.format("name=\"csvService2\" value=\"%s\"", session.getCsvParams().get(2).getService()), responseString);

            responseString = replaceFirstRegex("\"csvRequest0\"",String.format("\"csvRequest0\" %s", session.getCsvParams().get(0).isRequest()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"csvRequest1\"",String.format("\"csvRequest1\" %s", session.getCsvParams().get(1).isRequest()? "checked":""), responseString);
            responseString = replaceFirstRegex("\"csvRequest2\"",String.format("\"csvRequest2\" %s", session.getCsvParams().get(2).isRequest()? "checked":""), responseString);

            responseString = replaceFirstRegex("name=\"csvXpath00\" value=\"[^\"]*\"", String.format("name=\"csvXpath00\" value=\"%s\"", session.getCsvParams().get(0).getxPath().get(0)), responseString);
            responseString = replaceFirstRegex("name=\"csvXpath01\" value=\"[^\"]*\"", String.format("name=\"csvXpath01\" value=\"%s\"", session.getCsvParams().get(0).getxPath().get(1)), responseString);
            responseString = replaceFirstRegex("name=\"csvXpath10\" value=\"[^\"]*\"", String.format("name=\"csvXpath10\" value=\"%s\"", session.getCsvParams().get(1).getxPath().get(0)), responseString);
            responseString = replaceFirstRegex("name=\"csvXpath11\" value=\"[^\"]*\"", String.format("name=\"csvXpath11\" value=\"%s\"", session.getCsvParams().get(1).getxPath().get(1)), responseString);
            responseString = replaceFirstRegex("name=\"csvXpath20\" value=\"[^\"]*\"", String.format("name=\"csvXpath20\" value=\"%s\"", session.getCsvParams().get(2).getxPath().get(0)), responseString);
            responseString = replaceFirstRegex("name=\"csvXpath21\" value=\"[^\"]*\"", String.format("name=\"csvXpath21\" value=\"%s\"", session.getCsvParams().get(2).getxPath().get(1)), responseString);

            responseString = replaceFirstRegex("name=\"nsPrefix0\" value=\"[^\"]*\"", String.format("name=\"nsPrefix0\" value=\"%s\"", session.getPrefixList().get(0)), responseString);
            responseString = replaceFirstRegex("name=\"nsPrefix1\" value=\"[^\"]*\"", String.format("name=\"nsPrefix1\" value=\"%s\"", session.getPrefixList().get(1)), responseString);
            responseString = replaceFirstRegex("name=\"nsPrefix2\" value=\"[^\"]*\"", String.format("name=\"nsPrefix2\" value=\"%s\"", session.getPrefixList().get(2)), responseString);

            responseString = replaceFirstRegex("name=\"namespace0\" value=\"[^\"]*\"", String.format("name=\"namespace0\" value=\"%s\"", session.getNamespaceList().get(0)), responseString);
            responseString = replaceFirstRegex("name=\"namespace1\" value=\"[^\"]*\"", String.format("name=\"namespace1\" value=\"%s\"", session.getNamespaceList().get(1)), responseString);
            responseString = replaceFirstRegex("name=\"namespace2\" value=\"[^\"]*\"", String.format("name=\"namespace2\" value=\"%s\"", session.getNamespaceList().get(2)), responseString);

            responseString = replaceFirstRegex("\"csvseek\"",String.format("\"csvseek\" %s", session.isCsvSeek()? "checked":""), responseString);

            responseString = replaceFirstRegex("a href=\"download\"", String.format("a href=\"download?%s\"", session.getSession()), responseString);
            responseString = replaceFirstRegex("a href=\"result\"", String.format("a href=\"result?%s\"", session.getSession()), responseString);
            if (!session.isEligibleShowSession()) {
                responseString = replaceFirstRegex("<a href=\"sessions\">Sessions</a>", "", responseString);
            } else {
                responseString = replaceFirstRegex("<a href=\"sessions\">", String.format(" | <a href=\"sessions?%s\">", session.getSession()), responseString);
            }
            if (!session.isEligibleNoeSeek()) {
                responseString = replaceFirstRegex("NOE logy<input type=\"checkbox\" name=\"noeseek\">", "", responseString);
            } else {
                responseString = replaceFirstRegex("\"noeseek\"",String.format("\"noeseek\" %s", session.isNoeSeek()? "checked":""), responseString);
            }
            if (!session.isEligibleSeekLimit()) {
                responseString = replaceFirstRegex("<td align=\"left\">Seek limit:</td>", "", responseString);
                responseString = replaceFirstRegex("<td align=\"left\"><input type=\"text\" name=\"seekLimit\" value=\"\" class=\"word\"/></td>", "", responseString);
            } else {
                responseString = replaceFirstRegex("name=\"seekLimit\" value=\"[^\"]*\"", String.format("name=\"seekLimit\" value=\"%s\"", session.getSeekLimit()), responseString);
            }
            byte[] bytesToSend = responseString.getBytes(encoding);
            exchange.sendResponseHeaders(200, bytesToSend.length);
            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=%s", encoding));
            os.write(bytesToSend);
            os.close();
        } catch (Exception e) {
            LOGGER.error("Params",e);
        } finally {
            CloseUtil.close(os);

        }
    }

}
