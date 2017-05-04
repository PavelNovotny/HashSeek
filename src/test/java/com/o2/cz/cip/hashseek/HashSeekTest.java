package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.analyze.impl.DefaultOldHashSeekAnalyzer;
import com.o2.cz.cip.hashseek.indexer.BlockFileIndexer;
import com.o2.cz.cip.hashseek.indexer.core.HashIndexer;
import com.o2.cz.cip.hashseek.util.Utils;
import org.junit.Test;

import java.io.*;


/**
 * Created by pavelnovotny on 14.01.14.
 */
public class HashSeekTest {

    @Test
    public void testCreateIndex() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        Utils.outPrintLine("started testCreateIndex");
        BlockFileIndexer blockFileIndexer = new BlockFileIndexer(sourceFile);
        blockFileIndexer.indexFile("DefaultOldHashSeekAnalyzer");
        Utils.outPrintLine("ended testCreateIndex");
    }

    @Test
    public void testDocumentReader() throws Exception {
        File sourceFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File resultFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.result");
        File resultHashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File docAddressFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        Utils.outPrintLine("started testDocumentReader");
        HashIndexer hashCreator = new HashIndexer(resultFile, resultHashFile, "./hash", "hashRaw.hash");
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
        HashIndexer hashCreator = new HashIndexer(resultFile, resultHashFile, "./hash", "hashRaw.hash");
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
