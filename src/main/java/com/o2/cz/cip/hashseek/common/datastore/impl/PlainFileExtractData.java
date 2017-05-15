package com.o2.cz.cip.hashseek.common.datastore.impl;

import com.o2.cz.cip.hashseek.common.datastore.ExtractData;

import java.io.*;
import java.util.Arrays;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class PlainFileExtractData implements ExtractData {

    private File dataFile;
    //todo jestli bude pomalé tak nahradit RandomAccessFile
    private FileInputStream fin;
    //todo velikosti a inicializace
    private static final int MAX_DOC_SIZE= 10*1024*1024; //10 MB
    private byte[] document = new byte[MAX_DOC_SIZE];

    @Override
    public byte[] extractDocument(long pos, long len) {
        int docLen=0;
        try {
            fin.skip(pos);
            docLen = fin.read(document);
        } catch (IOException e) {
            //todo better error log
            e.printStackTrace();
        }
        return Arrays.copyOf(document, docLen);
    }

    @Override
    public void setDataFile(File dataFile) throws FileNotFoundException {
        fin = new FileInputStream(dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public void close() throws IOException {
        fin.close();
    }
}
