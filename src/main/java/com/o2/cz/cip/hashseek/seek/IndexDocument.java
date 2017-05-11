package com.o2.cz.cip.hashseek.seek;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pavelnovotny on 11.05.17.
 */
public class IndexDocument implements Comparable {
    private Integer indexDocNumber;
    private Set<Integer> wordHashes;

    public IndexDocument(Integer indexDocNumber) {
        this.wordHashes = new HashSet<Integer>();
        this.indexDocNumber = indexDocNumber;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof IndexDocument) {
            return this.wordHashes.size() > ((IndexDocument) o).wordHashesSize()?1:0;
        }
        return 0;
    }

    public int wordHashesSize() {
        return wordHashes.size();
    }

    public void addWordHash(Integer wordHash) {
        this.wordHashes.add(wordHash);
    }

    @Override
    public int hashCode() {
        return indexDocNumber;
    }


    public int getIndexDocNumber() {
        return indexDocNumber;
    }


}
