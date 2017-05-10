package com.o2.cz.cip.hashseek.datastore.impl;

import com.o2.cz.cip.hashseek.datastore.InsertData;

import java.io.*;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class PlainFileInsertData implements InsertData {

    private File storeFile;

    @Override
    public int insertDocument(byte[] document) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(storeFile, true)));
            out.write(document);
            out.flush();
            out.close();
        } catch (IOException e) {
            //todo log error better
            e.printStackTrace();
        }
        return document.length;
    }

    @Override
    public void setSourceFile(File sourceFile) {
        String sourceFileName = sourceFile.getAbsolutePath();
        this.storeFile = new File(sourceFileName + ".plain");
    }
}
