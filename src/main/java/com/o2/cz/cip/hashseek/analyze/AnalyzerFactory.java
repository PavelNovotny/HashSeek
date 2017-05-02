package com.o2.cz.cip.hashseek.analyze;

import com.o2.cz.cip.hashseek.analyze.impl.DefaultOldHashSeekAnalyzer;

/**
 * Created by pavelnovotny on 02.05.17.
 */
public class AnalyzerFactory {

    public static Analyzer createAnalyzerInstance(String analyzer) {
        if ("DefaultOldHashSeekAnalyzer".equals(analyzer)) {
            return new DefaultOldHashSeekAnalyzer();
        } else {
            return new DefaultOldHashSeekAnalyzer();
        }

    }
}
