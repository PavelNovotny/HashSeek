package com.o2.cz.cip.hashseek.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2013 EXNET s.r.o.
 * Chodska 1032/27, 120 00 - Praha 2, Czech Republic
 * http://www.exnet.cz
 * All Rights Reserved.
 *
 * @author Vladimir Zboril (zboril@exnet.cz)
 *         6.12.13 - 9:00
 * @version $Id$
 */
class ClipboardHandler implements HttpHandler, Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(ClipboardHandler.class);

	private String encoding = "utf-8";
	private HttpExchange exchange;

	public void handle(HttpExchange exchange) throws IOException {
		ClipboardHandler clipboardHandler = new ClipboardHandler();
		clipboardHandler.setExchange(exchange);
		(new Thread(clipboardHandler)).start();
	}

	public void setExchange(HttpExchange exchange) {
		this.exchange = exchange;
	}

	public void run() {
		OutputStream os = exchange.getResponseBody();
		try {

			Map<String, String> parameters = parseParameters(exchange);

			Headers headers = exchange.getResponseHeaders();
			headers.add("Content-Type", "application/force-download");
			headers.add("Content-Transfer-Encoding", "binary");
			headers.add("Accept-Ranges", "bytes");

			String filename = parameters.get("filename");
			headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));

			String data = parameters.get("data");
			data = URLDecoder.decode(data, encoding);
			byte[] content = data.getBytes(encoding);

			exchange.sendResponseHeaders(200, content.length);

			os.write(content);
			os.flush();
			os.close();

		} catch (Exception e) {
			try {
				exchange.sendResponseHeaders(200, 0);
				os.close();
			} catch (IOException e1) {
                LOGGER.error("Clipboard",e);
                LOGGER.error("Clipboard",e1);
			}
		}
	}

	private Map<String, String> parseParameters(HttpExchange exchange) throws Exception {

		BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), encoding));
		StringBuffer buf = new StringBuffer();
		String readLine;
		while ((readLine = in.readLine()) != null) {
			buf.append(readLine).append("\n");
		}

//		InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), encoding);
//		int c;
//		while ((c = isr.read()) != -1){
//			buf.append((char)c);
//		}

		Map<String, String> result = new HashMap<String, String>();
		String paramString = buf.toString();
		String[] params = paramString.split("&");
		for (int i = 0; i < params.length; i++) {
			String[] paramPair = params[i].split("=");
			result.put(paramPair[0], paramPair[1]);
		}
		return result;
	}

}
