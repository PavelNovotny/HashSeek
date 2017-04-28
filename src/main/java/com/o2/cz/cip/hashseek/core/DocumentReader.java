package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.io.RandomAccessFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by pavelnovotny on 24.04.17.
 */
public class DocumentReader {


    public static byte[] getDocument(File file, long start, long end) throws IOException {
        if (start > end) {
            throw new IOException("Wrong document offset");
        }
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        int size = (int) (end - start);
        raf.seek(start);
        byte[] bytes = new byte[size];
        int actualSize = raf.read(bytes);
        if (actualSize != size) {
            throw new IOException("Something wrong with document size");
        }
        return bytes;
    }

    public static String getDocumentString(File file, long start, long end) throws IOException {
        return new String(getDocument(file, start, end));
    }


}
