package com.o2.cz.cip.hashseek.io;

import com.o2.cz.cip.hashseek.util.CloseUtil;
import net.sf.samtools.util.BlockCompressedFilePointerUtil;
import net.sf.samtools.util.BlockCompressedInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Delegate trida na RandomAccessFile
 * Created by mfryl on 16.1.14.
 */
public class BgzSeekableInputStream extends SeekableInputStream {
    BlockCompressedInputStream raf;

    private ByteArrayOutputStream bout=new ByteArrayOutputStream(8000);

    BgzIndexFile bgzIndexFile;

    public BgzSeekableInputStream(File file)throws IOException{
        super(file);
        raf = new BlockCompressedInputStream(file);
        bgzIndexFile=new BgzIndexFile(BgzUtil.getBgzIndexFile(getFile()));

    }

    @Override
    public long length() throws IOException{
        return 0;
    }

    @Override
    public long getFilePointer() throws IOException {
        return bgzIndexFile.findUncompressedPointer(raf.getFilePointer());
    }

    @Override
    public void seek(long position) throws IOException {
        long bgzPosition=bgzIndexFile.findBlockGzPointer(position);
        raf.seek(bgzPosition);
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
    public void close() throws IOException {
        CloseUtil.close(raf);
    }

    @Override
    public boolean eof() throws IOException {
        return false;
    }

    @Override
    public String readRaw(int maxLen) throws IOException {
        byte[] raw = readRawBytes(maxLen);
        if (raw == null) {
            return null;
        } else {
            return new String(raw,"UTF-8");
        }
    }

    @Override
    public int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    @Override
    public long readLong() throws IOException {
        return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
    }

    @Override
    public byte[] readRawBytes(int blockSize) throws IOException {
        bout.reset();
        int c = -1;
        boolean eol = false;
        int len = 0;
        while (len < blockSize) {
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

    @Override
    public String readRecord() throws IOException {
        bout.reset();

        int c = -1;
        boolean eol = false;
        while (!eol) {
            switch (c = read()) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    long cur = getFilePointer();
                    if ((read()) != '\n') {
                        seek(cur);
                    }
                    break;
                default:
                    bout.write((byte)c);
                    break;
            }
        }

        if ((c == -1) && (bout.size() == 0)) {
            return null;
        }
        return new String(bout.toByteArray(),"UTF-8");
    }

    @Override
    public String readWord() throws IOException {
        bout.reset();
        int c = -1;
        boolean eow = false;
        while (!eow) {
            c = read();
            if (c < 32 || c > 122) { //special nebo  {,|,},~,DEL atd..
                break;
            }
            switch (c) {
                case -1:
                case '<':
                    eow = true;
                    break;
                case '>':
                    eow = true;
                    break;
                case ';':
                    eow = true;
                    break;
                case '\"':
                    eow = true;
                    break;
                case '=':
                    eow = true;
                    break;
                case '/':
                    eow = true;
                    break;
                case ' ':
                    eow = true;
                    break;
                case '\\':
                    eow = true;
                    break;
                case ',':
                    eow = true;
                    break;
                case '(':
                    eow = true;
                    break;
                case ')':
                    eow = true;
                    break;
                case '\'':
                    eow = true;
                    break;
                case ':':
                    eow = true;
                    break;
                case '$':
                    eow = true;
                    break;
                case '^':
                    eow = true;
                    break;
                case '&':
                    eow = true;
                    break;
                case '*':
                    eow = true;
                    break;
                case '#':
                    eow = true;
                    break;
                case '!':
                    eow = true;
                    break;
                case '`':
                    eow = true;
                    break;
                case ']':
                    eow = true;
                    break;
                case '[':
                    eow = true;
                    break;
                case '?':
                    eow = true;
                    break;
                case '+':
                    eow = true;
                    break;
                case '%':
                    eow = true;
                    break;
                default:
                    bout.write((byte)c);
                    break;
            }
        }
        if ((c == -1) && (bout.size() == 0)) {
            return null;
        }
        return new String(bout.toByteArray(),"UTF-8");
    }

}
