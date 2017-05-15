package com.o2.cz.cip.hashseek.common.indexer;

import com.o2.cz.cip.hashseek.common.analyze.Analyzer;
import com.o2.cz.cip.hashseek.common.analyze.AnalyzerFactory;
import com.o2.cz.cip.hashseek.common.datastore.InsertData;
import com.o2.cz.cip.hashseek.common.datastore.InsertDataFactory;
import com.o2.cz.cip.hashseek.common.indexer.core.HashIndexer;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * Created by pavelnovotny on 02.05.17.
 */
public class BlockFileIndexer {

    private static Logger LOGGER = Logger.getLogger(BlockFileIndexer.class);

    private File sourceFile;
    private File docAddresses;
    private File resultHashFile;

    public BlockFileIndexer(File sourceFile) {
        this.sourceFile = sourceFile;
        String sourceFileName = sourceFile.getAbsolutePath();
        //todo cleanup blocks file when finished
        this.docAddresses = new File(sourceFileName+".blocks");
        this.resultHashFile = new File(sourceFileName+".hash") ;
    }

    public void index(String analyzerKind, String dataStoreKind) throws Exception {
        HashIndexer hashCreator = new HashIndexer(resultHashFile, "./hash", "hashRaw.hash");
        LOGGER.debug(String.format("started indexing '%s'.", sourceFile.getPath()));
        if (docAddresses == null || !docAddresses.exists()) {
            LOGGER.error(String.format("block file '%s' must exists. file '%s' was not indexed. .", docAddresses.getPath(), sourceFile.getPath()));
            return;
        }
        long[] docsLoc = documentAddressArray(docAddresses);
        //todo analyzer zpropagovat až do indexu. V nové verzi indexu
        Analyzer analyzer = AnalyzerFactory.createInstance(analyzerKind);
        //todo dataStore zpropagovat až do indexu. V nové verzi indexu
        InsertData dataInsert = InsertDataFactory.createInstance(dataStoreKind);
        dataInsert.setSourceFile(this.sourceFile);
        for (int docIndex=0; docIndex<docsLoc.length-1; docIndex++) {
            long start = docsLoc[docIndex];
            long end = docsLoc[docIndex+1];
            byte[] doc = getDocument(sourceFile, start, end);
            hashCreator.indexDocument(doc, analyzer, dataInsert);
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
        blockFileIndexer.index(args[1], args[2]);
    }

    public static byte[] getDocument(File file, long start, long end) throws IOException {
        if (start > end) {
            throw new IOException("Wrong document offset");
        }
        com.o2.cz.cip.hashseek.common.io.RandomAccessFile raf = new com.o2.cz.cip.hashseek.common.io.RandomAccessFile(file, "r");
        int size = (int) (end - start);
        raf.seek(start);
        byte[] bytes = new byte[size];
        int actualSize = raf.read(bytes);
        if (actualSize != size) {
            throw new IOException("Something wrong with document size");
        }
        return bytes;
    }

    public static String getDocumentString(File file, long start, long end) throws IOException {
        return new String(getDocument(file, start, end));
    }

}
