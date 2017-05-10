package com.o2.cz.cip.hashseek.io;

import java.io.*;

/**
 * User: Pavel
 * Date: 19.8.12 20:31
 */
public class RandomAccessFile extends java.io.RandomAccessFile {
    private byte[] bytebuffer;
    private int maxread;
    private int buffpos;
    private ByteArrayOutputStream bout=new ByteArrayOutputStream(8000);

    public RandomAccessFile(File file, String mode) throws FileNotFoundException {
        this(file, mode, 65536);
    }

    public RandomAccessFile(File file, String mode, int bufferlength) throws FileNotFoundException {
        super(file, mode);
        bytebuffer = new byte[bufferlength];
        maxread = 0;
        buffpos = 0;
    }

    @Override
    public int read() throws IOException {
        if (buffpos >= maxread) {
            maxread = readchunk();
            if (maxread == -1) {
                return -1;
            }
        }
        buffpos++;
        return bytebuffer[buffpos - 1] & 0xFF;
    }

    @Override
    public long getFilePointer() throws IOException {
        return super.getFilePointer() + buffpos;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (maxread != -1 && pos < (super.getFilePointer() + maxread) && pos > super.getFilePointer()) {
            Long diff = (pos - super.getFilePointer());
            if (diff < Integer.MAX_VALUE) {
                buffpos = diff.intValue();
            } else {
                throw new IOException("something wrong w/ seek");
            }
        } else {
            buffpos = 0;
            super.seek(pos);
            maxread = readchunk();
        }
    }

    private int readchunk() throws IOException {
        long pos = super.getFilePointer() + buffpos;
        super.seek(pos);
        int read = super.read(bytebuffer);
        super.seek(pos);
        buffpos = 0;
        return read;
    }


    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        throw new IOException("TODO Must be implemented");
    }

    @Override
    public int read(byte[] b) throws IOException {
        for (int i = 0; i < b.length; i++) {
            int result=read();
            if(result==-1){
                if(i==0){
                    return -1;
                }else{
                    return i;
                }
            }
            b[i] =  (byte)result;

        }
        return  b.length;
    }

    public String readRaw(int maxLen) throws IOException {
        byte[] raw = readRawBytes(maxLen);
        if (raw == null) {
            return null;
        } else {
            return new String(raw,"UTF-8");
        }
    }

    public byte[] readRawBytes(int maxLen) throws IOException {
        bout.reset();
        int c = -1;
        boolean eol = false;
        int len = 0;
        while (len < maxLen) {
            switch (c = read()) {
                case -1:
                    eol = true;
                    break;
                default:
                    bout.write((byte) c);
                    len++;
                    break;
            }
        }
        if ((c == -1) && (bout.size() == 0)) {
            return null;
        }
        return bout.toByteArray();
    }
}
