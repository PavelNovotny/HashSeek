package com.o2.cz.cip.hashseek.datastore;

import com.o2.cz.cip.hashseek.datastore.impl.GzipFileInsertData;
import com.o2.cz.cip.hashseek.datastore.impl.NoFileInsertData;
import com.o2.cz.cip.hashseek.datastore.impl.PlainFileInsertData;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class InsertDataFactory {

    public static InsertData createInstance(String dataStore) {
        if ("PlainFileInsertData".equals(dataStore)) {
            return new PlainFileInsertData();
        } else if ("NoFileInsertData".equals(dataStore)) {
            return new NoFileInsertData();
        } else if ("GzipFileInsertData".equals(dataStore)) {
            return new GzipFileInsertData();
        } else {
            return null;
        }

    }
}
