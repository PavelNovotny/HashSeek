package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.analyze.Analyzer;
import com.o2.cz.cip.hashseek.analyze.AnalyzerFactory;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * Created by pavelnovotny on 02.05.17.
 */
public class BlockFileIndexer {

    private static Logger LOGGER = Logger.getLogger(BlockFileIndexer.class);

    private File sourceFile;
    private File docAddresses;
    private File resultFile;
    private File resultHashFile;

    public BlockFileIndexer(File sourceFile) {
        this.sourceFile = sourceFile;
        String sourceFileName = sourceFile.getAbsolutePath();
        this.docAddresses = new File(sourceFileName+".blocks");
        this.resultFile = null; //pouze indexujeme, zdrojový soubor s daty již existuje, není potřeba vytvářet vedle nový.
        this.resultHashFile = new File(sourceFileName+".hash") ;
    }

    public void indexFile(String analyzerKind) throws Exception {
        HashIndexer hashCreator = new HashIndexer(resultFile, resultHashFile, "./hash", "hashRaw.hash");
        LOGGER.debug(String.format("started indexing '%s'.", sourceFile.getPath()));
        if (docAddresses == null || !docAddresses.exists()) {
            LOGGER.error(String.format("block file '%s' must exists. file '%s' was not indexed. .", docAddresses.getPath(), sourceFile.getPath()));
            return;
        }
        long[] docsLoc = documentAddressArray(docAddresses);
        //todo analyzer zpropagovat až do indexu. V nové verzi indexu
        Analyzer analyzer = AnalyzerFactory.createAnalyzerInstance(analyzerKind);
        for (int docIndex=0; docIndex<docsLoc.length-1; docIndex++) {
            long start = docsLoc[docIndex];
            long end = docsLoc[docIndex+1];
            byte[] doc = DocumentReader.getDocument(sourceFile, start, end);
            byte[][] analyzed = analyzer.analyze(doc);
            hashCreator.indexDocument(doc, analyzed);
        }
        hashCreator.finalizeIndex();
    }

    public static long[] documentAddressArray(File docAddressesFile) throws IOException {
        int numberOfDocs = (int)(docAddressesFile.length() / HashIndexer.LONG_SIZE);
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(docAddressesFile)));
        long[] docAddressesArray = new long[numberOfDocs];
        int index = 0;
        try {
            while (true) {
                long customBlockAddress = in.readLong();
                docAddressesArray[index++] = customBlockAddress;
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }
        return docAddressesArray;
    }

    public static void main (String args[]) throws Exception { //fileToHash, analyzer
        BlockFileIndexer blockFileIndexer = new BlockFileIndexer(new File(args[0]));
        blockFileIndexer.indexFile(args[1]);
    }



}
