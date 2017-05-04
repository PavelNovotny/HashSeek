package com.o2.cz.cip.hashseek.datastore;

import com.o2.cz.cip.hashseek.datastore.impl.PlainFileDataStore;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class DataStoreFactory {

    public static DataStore createInstance(String analyzer) {
        if ("PlainFileDataStore".equals(analyzer)) {
            return new PlainFileDataStore();
        } else {
            return new PlainFileDataStore();
        }

    }
}
