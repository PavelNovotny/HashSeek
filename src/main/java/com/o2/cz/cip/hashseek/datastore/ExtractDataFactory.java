package com.o2.cz.cip.hashseek.datastore;

import com.o2.cz.cip.hashseek.datastore.impl.GzipFileExtractData;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class ExtractDataFactory {

    public static ExtractData createInstance(String dataStore) {
        if ("PlainFileExtractData".equals(dataStore)) {
            //todo implement
            return null;
        } else if ("GzipFileExtractData".equals(dataStore)) {
            return new GzipFileExtractData();
        } else {
            return null;
        }

    }
}
