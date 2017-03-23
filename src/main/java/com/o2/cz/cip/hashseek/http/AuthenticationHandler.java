package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.util.CloseUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 21.12.12 9:38
 */
public class AuthenticationHandler implements HttpHandler, Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(AuthenticationHandler.class);
    private HttpExchange exchange;
    private String encoding="UTF-8";

    public void handle(HttpExchange exchange) throws IOException {
        AuthenticationHandler authenticateHandler = new AuthenticationHandler();
        authenticateHandler.setExchange(exchange);
        (new Thread(authenticateHandler)).start();
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public void setExchange(HttpExchange exchange) {
        this.exchange = exchange;
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
            Session.outPrintLine(String.format("Received authenticated command: '%s'", query));
            Session session = Session.sessionMap.get(query);
            if (session == null) {
                exchange.sendResponseHeaders(200, 0);
                os.write("Session does not exists. Please do not call /authenticate page directly.\nGo to /findFlow instead.".getBytes());
                os.flush();
                os.close();
                return;
            }
            BufferedReader htmlReader = new BufferedReader(new InputStreamReader(new FileInputStream("./Authenticate.html"), encoding));
            StringBuffer response = new StringBuffer();
            String htmlLine;
            while ((htmlLine = htmlReader.readLine()) != null) {
                response.append(htmlLine);
            }
            String responseString = response.toString();
            responseString = replaceFirstRegex("\"session\" value=\"[^\"]*\"",String.format("\"session\" value=\"%s\"", session.getSession()), responseString);

            byte[] bytesToSend = responseString.getBytes(encoding);
            exchange.sendResponseHeaders(200, bytesToSend.length);
            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=%s", encoding));
            os.write(bytesToSend);
            os.close();
        } catch (Exception e) {
            LOGGER.error("Authentication",e);
        } finally {
            CloseUtil.close(os);

        }
    }

    private String replaceFirstRegex(String pattern, String replacement, String replacedString) {
        if (replacement.contains("null")) {
            return replacedString;
        } else {
            return Pattern.compile(pattern, Pattern.MULTILINE).matcher(replacedString).replaceFirst(replacement);
        }
    }

    public static void authenticateRedirect(HttpExchange exchange, Session session) throws IOException {
        OutputStream os = exchange.getResponseBody();
        Headers headers = exchange.getResponseHeaders();
        headers.add("Location",String.format("/authenticate?%s", session.getSession()));
        exchange.sendResponseHeaders(302, 0L);
        os.write("Authentication needed, redirecting... if fails, please goto /authenticateRedirect page manually.".getBytes());
        os.flush();
        os.close();
    }

}
