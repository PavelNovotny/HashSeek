package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.common.analyze.impl.DefaultOldHashSeekAnalyzer;
import com.o2.cz.cip.hashseek.common.seek.DataDocument;
import com.o2.cz.cip.hashseek.common.seek.SeekIndexFile;
import com.o2.cz.cip.hashseek.common.datastore.ExtractData;
import com.o2.cz.cip.hashseek.common.datastore.ExtractDataFactory;
import com.o2.cz.cip.hashseek.common.datastore.InsertData;
import com.o2.cz.cip.hashseek.common.datastore.InsertDataFactory;
import com.o2.cz.cip.hashseek.common.indexer.BlockFileIndexer;
import com.o2.cz.cip.hashseek.common.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.common.util.Utils;
import org.junit.Test;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by pavelnovotny on 14.01.14.
 */
public class HashSeekTest {

    public void testOldSeek(String seekedString, File seekedFile, File hashFile) throws IOException {
        SeekIndexFile seekIndexFile = new SeekIndexFile(hashFile, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer", 100);
        List<List<String>> toSeek = new LinkedList<List<String>>();
        List<String> strings = new LinkedList<String>();
        toSeek.add(strings);
        strings.add(seekedString);
        Map<Long, Integer> positions = seekIndexFile.rawDocLocations(toSeek, seekedFile, 100, System.out); //pozice a délka
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
    public void testOldSeek() throws IOException {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        testOldSeek("00000000000000000120693868", file, hashFile);
    }

    @Test
    public void testNewSeek() throws IOException {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        //testNewSeek("000000000000120736038</id><userId>MNP_Server</use", file, hashFile);
        //testNewSeek("Default (self-tuning)',5,Pooled Threads};7aadecf70000015a13a6e6c9ffffc2ea;generated-r1-iyypepvv-1s6h;ProxyService$CrmMNPManagementSimple$2", file, hashFile);
        //testNewSeek("isdn><number>+420724582681</numb", file, hashFile);
        testNewSeek("<mnp:BlMsisdnVerified xmlns:mnp=\"http://schemas.eurotel.cz/mnp\"><id>MNP:00000000000000000120734659</id><userId>MNP_Server</userId><transactionId>42</transactionId><spId>232</spId><msisdn><number>+420773499604</number></msisdn></mnp:BlMsisdnVerified>", file, hashFile);
        //testNewSeek("<mnp:BlMsisdnVerixied xmlns:mnp=\"http://scemas.eurotel.cz/mnp\"><id>MNP:000000000000000002073465</id><userd>MNP_Server</userId><transactionId>42</transactionId><spId>232</spId><msisdn><number>+420773499604</number></msisdn></mnp:BlMsisdnVerified>", file, hashFile);
    }

    public void testNewSeek(String seekString, File seekedFile, File hashFile) throws IOException {
        SeekIndexFile seekIndexFile = new SeekIndexFile(hashFile, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer", 100);
        List<DataDocument> documents = seekIndexFile.seek(seekString); //pozice a délka
    }

    @Test
    public void testJSON() throws IOException {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        SeekIndexFile seekIndexFile = new SeekIndexFile(file, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer", 100);
        String seekString = "<mnp:BlMsisdnVefie xmlns:mnp=\"http://schemas.eurotel.cz/mnp\"><id>MNP:0000000000000000120734659</id><userId>MNP_Server</userId><transactionId>42</transactionId><spId>232</spId><msisdn><number>+42077349604</number></msisdn></mnp:BlMsisdnVerified>";

        seekIndexFile.seek(seekString);
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
