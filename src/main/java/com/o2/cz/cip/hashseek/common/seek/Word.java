package com.o2.cz.cip.hashseek.common.seek;

import java.util.Comparator;

/**
 * Created by pavelnovotny on 19.05.17.
 */
public class Word {
    public int docCount;
    public long docsOffset;

    public static Comparator<Word> sizeComparator() {
        return new Comparator<Word>() {
            @Override
            public int compare(Word o1, Word o2) {
                return o1.docCount - o2.docCount;
            }
        };
    }


}
