package com.o2.cz.cip.hashseek.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Pavel
 * Date: 29.9.12 12:20
 */
class EchoHandler implements HttpHandler {
    public void handle(HttpExchange xchg) throws IOException {
        Headers headers = xchg.getRequestHeaders();
        Set<Map.Entry<String, List<String>>> entries = headers.entrySet();

        StringBuffer response = new StringBuffer();
        for (Map.Entry<String, List<String>> entry : entries)
            response.append(entry.toString() + "\n");

        xchg.sendResponseHeaders(200, response.length());
        OutputStream os = xchg.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
}
