package com.o2.cz.cip.hashseek.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;

/**
 * User: Pavel
 * Date: 29.9.12 12:20
 */
class DownloadHandler implements HttpHandler, Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(DownloadHandler.class);

    private HttpExchange exchange;

    public void handle(HttpExchange exchange) throws IOException {
        DownloadHandler downloadHandler = new DownloadHandler();
        downloadHandler.setExchange(exchange);
        (new Thread(downloadHandler)).start();
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public void setExchange(HttpExchange exchange) {
        this.exchange = exchange;
    }

    private void sendErrorMessage(HttpExchange exchange, Headers headers, OutputStream os, String errorMessage) throws IOException {
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"","error-message.txt"));
        exchange.sendResponseHeaders(200, 0);
        os.write(errorMessage.getBytes());
        os.flush();
        os.close();
        return;
    }

    public void run() {
        OutputStream os = exchange.getResponseBody();
        try {
            BufferedInputStream httpBin = new BufferedInputStream(exchange.getRequestBody());
            StringBuffer buf = new StringBuffer("UTF-8");
            while (httpBin.available() > 0 && buf.length() < 4096) { //zpracujeme max 4KB
                buf.append((char)httpBin.read());
            }
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            Session.outPrintLine(String.format("Received download command: '%s'", query));
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type","application/force-download");
            headers.add("Content-Transfer-Encoding", "binary");
            headers.add("Accept-Ranges", "bytes");
            Session session = Session.sessionMap.get(query);
            if (session == null) {
                sendErrorMessage(exchange, headers, os, "SPATNA IDENTIFIKACE POZADAVKU.");
                return;
            }
            if (!session.isAuthenticated()) {
                AuthenticationHandler.authenticateRedirect(exchange,session);
                return;
            }
            String fileName = session.getFileName();
            if (fileName == null) {
                sendErrorMessage(exchange, headers, os, "SOUBOR NENI Z PROCESU NASETOVAN, BYLO ZAHAJENO HLEDANI?");
                return;
            }
            File file = new File(fileName);
            if (!file.exists()) {
                sendErrorMessage(exchange, headers, os, "SOUBOR JESTE NEEXISTUJE, BYLO HLEDANI DOKONCENO?");
                return;
            }
            if (file.lastModified() < session.getTimestamp()) {
                sendErrorMessage(exchange, headers, os, "SOUBOR S VYSLEDKY JE STARY, BYLO HLEDANI DOKONCENO?");
                return;
            }
            headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"",file.getName()));
            exchange.sendResponseHeaders(200, 0);
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = input.readLine()) != null) {
                os.write(String.format("%s\r\n",line).getBytes("UTF-8"));
            }
            os.flush();
            os.close();
        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(200, 0);
                os.close();
            } catch (IOException e1) {
                LOGGER.error("Download",e);
                LOGGER.error("Download",e1);
            }
        }
    }

}
