package com.o2.cz.cip.hashseek.o2seek.http;

import com.o2.cz.cip.hashseek.o2seek.ConfigurationDto;
import com.o2.cz.cip.hashseek.o2seek.O2Seek;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * User: Pavel
 * Date: 22.8.12 14:2
 */
public class HttpServer {
    static final Logger LOGGER= LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) throws IOException, ParseException {

        JSONParser parser = new JSONParser();
        //todo location of configuration file in startup parameters
        JSONObject obj = (JSONObject) parser.parse(new FileReader("configuration.json"));
        O2Seek.conf = new ConfigurationDto(obj);
        //todo možná sjednotit s json konfigurací
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

