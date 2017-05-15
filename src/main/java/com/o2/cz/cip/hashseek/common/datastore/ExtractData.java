package com.o2.cz.cip.hashseek.common.datastore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by pavelnovotny on 04.05.17.
 */
public interface ExtractData {
    //we can do various staff with documents (compress, encrypt, etc...)
    public abstract byte[] extractDocument(long pos, long len); //retrieve document
    public abstract void setDataFile(File dataFile) throws FileNotFoundException;
    public abstract void close() throws IOException;
}
