package com.o2.cz.cip.hashseek.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * slouzi jako abstrakce pro RandomAccessFile a BlockCompressedInputStream
 *
 * Created by mfrydl on 16.1.14.
 */
public abstract class SeekableInputStream extends InputStream {
    File file;

    public File getFile() {
        return file;
    }

    protected SeekableInputStream(File file) {
        this.file = file;
    }

    public abstract long length() throws IOException;

    public abstract long getFilePointer() throws IOException;

    public abstract void seek(long position) throws IOException;

    public abstract int read(byte[] buffer, int offset, int length) throws IOException;

    public abstract void close() throws IOException;

    public abstract boolean eof() throws IOException;

    public abstract String readWord() throws IOException;

    public abstract String readRecord() throws IOException;

    public abstract String readRaw(int maxLen) throws IOException;

    public abstract  int readInt() throws IOException;

    public abstract  long readLong() throws IOException;
    public abstract  byte[] readRawBytes(int blockSize) throws IOException;
}
