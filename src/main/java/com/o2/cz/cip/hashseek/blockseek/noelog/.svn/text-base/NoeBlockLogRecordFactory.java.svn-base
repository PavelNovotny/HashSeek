package com.o2.cz.cip.hashseek.blockseek.noelog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class NoeBlockLogRecordFactory implements BlockRecordFactory {

    @Override
    public AbstractBlockRecord getBlockRecordInstance(String rawData, File logFile, long filePosition) {
        return new NoeBlockLogRecord(rawData, logFile, filePosition);
    }
}
