package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.util.CloseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 29.9.12 12:20
 */
class SessionHandler implements HttpHandler, Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(SessionHandler.class);

    private HttpExchange exchange;
    private String encoding="UTF-8";

    public void handle(HttpExchange exchange) throws IOException {
        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setExchange(exchange);
        (new Thread(sessionHandler)).start();
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
            return Pattern.compile(pattern,Pattern.MULTILINE).matcher(replacedString).replaceFirst(replacement);
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
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            Session.outPrintLine(String.format("Received session command: '%s'", query));
            Session seekParameters = Session.sessionMap.get(query);
            if (seekParameters == null) {
                exchange.sendResponseHeaders(200, 0);
                os.write("Session does not exists. Please do not call /sessions page directly.\nGo to /findFlow instead.".getBytes());
                os.flush();
                os.close();
                return;
            }
            if (!seekParameters.isAuthenticated()) {
                AuthenticationHandler.authenticateRedirect(exchange, seekParameters);
                return;
            }

            List<Session> sessions = new ArrayList<Session>();
            Session.sortDirection= Session.SortDirection.DESC;
            Session.sortBy= Session.SortBy.TIMESTAMP;
            sessions.addAll(Session.sessionMap.values());
            Collections.sort(sessions);
            Session.outPrintLine(String.format("Number of sessions %s", sessions.size()));

            StringBuffer response = new StringBuffer();
            response.append("<html> <head><meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"></head> <body> <h3>Vypis session</h3>");
            response.append("<table border=0 cellspacing=0>");
            for (Session session : sessions) {
                response.append("<tr>");
                response.append(String.format("<td align \"right\">Logon time: </td><td align \"left\">%s</td><td align \"left\">%s</td>", session.getLogonTime(), session.getLogonSinceTime()));
                response.append("</tr><tr>");
                response.append(String.format("<td align \"right\">Last activity time: </td><td align \"left\">%s</td><td align \"left\">%s</td>", session.getLastActivityTime(), session.getLastActivitySinceTime()));
                response.append("</tr><tr>");
                response.append(String.format("<td align \"right\">Name: </td><td align \"left\">'%s'</td><td align \"left\"></td>", session.getUserName()));
                response.append("</tr><tr>");
                response.append(String.format("<td align \"right\">Email: </td><td align \"left\">'%s'</td><td align \"left\"></td>", session.getUserMail()));
                response.append("</tr><tr>");
                response.append(String.format("<td align \"right\">Phone: </td><td align \"left\">'%s'</td><td align \"left\"></td>", session.getUserPhone()));
                response.append("</tr><tr>");
                response.append(String.format("<td align \"right\"></td><td align \"left\">%s</td><td align \"left\"></td>", "")); //delimiter
                response.append("</tr>");
            }
            response.append("</table></body> </html>");
            String responseString = response.toString();
            byte[] bytesToSend = responseString.getBytes(encoding);
            exchange.sendResponseHeaders(200, bytesToSend.length);
            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=%s", encoding));
            os.write(bytesToSend);
            os.close();
        } catch (Exception e) {
            LOGGER.error("SessionHandler",e);
        } finally {
            CloseUtil.close(os);

        }
    }

}
