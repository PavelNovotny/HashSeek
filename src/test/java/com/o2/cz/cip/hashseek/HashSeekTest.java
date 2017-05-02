package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.analyze.impl.DefaultOldHashSeekAnalyzer;
import com.o2.cz.cip.hashseek.core.HashIndexer;
import com.o2.cz.cip.hashseek.core.DocumentReader;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import org.junit.Test;

import java.io.*;


/**
 * Created by pavelnovotny on 14.01.14.
 */
public class HashSeekTest {

    @Test
    public void testCreateIndex() throws Exception {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File blockFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        HashSeekConstants.outPrintLine("started testCreateIndex");
        HashIndexer hashCreator = new HashIndexer();
        hashCreator.createHashFile(file, hashFile, blockFile, "DefaultOldHashSeekAnalyzer");
        HashSeekConstants.outPrintLine("ended testCreateIndex");
    }

    @Test
    public void testDocumentReader() throws Exception {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File blockFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        HashSeekConstants.outPrintLine("started testDocumentReader");
        HashIndexer hashCreator = new HashIndexer();
        long[] documentAddresses = HashIndexer.documentAddressArray(blockFile);
        for (int index=0; index<documentAddresses.length-1; index++) {
            System.out.println("----------------------------------------------");
            System.out.println(DocumentReader.getDocumentString(file, documentAddresses[index], documentAddresses[index+1]));
        }
        HashSeekConstants.outPrintLine("ended testDocumentReader");
    }

    @Test
    public void testAnalyzer() throws Exception {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File blockFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        PrintStream out = new PrintStream(new FileOutputStream("analyzer.txt"));
        HashIndexer hashCreator = new HashIndexer();
        long[] docsLoc = HashIndexer.documentAddressArray(blockFile);
        long[] documentAddresses = HashIndexer.documentAddressArray(blockFile);
        DefaultOldHashSeekAnalyzer analyzer = new DefaultOldHashSeekAnalyzer();
        for (int docIndex=0; docIndex<documentAddresses.length-1; docIndex++) {
            long start = docsLoc[docIndex];
            long end = docsLoc[docIndex+1];
            byte[] doc = DocumentReader.getDocument(file, start, end);
            byte[][] words = analyzer.analyze(doc);
            for (int i=0; i<words.length;i++) {
                out.println(new String(words[i]));
            }
        }
    }


}
