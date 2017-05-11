package com.o2.cz.cip.hashseek.datastore;

import com.o2.cz.cip.hashseek.datastore.impl.GzipFileExtractData;
import com.o2.cz.cip.hashseek.datastore.impl.PlainFileExtractData;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class ExtractDataFactory {

    public static ExtractData createInstance(String dataStore) {
        if ("PlainFileExtractData".equals(dataStore)) {
            return new PlainFileExtractData();
        } else if ("GzipFileExtractData".equals(dataStore)) {
            return new GzipFileExtractData();
        } else {
            return null;
        }

    }
}
