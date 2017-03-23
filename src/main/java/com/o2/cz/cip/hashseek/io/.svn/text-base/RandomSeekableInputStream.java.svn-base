package com.o2.cz.cip.hashseek.io;

import com.o2.cz.cip.hashseek.logs.auditlog.HashSeekAuditLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Delegate trida na RandomAccessFile
 * Created by mfryl on 16.1.14.
 */
public class RandomSeekableInputStream extends SeekableInputStream {
    RandomAccessFile raf;

    public RandomSeekableInputStream(File file, String mode) throws FileNotFoundException {
        this(file, mode, 65536);
    }

    public RandomSeekableInputStream(File file, String mode, int bufferlength)throws FileNotFoundException{
        super(file);
        raf = new RandomAccessFile(file, mode, bufferlength);
    }

    @Override
    public long length() throws IOException{
        return raf.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    @Override
    public void seek(long position) throws IOException {
        raf.seek(position);
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return raf.read(buffer,offset,length);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return raf.read(b);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @Override
    public boolean eof() throws IOException {
        return false;
    }

    @Override
    public String readWord() throws IOException {
        return raf.readWord();
    }

    @Override
    public String readRecord() throws IOException {
        return raf.readRecord();
    }

    @Override
    public String readRaw(int maxLen) throws IOException {
        return raf.readRaw(maxLen);
    }

    @Override
    public int readInt() throws IOException {
        return raf.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return raf.readLong();
    }

    @Override
    public byte[] readRawBytes(int blockSize) throws IOException {
        return raf.readRawBytes(blockSize);
    }
}
