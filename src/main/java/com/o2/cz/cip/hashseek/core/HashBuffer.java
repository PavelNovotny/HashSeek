package com.o2.cz.cip.hashseek.core;

import java.io.*;

/**
 * User: Pavel
 * Date: 15.4.13 17:31
 */
public class HashBuffer {

    private long[] pointers;
    private long[] djb2Hashes;
    private int[] bufferIndexes;
    private int index;
    private int bufferNo;
    File cacheFile;
    private int bytesWritten;

    public HashBuffer(int bufferNo) {
        pointers = new long[HashSeekConstants.BUFFER_SIZE];
        djb2Hashes = new long[HashSeekConstants.BUFFER_SIZE];
        bufferIndexes = new int[HashSeekConstants.BUFFER_SIZE];
        index=0;
        this.bufferNo = bufferNo;
        cacheFile = new File(String.format("%scache.%s", HashSeekConstants.BUFFER_LOCATION, bufferNo));
    }

    public void add(Collisions[] workingCopy, int bufferIndex, long djb2Hash, long pointer) throws IOException {
        bufferIndexes[index] = bufferIndex;
        djb2Hashes[index] = djb2Hash;
        pointers[index] = pointer;
        if (++index == HashSeekConstants.BUFFER_SIZE) {
            processBuffer(workingCopy);
        }
    }

    public void processBuffer(Collisions[] workingCopy) throws IOException {
        //cteni a zapis do souboru.
//        HashSeekConstants.outPrintLine(String.format("processing buffer %s", bufferNo));
        readCacheFileTo(workingCopy);
        mergeBuffer(workingCopy);
        writeHashCacheFrom(workingCopy);
        index=0; //reset
    }

    private void reset(Collisions[] workingCopy) {
        for (int i= 0; i< workingCopy.length; i++) {
            workingCopy[i].reset();
        }
    }

    public void readCacheFileTo(Collisions[] workingCopy) throws IOException {
        reset(workingCopy);
//        HashSeekConstants.outPrintLine(String.format("started reading hash file '%s'", cacheFile.getPath()));
        if (!cacheFile.exists()) {
            return;
        }
        DataInputStream is = new DataInputStream( new BufferedInputStream(new FileInputStream(cacheFile)));
        while (readSingleHash(is, workingCopy));
        is.close();
//        HashSeekConstants.outPrintLine(String.format("ended reading hash file '%s'", cacheFile.getPath()));
        return;
    }

    private boolean readSingleHash(DataInputStream is, Collisions[] hashTable) throws IOException {
        try {
            int hash = is.readInt();
            int collisionLen = is.readInt();
            Collisions collisions = hashTable[hash];
            for (int i=0; i< collisionLen; i++) {
                int pointerLen = is.readInt();
                long djb2Hash = is.readLong();
                for (int j=0; j < pointerLen; j++) {
                    collisions.add(is.readLong(), djb2Hash);
                }
            }
        } catch (EOFException e) {
            return false;
        }
        return true;
    }

    private void writeHashCacheFrom(Collisions[] hashTable) throws IOException {
//        HashSeekConstants.outPrintLine(String.format("started writing cache to file '%s'", cacheFile.getPath()));
        DataOutputStream os = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(cacheFile)));
        for (int hash=0; hash < hashTable.length; hash++) {
            Collisions collisions = hashTable[hash];
            collisions.writeCache(hash, os);
        }
        os.close();
//        HashSeekConstants.outPrintLine(String.format("ended writing cache to file '%s'", cacheFile.getPath()));
    }

    private void mergeBuffer(Collisions[] hashTable) throws IOException {
        for (int i = 0; i< index; i++) {
            Collisions collisions = hashTable[bufferIndexes[i]];
            collisions.add(pointers[i], djb2Hashes[i]);
        }
    }

    public int getBufferNo() {
        return bufferNo;
    }

    public void setBytesWritten(int bytesWritten) {
        this.bytesWritten = bytesWritten;
    }

    public int getBytesWritten() {
        return bytesWritten;
    }
}
