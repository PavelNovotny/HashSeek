package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.Utils;

import java.io.*;
import java.util.*;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashIndexer {

    private static final int SORT_BUFFER_SIZE_IN_BYTES = 512*1024*1024; //0,5 GB
    private static final int CUSTOM_BLOCKS_KIND = 1; //always, todo remove in next index version
    private static final int HASH_FILE_VERSION = 1;
    private static final int FILE_HASH_WITH_POINTER_BUFFER_SIZE = SORT_BUFFER_SIZE_IN_BYTES/Long.SIZE*Byte.SIZE;
    private static final int DOCPOSITIONS_ALLOCATE = 200000;
    private long[] hashWithPointerBuffer;
    private int hashWithPointerBufferPosition;
    private int fileCounter;
    private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;
    public static final int LONG_SIZE = Long.SIZE / Byte.SIZE;
    private String tempFolder = "./hash/";
    private String tempFileName = "raw.hash";
    private File resultFile;
    private File resultHashFile;
    private File docPositionFile;
    private  long[] docPositions;
    private  int docPositionsIndex;
    private  int docNumber;
    private long docEndPosition;

    public HashIndexer(File resultFile, File resultHashFile, String tempFolder, String rawFileName) {
        this.resultFile = resultFile;
        this.resultHashFile = resultHashFile;
        this.tempFolder = tempFolder;
        this.tempFileName = rawFileName;
        this.docNumber = 0;
        this.docEndPosition = 0;
        this.docPositionFile = new File(String.format("%s/%s.%s", tempFolder, tempFileName, "docpos"));
        allocateBuffers();
    }


    public void indexDocument(byte[] originalDoc, byte[][] analyzedDoc) throws IOException {
        for (int i=0; i<analyzedDoc.length; i++) {
            byte[] word = analyzedDoc[i];
            int javaHash = Utils.javaHash(word);
            writeToRawHashFile(word.length, javaHash, docNumber);
        }
        docNumber++;
        putDocLocation(originalDoc.length);
        writeToResultFile(originalDoc);
    }

    private void putDocLocation(int documentLen) throws IOException {
        docPositions[docPositionsIndex] = docEndPosition;
        docEndPosition = docEndPosition + documentLen;
        docPositionsIndex++;
        if (docPositionsIndex >= docPositions.length) {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(docPositionFile, true)));
            for (int i=0; i< docPositions.length; i++) {
                out.writeLong(docPositions[i]);
            }
            out.flush();
            out.close();
            docPositionsIndex = 0;
        }
    }

    private long[] getDocLocations() throws IOException {
        int storedNumberOfDocs = (int)(docPositionFile.length() / LONG_SIZE);
        int totalNumberOfDocs = storedNumberOfDocs + docPositionsIndex + 1;
        long[] docAddressesArray = new long[totalNumberOfDocs];
        int index = 0;
        if (docPositionFile.exists()) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(docPositionFile)));
            for (int i=0; i<storedNumberOfDocs; i++) {
                long customBlockAddress = in.readLong();
                docAddressesArray[index++] = customBlockAddress;
            }
            in.close();
        }
        for (int i=0; i<docPositionsIndex; i++) {
            docAddressesArray[index++] = docPositions[i];
        }
        docAddressesArray[index++] = docEndPosition;
        return docAddressesArray;
    }


    public void finalizeIndex() throws IOException {
        long[] docsLoc = getDocLocations();
        writeSortedHashPart(); //posledni nemusí být úplně plný
        freeFileBuffer();
        File integerSpaceFile = new File(String.format("%s/%s.sorted", tempFolder, tempFileName));
        int newSpaceSize = mergeSortedFiles(integerSpaceFile);
        System.out.println(String.format("hash space size %s", newSpaceSize));
        normalizeHashSpace(newSpaceSize, integerSpaceFile);
        writeFinalHashFile(resultHashFile, docsLoc, newSpaceSize);
        cleanupTemp();
    }

    private void cleanupTemp() {
        for (int i=0; i<fileCounter; i++) {
            File sortedBufferFile = new File (String.format("%s/%s.%03d", tempFolder, tempFileName, i));
            if (sortedBufferFile.exists()) {
                sortedBufferFile.delete();
            }
        }
        deleteFile(docPositionFile);
        File normalizedSpaceFile = new File(String.format("%s/%s.%s", tempFolder, tempFileName, "normalized"));
        deleteFile(normalizedSpaceFile);
        File integerSpaceFile = new File(String.format("%s/%s.sorted", tempFolder, tempFileName));
        deleteFile(integerSpaceFile);
    }

    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private void writeToResultFile(byte[] doc) throws IOException {
        if (resultFile == null) return;
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(resultFile, true)));
        out.write(doc);
        out.flush();
        out.close();
    }

    private void resetFileCounter() {
        this.fileCounter = 0;
    }

    private void allocateBuffers() {
        this.hashWithPointerBuffer = new long[FILE_HASH_WITH_POINTER_BUFFER_SIZE]; //alokuje maximalne SORT_BUFFER_SIZE_IN_BYTES
        this.docPositions = new long[DOCPOSITIONS_ALLOCATE];
        docPositionsIndex = 0;
        resetFileBuffer();
    }

    private void freeFileBuffer() {
        this.hashWithPointerBuffer = null;
    }

    private void resetFileBuffer() {
        this.hashWithPointerBufferPosition = 0;
    }

    private int mergeSortedFiles(File destinationFile) throws IOException { //vrací velikost hashSpace
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destinationFile, false)));
        int mergeBufferSize = (FILE_HASH_WITH_POINTER_BUFFER_SIZE / fileCounter); //aby se vyuzila alokovana pamet, ale ne vice.
        PriorityQueue<MergeFileBuffer> mergeQueue = new PriorityQueue<MergeFileBuffer>(fileCounter, new Comparator<MergeFileBuffer>() {
            @Override
            public int compare(MergeFileBuffer i, MergeFileBuffer j) {
                return (i.peek() < j.peek()?-1:1);
            }
        });
        for (int i=0; i<fileCounter; i++) {
            File sortedBufferFile = new File (String.format("%s/%s.%03d", tempFolder, tempFileName, i));
            MergeFileBuffer mergeFileBuffer = new MergeFileBuffer(mergeBufferSize, sortedBufferFile);
            mergeQueue.add(mergeFileBuffer);
        }
        long alreadyWrittenValue = 0;
        int previousHash = 0;
        int hashSpaceSize = 0;
        while (!mergeQueue.isEmpty()) {
            long value = mergeQueue.peek().poll();
            MergeFileBuffer polled = mergeQueue.poll();//vybereme buffer z queue a pokud již je jeho end, tak ho nepotřebujeme, jinak ho zase vložíme aby se PriorityQueue přepočítala.
            if (value != Long.MIN_VALUE) {
                if (value != alreadyWrittenValue) { //pouze distinct hodnoty
                    int hash = (int) (value >>32);
                    if (hash !=previousHash) { //zjistíme počet unikátních hashů pro účely normalizace velikosti hash tabulky
                        previousHash = hash;
                        hashSpaceSize++;
                    }
                    out.writeLong(value);
                    alreadyWrittenValue = value;
                }
                mergeQueue.add(polled);
            }
        }
        out.close();
        return hashSpaceSize;
    }

    private int normalizeHashSpace(int newSpaceSize, File oldSpaceFile) throws IOException { //účelem je přepočítat hashe na novou velikost a setřídit. Ve výsledku dostaneme k sobě hashe, které by byly na n-té pozici původní hash tabulky, kde n je nová velikost hash tabulky, tím pádem nepřijdeme o žádné pointry.
        allocateBuffers();
        resetFileCounter();
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(oldSpaceFile)));
        try {
            while (true) {
                long hashWithPointer = in.readLong();
                int hashValue = (int) (hashWithPointer >>32);
                hashValue = Utils.normalizeToHashSpace(hashValue, newSpaceSize);
                int pointerValue = (int) (hashWithPointer);
                this.hashWithPointerBuffer[hashWithPointerBufferPosition++] = Utils.makeLongFromTwoInts(hashValue, pointerValue);
                if (hashWithPointerBufferPosition >= FILE_HASH_WITH_POINTER_BUFFER_SIZE) {
                    writeSortedHashPart();
                }
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }
        writeSortedHashPart(); //zapíšeme zbytek v bufferu
        freeFileBuffer();
        File normalizedSpaceFile = new File(String.format("%s/%s.%s", tempFolder, tempFileName, "normalized"));
        int spaceSize = mergeSortedFiles(normalizedSpaceFile);
        System.out.println(String.format("required hash space size %s", newSpaceSize));
        System.out.println(String.format("final hash space size %s", spaceSize));
        return newSpaceSize;
    }

    private void writeFinalHashFile(File hashIndexFile, long[] docLocations, int hashSpace) throws IOException {
        File normalizedSpaceFile = new File(String.format("%s/%s.%s", tempFolder, tempFileName, "normalized"));
        RandomAccessFile hashRawSortedFile = new RandomAccessFile(normalizedSpaceFile,"r");
        if (hashIndexFile.exists()) {
            hashIndexFile.delete();
        }
        long[] hashSpacePointers = new long[hashSpace];
        int[] hashSpaceValuesCount = new int[hashSpace];
        DataOutputStream finalHash = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(hashIndexFile)));
        long filePointer = 0L;
        long longValue;
        int version = HASH_FILE_VERSION, hashValue, oldHashValue = 0, pointerValue; //verze hash indexu, pomocné proměnné
        List<Integer> pointers = new LinkedList<Integer>(); //list pro uložení aktuálních pointerů k hash
        //struktura souboru: verze, pozice na tabulku hashspace, velikost hashspace, případná tabulka custom bloků, pointery do hashovaného souboru, hashspace tabulka
        filePointer += writeInt(finalHash, version);
        filePointer += writeLong(finalHash, 0L); // hashSpaceTablePosition, později přepíšeme aktuální hodnotou pomocí RandomAccessFile
        filePointer += writeInt(finalHash, 0); //velikost hashSpace spočítáme na konci a pak přepíšeme
        //zapíšeme o jaký typ bloku se jedná, a velikost bloku (pro fixed) nebo počet bloků (pro custom)
        int blockSize, blockKind;
        blockKind = CUSTOM_BLOCKS_KIND;
        blockSize = docLocations.length;
        filePointer += writeInt(finalHash, blockKind);//zda se používá se customBlocks (1), nebo fixedBlocks (2)
        filePointer += writeInt(finalHash, blockSize);//velikost customBlocks, popř počet bytů na block, v případě fixedBlocks
        for (int i=0;i<docLocations.length;i++) {
            filePointer += writeLong(finalHash, docLocations[i]);
        }
        try {
            while (true) {
                longValue = hashRawSortedFile.readLong();
                hashValue = (int) (longValue >>32);
                pointerValue = (int) (longValue);
                if (oldHashValue != hashValue) {  //změna
                    if (pointers.size() > 0) {
                        hashSpacePointers[oldHashValue] = filePointer;//pozor, filePointer musíme počítat, metoda size() na streamu je jenom int, tj. max velikost hash souboru by mohla  být max. 2GB, teď je to OK.
                        hashSpaceValuesCount[oldHashValue] = pointers.size();
                        for (int pointer : pointers) {
                            filePointer += writeInt(finalHash, pointer);
                        }
                        pointers.clear();
                    }
                    oldHashValue = hashValue;
                }
                pointers.add(pointerValue);
            }
        } catch (EOFException e) {
        } finally {
            hashRawSortedFile.close();
            //zapiseme posledni zaznam
            if (pointers.size() > 0) {
                hashSpacePointers[oldHashValue] = filePointer;//pozor, filePointer musíme počítat, metoda size() na streamu je jenom int, tj. max velikost hash souboru by mohla  být max. 2GB, teď je to OK.
                hashSpaceValuesCount[oldHashValue] = pointers.size();
                for (int pointer : pointers) {
                    filePointer += writeInt(finalHash, pointer);
                }
            }
            long hashSpaceTablePosition = filePointer;
            for (int i=0; i < hashSpace; i++) {
                filePointer += writeLong(finalHash, hashSpacePointers[i]);
                filePointer += writeInt(finalHash, hashSpaceValuesCount[i]);
            }
            finalHash.close();
            java.io.RandomAccessFile finalHashRafw = new java.io.RandomAccessFile(hashIndexFile, "rw");
            finalHashRafw.seek(INT_SIZE); //pozice za version
            finalHashRafw.writeLong(hashSpaceTablePosition);
            finalHashRafw.writeInt(hashSpace);
            finalHashRafw.close();
        }
    }

    private int writeInt(DataOutputStream out, int value) throws IOException {
        out.writeInt(value);
        return INT_SIZE;
    }

    private int writeLong(DataOutputStream out, long value) throws IOException {
        out.writeLong(value);
        return LONG_SIZE;
    }

    private void writeToRawHashFile (int wordLength, int javaHash, int pointer) throws IOException {
        this.hashWithPointerBuffer[hashWithPointerBufferPosition++] = Utils.makeLongFromTwoInts(Utils.maskSign(javaHash), pointer);
        if (hashWithPointerBufferPosition >= FILE_HASH_WITH_POINTER_BUFFER_SIZE) {
            writeSortedHashPart();
        }
    }


    private void writeSortedHashPart() throws IOException {
        Arrays.sort(hashWithPointerBuffer, 0, hashWithPointerBufferPosition); //quick sort, std. implementace v jave je ok, a pro tenhle ucel se hodi.
        File hashRawFile = new File (String.format("%s/%s.%03d", tempFolder, tempFileName, fileCounter++));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(hashRawFile, false)));
        for (int i=0; i< hashWithPointerBufferPosition; i++) {
            out.writeLong(hashWithPointerBuffer[i]);
        }
        out.close();
        resetFileBuffer();
    }


}

class MergeFileBuffer { //buffer pro file merge.
    private long[] buffer;
    private int bufferSize;
    private int maxBufferSize;
    File sourceBufferFile;
    private long fileBufferPosition;
    private int bufferPosition;
    private boolean eof = false;
    private boolean theEnd = false;

    public MergeFileBuffer(int maxBufferSize, File sourceBufferFile) throws IOException {
        this.maxBufferSize = maxBufferSize;
        this.buffer = new long[maxBufferSize]; //actual buffer
        this.sourceBufferFile = sourceBufferFile; //soubor pro doplňování bufferu
        this.fileBufferPosition = 0L;
        load();
    }

    private void load() throws IOException {
        com.o2.cz.cip.hashseek.io.RandomAccessFile raf = new RandomAccessFile(sourceBufferFile, "r");
        raf.seek(fileBufferPosition);
        try {
            for (bufferSize=0; bufferSize < maxBufferSize; bufferSize++) {
                buffer[bufferSize] = raf.readLong();
            }
            fileBufferPosition = raf.getFilePointer();
        } catch (EOFException e) {
            eof = true;
        } finally {
            bufferPosition = 0;
            raf.close();
        }
    }

    public long peek() {
        if (!theEnd) {
            return buffer[bufferPosition];
        } else {
            return Long.MIN_VALUE;
        }
    }

    public long poll() throws IOException {
        if (theEnd) {
            return Long.MIN_VALUE;
        }
        long value = buffer[bufferPosition++];
        if (bufferPosition >= bufferSize) { //end of buffer
            if (!eof) {
                load();
            } else { //při nastaveném eof a celem prectenem bufferu je empty.
                theEnd = true;
            }
        }
        return value;
    }
}

