package com.o2.cz.cip.hashseek.o2seek.http;

import com.o2.cz.cip.hashseek.o2seek.O2Seek;
import com.o2.cz.cip.hashseek.o2seek.SeekParamsDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * User: Pavel
 * Date: 29.9.12 12:20
 */
class SeekHandler implements HttpHandler, Runnable {
    static Logger loggerAccess= LoggerFactory.getLogger("hashseek.access");
    static  Logger LOGGER=LoggerFactory.getLogger(SeekHandler.class);
    
	private HttpExchange exchange;
	private SeekHandler parent;
	private SeekHandler activeChild;
	private int numberOfThreads = 0;
	private static int MAX_THREADS = 1;

	public void handle(HttpExchange exchange) throws IOException {
		if (numberOfThreads < MAX_THREADS) {
			numberOfThreads++;
			SeekHandler seekHandler = new SeekHandler();
			seekHandler.setExchange(exchange);
			seekHandler.setParent(this);
			(new Thread(seekHandler)).start();
			this.activeChild = seekHandler;
		} else {
			URI requestURI = activeChild.getExchange().getRequestURI();
			String query = requestURI.getQuery();
			OutputStream os = exchange.getResponseBody();
            Writer writer = new OutputStreamWriter(os);
			exchange.sendResponseHeaders(200, 0);
            //todo ujasnit formát chyb
            JSONObject obj = new JSONObject();
            obj.put("error", String.format("Maximum number of concurrent seeks '%s' exceeded.", MAX_THREADS));
            obj.writeJSONString(writer);
            writer.close();
			os.close();
		}
	}

	public HttpExchange getExchange() {
		return exchange;
	}

	public void setExchange(HttpExchange exchange) {
		this.exchange = exchange;
	}

	public SeekHandler getParent() {
		return parent;
	}

	public void setParent(SeekHandler parent) {
		this.parent = parent;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}



    private boolean notNum(String string) {
		try {
			Integer.valueOf(string);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean writeInteractiveParam(OutputStream os, BufferedReader bufferedReader, PrintWriter writer, String param) throws IOException, InterruptedException {
		String line = writeParamLineFromProcess(bufferedReader, os);
		if (line.contains("Chyba vstupu.")) {
			return false;
		}
		writeParam(os, writer, param);
		return true;
	}

	private void writeParam(OutputStream os, PrintWriter writer, String param) throws IOException, InterruptedException {
		os.write(String.format("%s\n", param).getBytes());
		os.flush();
		writer.println(param);
		writer.flush();
//        Thread.sleep(500);
	}

	private String writeParamLineFromProcess(BufferedReader input, OutputStream os) throws IOException {
		char[] cbuf = new char[10];
		String line = "";
		while (!line.contains(":")) {
			int numberReads = input.read(cbuf);
			String chunk = new String(cbuf, 0, numberReads);
			line = line.concat(chunk);
		}
		os.write(line.getBytes());
		os.flush();
		return line;
	}

	public static void main(String[] args) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("java");
		Map<String, String> env = pb.environment();
		pb.directory(new File("./"));
		pb.redirectErrorStream(true);
		Process p = pb.start();
		InputStream inputStream = p.getInputStream();
		BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = input.readLine()) != null) {
			LOGGER.info(line);
		}
	}

	public void run() {
		OutputStream os = exchange.getResponseBody();
        Writer writer = new OutputStreamWriter(os);
        StringBuffer buf = new StringBuffer();
		try {
			BufferedInputStream httpBin = new BufferedInputStream(exchange.getRequestBody());
			while (httpBin.available() > 0 && buf.length() < 4096) { //zpracujeme max 4KB
				buf.append((char) httpBin.read());
			}
			URI requestURI = exchange.getRequestURI();
			String query = requestURI.getQuery();
			System.out.println(String.format("Received query: '%s'", query));
			exchange.sendResponseHeaders(200, 0);
            JSONObject par = (JSONObject) parseParams(buf);
            seek(par, writer);
		} catch (Exception e) {
            LOGGER.error(buf.toString());
            LOGGER.error("run",e);
            JSONObject obj = new JSONObject();
            obj.put("error", e);
            obj.put("message", buf.toString());
            try {
                obj.writeJSONString(writer);
            } catch (IOException e1) {
                LOGGER.error("run", e1);
            }
        } finally {
            try {
                writer.close();
                os.close();
            } catch (IOException e1) {
                LOGGER.error("run-catch", e1);
                e1.printStackTrace();
            }
			parent.setNumberOfThreads(parent.getNumberOfThreads() - 1);
		}
	}

    private void seek(JSONObject seekParam, Writer writer) throws IOException, ParseException, java.text.ParseException {
        O2Seek o2seek = new O2Seek();
        SeekParamsDto seekParamsDto = new SeekParamsDto(seekParam);
        o2seek.seek(seekParamsDto).writeJSONString(writer);
    }



    private Object parseParams(StringBuffer buf) throws ParseException {
        JSONParser parser = new JSONParser();
        return parser.parse(buf.toString());
    }

}


