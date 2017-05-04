package com.o2.cz.cip.hashseek.datastore;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public interface DataStore {
    //we can do various staff with documents (compress, encrypt, etc...)
    public abstract int insertDocument(byte[] document); //store document and returns new document size
    public abstract byte[] extractDocument(long pos, long len); //retrieve document
}
