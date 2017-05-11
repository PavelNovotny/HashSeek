package com.o2.cz.cip.hashseek.datastore.impl;

import com.o2.cz.cip.hashseek.datastore.InsertData;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class GzipFileInsertData implements InsertData {

    private File storeFile;

    @Override
    public int insertDocument(byte[] document) {
        long startSize = storeFile.length();
        try {
            GZIPOutputStream gzOut = new GZIPOutputStream(new FileOutputStream(storeFile, true));
            gzOut.write(document);
            gzOut.finish();
            gzOut.close();
        } catch (IOException e) {
            //todo better error log
            e.printStackTrace();
        }
        long endSize = storeFile.length();
        return (int) (endSize - startSize);
    }

    @Override
    public void setSourceFile(File sourceFile) {
        String sourceFileName = sourceFile.getAbsolutePath();
        this.storeFile = new File(sourceFileName + ".dgz");
    }

    @Override
    public void close() {
        //todo inicializace streamu do setSourceFile a close streamu sem
    }
}
