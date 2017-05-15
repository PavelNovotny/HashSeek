package com.o2.cz.cip.hashseek.common.datastore.impl;

import com.o2.cz.cip.hashseek.common.datastore.ExtractData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public class GzipFileExtractData implements ExtractData {

    private File dataFile;
    //todo velikosti a inicializace
    private static final int MAX_DOC_SIZE_UNZIPPED = 10*1024*1024; //10 MB
    private byte[] document = new byte[MAX_DOC_SIZE_UNZIPPED];

    @Override
    public byte[] extractDocument(long pos, long len) {
        int unzipLen=0;
        try {
            FileInputStream fin = new FileInputStream(dataFile);
            fin.skip(pos);
            GZIPInputStream gzIn = new GZIPInputStream(fin);
            unzipLen = gzIn.read(document);
            gzIn.close();
            fin.close();
        } catch (IOException e) {
            //todo better error log
            e.printStackTrace();
        }
        return Arrays.copyOf(document, unzipLen);
    }

    @Override
    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public void close() {
        //todo inicializace streamu do setDataFile a close streamu sem
    }
}
