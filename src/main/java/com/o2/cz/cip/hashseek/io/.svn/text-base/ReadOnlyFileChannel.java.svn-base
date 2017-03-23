package com.o2.cz.cip.hashseek.io;

import net.sf.samtools.util.BlockCompressedInputStream;

import java.io.*;

/**
 * User: Pavel
 * Date: 24.3.13 7:15
 */
public class ReadOnlyFileChannel {
    public static final int END_BIG = 0x01;
    public static final int END_SMALL = 0x02;
    public static final int END_ALL = END_BIG  | END_SMALL;
    private static final int BUFFER_SIZE = 67108864;
    public static final long INITIAL_DJB2 = 5381;
    public int javaHashSmall; //java hash maleho slova
    public int javaHashBig; //java hash velkeho slova
    public long djb2HashBig; //djb2 hash velkeho slova
    public long djb2HashSmall; //djb2 hash maleho slova
    public int smallWordLength; //delka maleho slova
    public int bigWordLength; //delka velkeho slova
    public long wordPosition = 0L;
    private long filePosition = 0L;
    private FileInputStream fis;
    private long bufferPos = 0L;
    private long nextWordPosition = 0L;
    InputStream inStream;

    public ReadOnlyFileChannel(File file) throws IOException {
        fis = new FileInputStream(file);
        if(BgzUtil.isBgzFile(file)){
            inStream= new BlockCompressedInputStream(fis);
        }else{
            inStream = new BufferedInputStream(fis, BUFFER_SIZE);
        }
        this.javaHashBig = 0;
        this.javaHashSmall = 0;
        this.djb2HashBig = INITIAL_DJB2;
        this.djb2HashSmall = INITIAL_DJB2;
        this.smallWordLength = 0;
        this.bigWordLength = 0;
    }

    public int readWords() throws IOException {
        int end = 0;
        int character;
        javaHashSmall=0;
        djb2HashSmall=INITIAL_DJB2;
        smallWordLength =0;
        wordPosition = nextWordPosition;
        while (end == 0) {
            character = inStream.read();
            if (character == -1) {
                return 0;
            }
            filePosition++;
            if (character < 32 || character > 122) { //special nebo  {,|,},~,DEL atd..
                nextWordPosition = filePosition;
                end = END_ALL;
                break;
            }
            switch (character) {
                case '<':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '>':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case ';':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '\"':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '=':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '/':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case ' ':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '\\':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case ',':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '(':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case ')':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '\'':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case ':':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '$':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '^':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '&':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '*':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '#':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '!':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '`':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case ']':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '[':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '?':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '+':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '%':
                    nextWordPosition = filePosition;
                    end = END_ALL;
                    break;
                case '-':
                    javaHashBig = 31 * javaHashBig + (char)character;
                    djb2HashBig = ((djb2HashBig << 5) + djb2HashBig) + (char)character; /* hash * 33 + c */
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                case '@':
                    javaHashBig = 31 * javaHashBig + (char)character;
                    djb2HashBig = ((djb2HashBig << 5) + djb2HashBig) + (char)character; /* hash * 33 + c */
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                case '_':
                    javaHashBig = 31 * javaHashBig + (char)character;
                    djb2HashBig = ((djb2HashBig << 5) + djb2HashBig) + (char)character; /* hash * 33 + c */
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                case '.':
                    javaHashBig = 31 * javaHashBig + (char)character;
                    djb2HashBig = ((djb2HashBig << 5) + djb2HashBig) + (char)character; /* hash * 33 + c */
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                default:
                    javaHashBig = 31 * javaHashBig + (char)character;
                    javaHashSmall = 31 * javaHashSmall + (char)character;
                    djb2HashBig = ((djb2HashBig << 5) + djb2HashBig) + (char)character; /* hash * 33 + c */
                    djb2HashSmall = ((djb2HashSmall << 5) + djb2HashSmall) + (char)character; /* hash * 33 + c */
                    smallWordLength++;
                    bigWordLength++;
                    break;
            }
        }
        return end;
    }

    public void close() throws IOException {
        inStream.close();
    }


}
