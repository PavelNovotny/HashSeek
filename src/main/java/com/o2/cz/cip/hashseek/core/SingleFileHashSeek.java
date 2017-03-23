package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.io.BgzSeekableInputStream;
import com.o2.cz.cip.hashseek.io.BgzUtil;
import com.o2.cz.cip.hashseek.io.RandomSeekableInputStream;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.util.CloseUtil;
import net.sf.samtools.util.BlockCompressedInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class SingleFileHashSeek {

    private File file;
    private long[][] hashTable;
    private PrintStream output;
    private boolean isProblemFile;
    private boolean foundInFile;

    public SingleFileHashSeek(File file) throws IOException {
        this(file, System.out);
    }

    public SingleFileHashSeek(File file, PrintStream output) throws IOException {
        this.file = file;
        this.output = output;
        this.isProblemFile = false;
        this.foundInFile = false;
    }

    public long[] getFilePointers(String seekedString) {
        int hash = HashSeekConstants.javaHashCodeTableIndex(seekedString);
        return hashTable[hash];
    }

    public List<Long> getNoCollisionPointers(String seekedString, SeekableInputStream raf) throws IOException {
        int hash = HashSeekConstants.javaHashCodeTableIndex(seekedString);
        this.hashTable  = readHashFile(file, hash);
        int bufferHash = hash % HashSeekConstants.BUFFER_SIZE;
        List<Long> noCollisionPointers = new ArrayList<Long>();
        long[] pointers = hashTable[bufferHash];
        if (pointers != null) {
            for (long pointer : pointers) {
                raf.seek(pointer);
                String fileWord = raf.readWord();
                if (fileWord.contains(seekedString)) {
                    noCollisionPointers.add(pointer);
                }
            }
        }
        if (noCollisionPointers.isEmpty()) {
            HashSeekConstants.outPrintLine(output, String.format("'%s' was NOT found in '%s'", seekedString, raf.getFile().getPath()));
        } else {
            HashSeekConstants.outPrintLine(output, String.format("'%s' was FOUND in '%s'", seekedString, raf.getFile().getPath()));
            foundInFile = true;
        }
        return noCollisionPointers;
    }

    public void checkPointersExists(String seekedString, File hashFile) throws IOException {
        int hash = HashSeekConstants.javaHashCodeTableIndex(seekedString);
        this.hashTable  = getHashTable(hashFile, hash);
        HashSeekConstants.outPrintLine(output, String.format("Checking hash file '%s'", hashFile.getPath()));
        if (isProblemFile()) {
            HashSeekConstants.outPrintLine(output, String.format("Problems reading '%s'.", hashFile.getPath()));
            return;
        }
        int bufferHash = hash % HashSeekConstants.BUFFER_SIZE;
        long[] pointers = hashTable[bufferHash];
        if (pointers == null || pointers.length==0) {
            HashSeekConstants.outPrintLine(output, String.format("'%s' was NOT found in '%s'", seekedString, hashFile.getPath()));
        } else {
            for (long pointer : pointers) {
                HashSeekConstants.outPrintLine(output, String.format("'%s' with hash '%s', found position '%s'", seekedString, hash, pointer));
            }
            this.foundInFile = true;
        }
    }

    private long[][] readHashFile(File originalFile, int hash) throws IOException {
        File hashFile =new File( AppProperties.getHashDir(originalFile.getParentFile()),originalFile.getName()+HashSeekConstants.HASH_FILE_SUFFIX);
        File hashFileBgz=new File(hashFile.getParentFile(),hashFile.getName()+HashSeekConstants.BGZ_FILE_SUFFIX);
        if(hashFileBgz.exists()){
            hashFile=hashFileBgz;
        }
        HashSeekConstants.outPrintLine(output, String.format("started reading hash file '%s'", hashFile.getPath()));
        long[][] hashTable = getHashTable(hashFile, hash);
        HashSeekConstants.outPrintLine(output, String.format("ended reading hash file '%s'", hashFile.getPath()));
        return hashTable;
    }

    private long[][] getHashTable(File hashFile, int hash) throws IOException {
        long[][] hashToPointer = new long[HashSeekConstants.BUFFER_SIZE][];
        SeekableInputStream raf=null;
        try {
            if(BgzUtil.isBgzFile(hashFile)){
                raf= new BgzSeekableInputStream(hashFile);
            }else{
                raf = new RandomSeekableInputStream(hashFile, "r");
            }
            long endPosition = placeToStartPosition(raf, hash);
            while (readSingleHash(raf, hashToPointer, endPosition));
            raf.close();
        } catch (Exception e) {
            isProblemFile = true;
        }
        CloseUtil.close(raf);
        return hashToPointer;
    }

    private long placeToStartPosition(SeekableInputStream raf, int hash) throws IOException {
        int hashTableSize = raf.readInt(); // HASH_TABLE_SIZE
        int bufferSize = raf.readInt(); // BUFFER_SIZE
        int bufferCount = raf.readInt();
        long startPosition = 0;
        long endPosition = 0;
        for (int i=0; i< bufferCount; i++) { //predpokladame setridene, jinak setridit
            if (hash < raf.readInt()) { // indexFrom
                break;
            }
            startPosition = raf.readLong();
            endPosition = raf.readLong();
        }
        raf.seek(startPosition);
        return endPosition;
    }

    private boolean readSingleHash(SeekableInputStream raf, long[][] hashTable, long endPosition) throws IOException {
        try {
            int hash = raf.readInt();
            int pointerSize = raf.readInt();
            long[] pointers = new long[pointerSize];
            for (int i = 0; i< pointerSize; i++) {
                long pointer = raf.readLong();
                pointers[i] = pointer;
            }
            hashTable[hash] = pointers;
            if (raf.getFilePointer() >= endPosition) {
                return false;
            }
        } catch (EOFException e) {
            return false;
        }
        return true;
    }

    public File getFile() {
        return file;
    }

    public long[][] getHashTable() {
        return hashTable;
    }

    public boolean isProblemFile() {
        return isProblemFile;
    }

    public boolean isFoundInFile() {
        return foundInFile;
    }
}
