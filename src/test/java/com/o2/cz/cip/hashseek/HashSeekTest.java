package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.analyze.impl.DefaultOldHashSeekAnalyzer;
import com.o2.cz.cip.hashseek.seek.FileSeek;
import com.o2.cz.cip.hashseek.datastore.ExtractData;
import com.o2.cz.cip.hashseek.datastore.ExtractDataFactory;
import com.o2.cz.cip.hashseek.datastore.InsertData;
import com.o2.cz.cip.hashseek.datastore.InsertDataFactory;
import com.o2.cz.cip.hashseek.indexer.BlockFileIndexer;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.Utils;
import org.junit.Test;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by pavelnovotny on 14.01.14.
 */
public class HashSeekTest {

    public void testSeek(String seekedString, File seekedFile) throws IOException {
        FileSeek fileSeek = new FileSeek();
        List<List<String>> toSeek = new LinkedList<List<String>>();
        List<String> strings = new LinkedList<String>();
        toSeek.add(strings);
        strings.add(seekedString);
        Map<Long, Integer> positions = fileSeek.rawDocLocations(toSeek, seekedFile, 100, System.out); //pozice a délka
        RandomAccessFile raf = new RandomAccessFile(seekedFile, "r");
        byte[] seekedBytes = seekedString.getBytes();
        for (Long position : positions.keySet()) { //některé jsou falešné (kolizní), ale alespoň jedna tam musí být.
            raf.seek(position);
            byte[] rawBytes = raf.readRawBytes(positions.get(position));
            int index = Utils.indexOf(rawBytes, seekedBytes);
            if (index > -1) {
                System.out.println(new String(rawBytes, "UTF-8"));
            }
        }
    }

    @Test
    public void testNewSeek() throws IOException {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        testSeek("00000000000000000120700486", file);
    }

    @Test
    public void testCreateIndex() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        Utils.outPrintLine("started testCreateIndex");
        BlockFileIndexer blockFileIndexer = new BlockFileIndexer(sourceFile);
        //blockFileIndexer.index("DefaultOldHashSeekAnalyzer", "NoFileInsertData");
        blockFileIndexer.index("DefaultOldHashSeekAnalyzer", "GzipFileInsertData");
        Utils.outPrintLine("ended testCreateIndex");
    }

    @Test
    public void testGzInsert() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        long[] documentAddresses = BlockFileIndexer.documentAddressArray(docAddressFile);
        InsertData insertData = InsertDataFactory.createInstance("GzipFileInsertData");
        insertData.setSourceFile(sourceFile);
        for (int index=0; index<documentAddresses.length-1; index++) {
            byte[] document = BlockFileIndexer.getDocument(sourceFile, documentAddresses[index], documentAddresses[index + 1]);
            insertData.insertDocument(document);
        }
        Utils.outPrintLine("end");
    }

    @Test
    public void testGzRead() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File dataFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.dgz");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        long[] documentAddresses = BlockFileIndexer.documentAddressArray(docAddressFile);
        ExtractData extractData = ExtractDataFactory.createInstance("GzipFileExtractData");
        extractData.setDataFile(dataFile);
        InsertData insertData = InsertDataFactory.createInstance("GzipFileInsertData");
        insertData.setSourceFile(sourceFile);
        long[] gzDocAddresses = new long[documentAddresses.length];
        for (int index=0; index<documentAddresses.length-1; index++) {
            byte[] document = BlockFileIndexer.getDocument(sourceFile, documentAddresses[index], documentAddresses[index + 1]);
            int gzLen = insertData.insertDocument(document);
            gzDocAddresses[index+1] = gzDocAddresses[index]+gzLen;
        }
        for (int index=0; index<gzDocAddresses.length-1; index++) {
            byte[] data = extractData.extractDocument(gzDocAddresses[index], gzDocAddresses[index + 1] - gzDocAddresses[index]);
            System.out.println("---------------------------------");
            System.out.println(new String(data));
        }
        Utils.outPrintLine("end");
    }

    @Test
    public void testDocumentReader() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        Utils.outPrintLine("started testDocumentReader");
        long[] documentAddresses = BlockFileIndexer.documentAddressArray(docAddressFile);
        for (int index=0; index<documentAddresses.length-1; index++) {
            System.out.println("----------------------------------------------");
            System.out.println(BlockFileIndexer.getDocumentString(sourceFile, documentAddresses[index], documentAddresses[index + 1]));
        }
        Utils.outPrintLine("ended testDocumentReader");
    }

    @Test
    public void testAnalyzer() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        PrintStream out = new PrintStream(new FileOutputStream("analyzer.txt"));
        long[] documentAddresses = BlockFileIndexer.documentAddressArray(docAddressFile);
        DefaultOldHashSeekAnalyzer analyzer = new DefaultOldHashSeekAnalyzer();
        for (int docIndex=0; docIndex<documentAddresses.length-1; docIndex++) {
            long start = documentAddresses[docIndex];
            long end = documentAddresses[docIndex+1];
            byte[] doc = BlockFileIndexer.getDocument(sourceFile, start, end);
            byte[][] words = analyzer.analyze(doc);
            for (int i=0; i<words.length;i++) {
                out.println(new String(words[i]));
            }
        }
    }


}
