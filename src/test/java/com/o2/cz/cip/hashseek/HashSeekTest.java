package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.core.Analyzer;
import com.o2.cz.cip.hashseek.core.BlockHashFileCreator;
import com.o2.cz.cip.hashseek.core.DocumentReader;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.BlockHashReader;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;


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
        BlockHashFileCreator hashCreator = new BlockHashFileCreator();
        hashCreator.createHashFile(file, hashFile, blockFile);
        HashSeekConstants.outPrintLine("ended testCreateIndex");
    }

    @Test
    public void testDocumentReader() throws Exception {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File blockFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        HashSeekConstants.outPrintLine("started testDocumentReader");
        BlockHashFileCreator hashCreator = new BlockHashFileCreator();
        long[] documentAddresses = hashCreator.documentAddressArray(blockFile);
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
        BlockHashFileCreator hashCreator = new BlockHashFileCreator();
        long[] docsLoc = hashCreator.documentAddressArray(blockFile);
        long[] documentAddresses = hashCreator.documentAddressArray(blockFile);
        for (int docIndex=0; docIndex<documentAddresses.length-1; docIndex++) {
            long start = docsLoc[docIndex];
            long end = docsLoc[docIndex+1];
            byte[] doc = DocumentReader.getDocument(file, start, end);
            Analyzer analyzer = new Analyzer(doc);
            byte[][] words = analyzer.analyze();
            for (int i=0; i<words.length;i++) {
                out.println(new String(words[i]));
            }
        }
    }

    @Test
    public void testBlockHashReader() throws Exception {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        File blockFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.blocks");
        //File blockFile = null;
        PrintStream out = new PrintStream(new FileOutputStream("puvodni.txt"));
        BlockHashFileCreator hashCreator = new BlockHashFileCreator();
        long[] documentAddresses = hashCreator.documentAddressArray(blockFile);
        BlockHashReader hashReader = new BlockHashReader(file, 0, documentAddresses);
        int end = 0;
        while ((end = hashReader.readWords()) > 0) {
            switch (end) {
                case BlockHashReader.END_ALL:
                    if (hashReader.smallWordLength > 0) {
                        String word = new String(Arrays.copyOf(hashReader.smallWord, hashReader.smallWordLength));
                        out.println(word);
                    }
                    if (hashReader.smallWordLength != hashReader.bigWordLength) {
                        if (hashReader.bigWordLength > 0) {
                            String word = new String(Arrays.copyOf(hashReader.bigWord, hashReader.bigWordLength));
                            out.println(word);
                        }
                    }
                    hashReader.bigWordLength=0;
                    break;
                case BlockHashReader.END_SMALL:
                    if (hashReader.smallWordLength > 0) {
                        String word = new String(Arrays.copyOf(hashReader.smallWord, hashReader.smallWordLength));
                        out.println(word);
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
