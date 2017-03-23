package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GunzipHandler implements HttpHandler, Runnable {

    private HttpExchange exchange;
    static final Logger LOGGER= LoggerFactory.getLogger(GunzipHandler.class);
    
    public void handle(HttpExchange exchange) throws IOException {
        GunzipHandler gunzipHandler = new GunzipHandler();
        gunzipHandler.setExchange(exchange);
        (new Thread(gunzipHandler)).start();
    }

    public void setExchange(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void run() {

        try {

            exchange.getResponseHeaders().add("Content-Type", String.format("text/html; charset=UTF-8"));

            OutputStream os = exchange.getResponseBody();
            byte[] content = unpack().toString().getBytes("UTF-8");

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
        return "";
    }

    private StringBuilder unpack() throws IOException {

        StringBuilder sb = new StringBuilder();

        Map<String, Object> params = LogInfoHandler.parsePostParams(exchange);

        String dayString = (String)params.get("day");
        String type = (String)params.get("type");
        List<String> files;
        if(params.get("files") instanceof Collection) {
            //noinspection unchecked
            files = (List<String>)params.get("files");
        } else {
            files = new ArrayList<String>();
            files.add((String) params.get("files"));
        }

        String log_dir = AppProperties.getLogLocation(type);
        String hash_dir = AppProperties.getHashLocation(type);

        LogInfoHandler.printBodyBefore(sb, styles());
        LogInfoHandler.printDiskUsage(sb, log_dir);

        List<String> filesToUnpack = new ArrayList<String>();

        for(String fileName : files) {

            String dir = fileName.endsWith("hash") ? hash_dir : log_dir;
            File fileToUnpack = new File(dir + "/" + fileName);

            if(fileToUnpack.exists()) {
                sb.append("Soubor ").append(fileToUnpack.getAbsolutePath()).append(" je již rozbalen nebo se zrovna rozbaluje.<br />\n");
            } else {

                File fileToUnpackGz = new File(dir + "/" + fileName + ".gz");

                if(fileToUnpackGz.exists()) {
                    filesToUnpack.add(dir + "/" + fileName + ".gz");
                } else {
                    sb.append("Soubor NEEXISTUJE: ").append(fileToUnpackGz.getAbsolutePath()).append("<br />\n");
                }

            }
        }

        sb.append("<br /><br />Rozbalování zahájeno... počet souborů: ").append(filesToUnpack.size()).append("<br />\n");

        final StringBuilder toGunZip = LogInfoHandler.join(filesToUnpack, " ");

        for(String file : filesToUnpack) {
            sb.append(file).append("<br />\n");
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    LOGGER.info("[log] Start thread unpacking... run");
                    Runtime rt = Runtime.getRuntime();
                    rt.exec("gunzip "+ toGunZip.toString());
                    LOGGER.info("[log] Start thread unpacking... finished");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        sb.append("<input type=\"button\" onclick=\"window.location = 'gunzipForm?type=").append(type).append("&amp;day=").append(dayString).append("'\" value=\"Zpět\" />");

        LogInfoHandler.printBodyAfter(sb);

        return sb;
    }
}
