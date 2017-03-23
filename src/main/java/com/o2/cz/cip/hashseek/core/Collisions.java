package com.o2.cz.cip.hashseek.core;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * User: Pavel
 * Date: 22.3.13 17:59
 */
public class Collisions {
    private int index = 0;
    HashTableItem[] arrayOfLongs = new HashTableItem[HashSeekConstants.MAX_COLLISIONS];

    public Collisions() {
        for (int i=0; i< HashSeekConstants.MAX_COLLISIONS; i++) {
            arrayOfLongs[i] = new HashTableItem();
        }
    }

    public void add(long pointer, long djb2Hash) {
        for (int i=0; i< index; i++) {
            if (arrayOfLongs[i].getDjb2Hash()== djb2Hash) {
                arrayOfLongs[i].add(pointer);
                return; // jinak pokud nenalezeno, pridame na konec indexu jako novou kolizi
            }
        }
        if (index < HashSeekConstants.MAX_COLLISIONS) { //nova kolize
            arrayOfLongs[index].add(pointer);
            arrayOfLongs[index++].setDjb2Hash(djb2Hash);
        } else {
            HashSeekConstants.outPrintLine(String.format("maximum number of '%s' collision reached! Index may be incomplete.", HashSeekConstants.MAX_COLLISIONS));
        }
    }

    public int size() {
        return index;
    }

    public boolean ifFull() {
     return index >= HashSeekConstants.MAX_COLLISIONS;
    }

    public void writeCache(int hash, DataOutputStream os) throws IOException {
        if (index >0) {
            os.writeInt(hash);
            os.writeInt(index);
            for (int i=0; i< index; i++) {
                arrayOfLongs[i].writeCache(os);
            }
        }
    }

    public int writeHash(int hash, DataOutputStream os) throws IOException {
        int pointersSize = pointersSize();
        if (pointersSize > 0) {
            os.writeInt(hash);
            os.writeInt(pointersSize);
            for (int i=0; i< index; i++) {
                arrayOfLongs[i].writePointers(os);
            }
            return ((pointersSize*Long.SIZE) + (Integer.SIZE*2))  / Byte.SIZE;
        }
        return 0;
    }

    public int pointersSize() {
        int pointersSize = 0;
        for (int i=0; i< index; i++) {
            pointersSize += arrayOfLongs[i].size();
        }
        return pointersSize;
    }


    public void reset() {
        index = 0;
        for (int i=0; i< HashSeekConstants.MAX_COLLISIONS; i++) {
            arrayOfLongs[i].reset();
        }
    }
}
