package com.o2.cz.cip.hashseek.pokus;

import com.o2.cz.cip.hashseek.io.RandomAccessFile;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by pavelnovotny on 31.03.17.
 */
//Document gz
public class Dgz {
    byte[] gz =   new byte[10000000];
    byte[] read = new byte[100000000];

    public static void main(String[] args) throws IOException {
        Dgz dgz = new Dgz();
        dgz.compress();
    }

    private void compress() throws IOException {
        File file1 = new File("/Users/pavelnovotny/Downloads/4801_20170330-20170330-5.txt");
        File file2 = new File("/Users/pavelnovotny/Downloads/2754_20170331-20170429.txt");
        File dest = new File("/Users/pavelnovotny/Downloads/dest.gz");
        long len1 = writeGzChunk("ahoj".getBytes(), dest, false);
        System.out.println(len1);
        long len2 = writeGzChunk("XXXXXXXX".getBytes(), dest, true);
        System.out.println(len2);
        String chunk1 = readGzChunk(dest, 0, len1);
        String chunk2 = readGzChunk(dest, len1, len2);
        System.out.println(chunk1);
        System.out.println(chunk2);

        len1 = writeGzChunk("ahoj xxxxxxxxxxxxxx af asfas fůaslf asůlfk aůslf asůldfkasdjfůlasjflůasdjfasůdfasjfjf  afůa sfaks faůs".getBytes(), dest, false);
        System.out.println(len1);
        len2 = writeGzChunk("XXXXXXXX afasf asfl asfafas fsf asfkHH".getBytes(), dest, true);
        long len3 = writeGzChunk("XXXXXXXX afasf asfl asfafas fsf asfkHH bbbbbbbbbbbbbb NNNNNNN".getBytes(), dest, true);
        System.out.println(len2);
        chunk1 = readGzChunk(dest, 0, len1);
        chunk2 = readGzChunk(dest, len1, len2);
        String chunk3 = readGzChunk(dest, len2, len3);
        System.out.println(chunk1);
        System.out.println(chunk2);
        System.out.println(chunk3);

    }

    private String readGzChunk(File file, long offset, long len) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(offset);
        int readLen = raf.read(gz);
        GZIPInputStream gzIn = new GZIPInputStream(new ByteArrayInputStream(gz));
        int unzipLen = gzIn.read(read);
        return new String(read, 0, unzipLen);
    }

    private long writeGzChunk(byte[] bytes, File file, boolean append) throws IOException {
        GZIPOutputStream gzOut = new GZIPOutputStream(new FileOutputStream(file, append));
        gzOut.write(bytes);
        gzOut.finish();
        gzOut.close();
        return file.length();
    }


}
