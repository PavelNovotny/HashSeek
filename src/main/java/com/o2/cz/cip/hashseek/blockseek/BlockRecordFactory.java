package com.o2.cz.cip.hashseek.blockseek;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public interface BlockRecordFactory {
    public AbstractBlockRecord getBlockRecordInstance(String rawData, File logFile, long filePosition);
}
