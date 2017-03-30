package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.io.*;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.BlockSeekUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class BlockHashFileCreator {

    public static final int SORT_BUFFER_SIZE_IN_BYTES = 512*1024*1024; //0,5 GB
    public static final int CUSTOM_BLOCKS_KIND = 1;
    public static final int FIXED_BLOCKS_KIND = 2;
    public static final int HASH_FILE_VERSION = 1;
    public static final int DEFAULT_FIXED_BLOCK_SIZE = 10000;
    public static final int FILE_HASH_WITH_POINTER_BUFFER_SIZE = SORT_BUFFER_SIZE_IN_BYTES/Long.SIZE*Byte.SIZE;
    private long[] hashWithPointerBuffer;
    private int hashWithPointerBufferPosition;
    private int fileCounter;
    private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;
    private static final int LONG_SIZE = Long.SIZE / Byte.SIZE;
    private String tempPlace = "./hash/";
    private String hashRawFileName = "hashRaw.hash";
    private String blockSuffix = HashSeekConstants.BLOCKS_FILE_SUFFIX;
    private String hashSuffix = ".hash";
    private static Logger LOGGER = Logger.getLogger(BlockHashFileCreator.class);

    public void resetFileCounter() {
        this.fileCounter = 0;
    }

    public void allocateFileBuffer() {
        this.hashWithPointerBuffer = new long[FILE_HASH_WITH_POINTER_BUFFER_SIZE]; //alokuje maximalne SORT_BUFFER_SIZE_IN_BYTES
        resetFileBuffer();
    }

    public void freeFileBuffer() {
        this.hashWithPointerBuffer = null;
    }

    public void resetFileBuffer() {
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
            File sortedBufferFile = new File (String.format("%s/%s.%03d",tempPlace, hashRawFileName, i));
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

    public void createHashFile(File fileToHash, File hashIndexFile, File blockFile) throws Exception {
        LOGGER.debug(String.format("started indexing '%s'.", fileToHash.getPath()));
        BlockHashReader bhr;
        allocateFileBuffer();
        if (blockFile!=null && blockFile.exists()) {
            int numberOfBlocks = (int)(blockFile.length() / LONG_SIZE);
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(blockFile)));
            long[] customBlocks = new long[numberOfBlocks];
            int index = 0;
            try {
                while (true) {
                    long customBlockAddress = in.readLong();
                    customBlocks[index++] = customBlockAddress;
                }
            } catch (EOFException e) {
            } finally {
                in.close();
            }
            bhr = new BlockHashReader(fileToHash, 0, customBlocks);
        } else {
            bhr = new BlockHashReader(fileToHash, DEFAULT_FIXED_BLOCK_SIZE, null);
        }
        int end;
        while ((end = bhr.readWords()) > 0) {
            switch (end) {
                case BlockHashReader.END_ALL:
                    writeToRawHashFile(bhr.smallWordLength, bhr.javaHashSmall, bhr.wordPosition);
                    if (bhr.smallWordLength != bhr.bigWordLength) {
                        writeToRawHashFile(bhr.bigWordLength, bhr.javaHashBig, bhr.wordPosition);
                    }
                    bhr.javaHashBig=0;
                    bhr.bigWordLength=0;
                    break;
                case BlockHashReader.END_SMALL:
                    writeToRawHashFile(bhr.smallWordLength, bhr.javaHashSmall, bhr.wordPosition);
                    break;
                default:
                    break;
            }
        }
        bhr.close();
        writeSortedHashPart(); //posledni nemusí být úplně plný
        freeFileBuffer();
        File integerSpaceFile = new File(String.format("%s/%s.sorted",tempPlace, hashRawFileName));
        int newSpaceSize = mergeSortedFiles(integerSpaceFile);
        System.out.println(String.format("hash space size %s", newSpaceSize));
        normalizeHashSpace(newSpaceSize, integerSpaceFile);
        writeFinalHashFile(hashIndexFile, blockFile, newSpaceSize);
    }


    private int normalizeHashSpace(int newSpaceSize, File oldSpaceFile) throws IOException { //účelem je přepočítat hashe na novou velikost a setřídit. Ve výsledku dostaneme k sobě hashe, které by byly na n-té pozici původní hash tabulky, kde n je nová velikost hash tabulky, tím pádem nepřijdeme o žádné pointry.
        allocateFileBuffer();
        resetFileCounter();
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(oldSpaceFile)));
        try {
            while (true) {
                long hashWithPointer = in.readLong();
                int hashValue = (int) (hashWithPointer >>32);
                hashValue = BlockSeekUtil.normalizeToHashSpace(hashValue, newSpaceSize);
                int pointerValue = (int) (hashWithPointer);
                this.hashWithPointerBuffer[hashWithPointerBufferPosition++] = BlockSeekUtil.makeLongFromTwoInts(hashValue, pointerValue);
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
        File normalizedSpaceFile = new File(String.format("%s/%s.%s",tempPlace, hashRawFileName, ".normalized"));
        int spaceSize = mergeSortedFiles(normalizedSpaceFile);
        System.out.println(String.format("required hash space size %s", newSpaceSize));
        System.out.println(String.format("final hash space size %s", spaceSize));
        return newSpaceSize;
    }

    private void writeFinalHashFile(File hashIdexFile, File blockFile, int hashSpace) throws IOException {
        File normalizedSpaceFile = new File(String.format("%s/%s.%s",tempPlace, hashRawFileName, ".normalized"));
        RandomAccessFile hashRawSortedFile = new RandomAccessFile(normalizedSpaceFile,"r");
        if (hashIdexFile.exists()) {
            hashIdexFile.delete();
        }
        long[] hashSpacePointers = new long[hashSpace];
        int[] hashSpaceValuesCount = new int[hashSpace];
        DataOutputStream finalHash = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(hashIdexFile)));
        long filePointer = 0L;
        long longValue;
        int version = HASH_FILE_VERSION, hashValue, oldHashValue = 0, pointerValue; //verze hash indexu, pomocné proměnné
        List<Integer> pointers = new LinkedList<Integer>(); //list pro uložení aktuálních pointerů k hash
        int fixedBlockSize = DEFAULT_FIXED_BLOCK_SIZE; //velikost bloku ke kterému se vztahuje ukazatel na blok - případ fixedBlocks
        //struktura souboru: verze, pozice na tabulku hashspace, velikost hashspace, případná tabulka custom bloků, pointery do hashovaného souboru, hashspace tabulka
        filePointer += writeInt(finalHash, version);
        filePointer += writeLong(finalHash, 0L); // hashSpaceTablePosition, později přepíšeme aktuální hodnotou pomocí RandomAccessFile
        filePointer += writeInt(finalHash, 0); //velikost hashSpace spočítáme na konci a pak přepíšeme
        //zapíšeme o jaký typ bloku se jedná, a velikost bloku (pro fixed) nebo počet bloků (pro custom)
        int blockSize, blockKind;
        if (blockFile != null && blockFile.exists()) { //custom blocks
            blockKind = CUSTOM_BLOCKS_KIND;
            blockSize = (int)(blockFile.length() / LONG_SIZE);
        } else { //fixed blocks
            blockKind = FIXED_BLOCKS_KIND;
            blockSize = fixedBlockSize;
        }
        filePointer += writeInt(finalHash, blockKind);//zda se používá se customBlocks (1), nebo fixedBlocks (2)
        filePointer += writeInt(finalHash, blockSize);//velikost customBlocks, popř počet bytů na block, v případě fixedBlocks
        if (blockFile != null && blockFile.exists()) { //custom blocks
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(blockFile)));
            try {
                while (true) {
                    long customBlockAddress = in.readLong();
                    filePointer += writeLong(finalHash, customBlockAddress);
                }
            } catch (EOFException e) {
            } finally {
                in.close();
            }
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
            java.io.RandomAccessFile finalHashRafw = new java.io.RandomAccessFile(hashIdexFile, "rw");
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

    protected void writeToRawHashFile (int wordLength, int javaHash, int pointer) throws IOException {
        if (wordLength < HashSeekConstants.MIN_WORD_SIZE || wordLength > BlockSeekUtil.MAX_WORD_SIZE) {
            return;
        }
        this.hashWithPointerBuffer[hashWithPointerBufferPosition++] = BlockSeekUtil.makeLongFromTwoInts(BlockSeekUtil.maskSign(javaHash), pointer);
        if (hashWithPointerBufferPosition >= FILE_HASH_WITH_POINTER_BUFFER_SIZE) {
            writeSortedHashPart();
        }
    }

    private void writeSortedHashPart() throws IOException {
        Arrays.sort(hashWithPointerBuffer, 0, hashWithPointerBufferPosition); //quick sort, std. implementace v jave je ok, a pro tenhle ucel se hodi.
        File hashRawFile = new File (String.format("%s/%s.%03d",tempPlace, hashRawFileName, fileCounter++));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(hashRawFile, false)));
        for (int i=0; i< hashWithPointerBufferPosition; i++) {
            out.writeLong(hashWithPointerBuffer[i]);
        }
        out.close();
        resetFileBuffer();
    }

    private void createHumanReadableFile() throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("/Users/pavelnovotny/temp/hash/hashRaw.hash.sorted.readable", false));
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("/Users/pavelnovotny/temp/hash/hashRaw.hash.sorted")));
        try {
            while (true) {
                out.write(String.format("%020d\n", in.readLong()).getBytes());
            }
        } catch (EOFException e) {
        } finally {
            in.close();
            out.close();
        }
    }

    public static void main (String args[]) throws Exception {
        BlockHashFileCreator blockHashFileCreator = new BlockHashFileCreator();
        blockHashFileCreator.createHashFile(new File(args[0]), new File(args[1]), new File(args[2]));
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

