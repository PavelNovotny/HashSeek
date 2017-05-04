package com.o2.cz.cip.hashseek.datastore.impl;

import com.o2.cz.cip.hashseek.datastore.DataStore;

import javax.xml.crypto.Data;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class PlainFileDataStore implements DataStore {

    @Override
    public int insertDocument(byte[] document) {
        return 0;
    }

    @Override
    public byte[] extractDocument(long pos, long len) {
        return new byte[0];
    }
}
