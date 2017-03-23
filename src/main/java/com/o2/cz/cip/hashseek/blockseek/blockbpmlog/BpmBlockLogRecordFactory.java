package com.o2.cz.cip.hashseek.blockseek.blockbpmlog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;
import com.o2.cz.cip.hashseek.blockseek.blocktimelog.TimeBlockLogRecord;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class BpmBlockLogRecordFactory implements BlockRecordFactory {

    @Override
    public AbstractBlockRecord getBlockRecordInstance(String rawData, File logFile, long filePosition) {
        return new BpmBlockLogRecord(rawData, logFile, filePosition);
    }
}
