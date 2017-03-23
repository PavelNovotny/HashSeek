package com.o2.cz.cip.hashseek.blockseek.blockb2blog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class B2BBlockLogRecordFactory implements BlockRecordFactory {

    @Override
    public AbstractBlockRecord getBlockRecordInstance(String rawData, File logFile, long filePosition) {
        return new B2BBlockLogRecord(rawData, logFile, filePosition);
    }
}
