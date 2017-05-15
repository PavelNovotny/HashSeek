package com.o2.cz.cip.hashseek.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * User: Pavel
 * Date: 22.8.12 14:2
 */
public class HttpServer {
    static final Logger LOGGER= LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();
        loadPropertyFile(new File("./HttpServer.properties"), properties);
        int port = Integer.parseInt(properties.get("port").toString());
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/processCommand", new SeekHandler());
        server.start();
    }

    private static void loadPropertyFile(File propertyFile, Properties properties) throws IOException {
        try {
            properties.load(new FileInputStream(propertyFile));
        } catch (Exception e) {
            LOGGER.error(String.format("Nelze otevrit property soubor '%s'. Ujistete se, ze soubor existuje a obsahuje parametry pro hledani.\n", propertyFile.getName()));
            e.printStackTrace();
            System.exit(0);
        }
    }
}

