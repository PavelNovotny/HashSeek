package com.o2.cz.cip.hashseek.util;

import com.o2.cz.cip.hashseek.core.HashIndexer;

import java.io.*;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class Utils {


    public static int normalizeToHashSpace(int javaHash, int hashSpace) {
        return javaHash % hashSpace;
    }

    public static int maskSign(int javaHash) {
        return (javaHash & 0x7fffffff);
    }

    public static int javaHash(String toBeHashed) throws UnsupportedEncodingException { //hashuje po bytech nikoliv po char, tj. je kompatibilni s BlockHashReader ktere je nezavisle od encoding
        byte[] bytes = toBeHashed.getBytes("UTF-8");
        return javaHash(bytes);
    }

    public static int javaHash(byte[] word) {
        int hash = 0;
        for (int i=0; i< word.length; i++) {
            hash = 31* hash + word[i];
        }
        return hash;
    }

    public static long makeLongFromTwoInts(int intOne, int intTwo) {
        return (long)intOne << 32 | intTwo & 0xFFFFFFFFL;
    }

    //just for reference how to make two ints from long, the method cannot be used
    public static void makeTwoIntsFromLong(long source, int intOne, int intTwo) {
        intOne = (int) (source >>32);
        intTwo = (int) (source);
    }

    /**
     * Knuth-Morris-Pratt Algorithm for Pattern Matching
     */
    /**
     * Finds the first occurrence of the pattern in the text.
     */
    public static int indexOf(byte[] data, byte[] pattern) {
        int[] failure = computeFailure(pattern);
        int j = 0;
        if (data.length == 0) return -1;
        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) { j++; }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Knuth-Morris-Pratt Algorithm for Pattern Matching
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    private static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];
        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }
        return failure;
    }


}
