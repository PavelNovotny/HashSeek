package com.o2.cz.cip.hashseek.datastore.impl;

import com.o2.cz.cip.hashseek.datastore.InsertData;

import java.io.*;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class NoFileInsertData implements InsertData { //just keeps original file and only index

    @Override
    public int insertDocument(byte[] document) {
        return document.length;
    }

    @Override
    public void setSourceFile(File sourceFile) {
    }
}
