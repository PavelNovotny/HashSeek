package com.o2.cz.cip.hashseek.datastore;

import java.io.File;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public interface InsertData {
    //we can do various staff with documents (compress, encrypt, etc...)
    public abstract int insertDocument(byte[] document); //store document and returns new document size
    public abstract void setSourceFile(File sourceFile); //na jeho zaklade urcime storeFile
}