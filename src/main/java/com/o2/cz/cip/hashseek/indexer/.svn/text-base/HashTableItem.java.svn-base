package com.o2.cz.cip.hashseek.core;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * User: Pavel
 * Date: 22.3.13 17:59
 */
public class HashTableItem { // to have primitive long values
    private int index = 0;
    long[] pointers = new long[HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS];
    private long djb2Hash;

    public void add(long pointer) {
        if (index < HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS) {
            pointers[index++] = pointer;
        }
    }

    public int size() {
        return index;
    }

    public long[] getPointers() {
        return pointers;
    }

    public boolean ifFull() {
     return index >= HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS;
    }

    public long getDjb2Hash() {
        return djb2Hash;
    }

    public void setDjb2Hash(long djb2Hash) {
        this.djb2Hash = djb2Hash;
    }

    public void writeCache(DataOutputStream os) throws IOException {
        os.writeInt(index);
        os.writeLong(djb2Hash);
        writePointers(os);
    }

    public void writePointers(DataOutputStream os) throws IOException {
        for (int i=0; i< index; i++) {
            os.writeLong(pointers[i]);
        }
    }

    public void reset() {
        index = 0;
    }

}
