package com.o2.cz.cip.hashseek.common.analyze;

import com.o2.cz.cip.hashseek.common.analyze.impl.DefaultOldHashSeekAnalyzer;

/**
 * Created by pavelnovotny on 02.05.17.
 */
public class AnalyzerFactory {

    public static Analyzer createInstance(String analyzer) {
        if ("DefaultOldHashSeekAnalyzer".equals(analyzer)) {
            return new DefaultOldHashSeekAnalyzer();
        } else {
            return new DefaultOldHashSeekAnalyzer();

        }

    }
}
