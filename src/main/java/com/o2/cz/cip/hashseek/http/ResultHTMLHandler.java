package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.html.HTMLOutput5;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

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
class ResultHTMLHandler implements HttpHandler, Runnable {
    static Logger LOGGER= LoggerFactory.getLogger(ResultHTMLHandler.class);

	private String encoding = "utf-8";
	HttpExchange exchange;

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		ResultHTMLHandler handler = new ResultHTMLHandler();
		handler.exchange = httpExchange;
		(new Thread(handler)).start();
	}

	@Override
	public void run() {

		try {

			Session session = Session.sessionMap.get(exchange.getRequestURI().getQuery());

			//session.setFileName("d:\\hs2.txt");

			String filename = session.getFileName();

			StringBuffer out = HTMLOutput5.process(filename, session.getDefect());

			//String s = out.toString().replaceAll("(3621362247)", "<span style=\"background-color: #33FF00\">$1</span>");
			//out = new StringBuffer(s);

			exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=%s", encoding));

			OutputStream os = exchange.getResponseBody();
			byte[] content = out.toString().getBytes(encoding);

			exchange.sendResponseHeaders(200, content.length);
			os.write(content);
			os.flush();
			os.close();

		} catch (Exception e) {
			HashSeekConstants.outPrintLine(e.getMessage());
			sendError();
		}

	}

	public void sendError() {
		try {
			String message = "<h1>Oooooops, asi nejsou vysledky...</h1>";
			exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=%s", encoding));
			exchange.sendResponseHeaders(200, 0);
			OutputStream os = exchange.getResponseBody();
			os.write(message.getBytes());
			os.flush();
			os.close();
		} catch (IOException e) {
            LOGGER.error("sendError",e);
		}

	}
}
