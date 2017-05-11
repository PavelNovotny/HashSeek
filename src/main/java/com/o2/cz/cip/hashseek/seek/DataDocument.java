package com.o2.cz.cip.hashseek.seek;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pavelnovotny on 11.05.17.
 */
public class DataDocument implements Comparable {

    private byte[] document;
    private int score;

    public DataDocument(byte[] document, int score) {
        this.document = document;
        this.score = score;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof DataDocument) {
            return this.score > ((DataDocument) o).getScore()?1:0;
        }
        return 0;
    }

    public int getScore() {
        return score;
    }

    public byte[] getDocument() {
        return document;
    }


}
