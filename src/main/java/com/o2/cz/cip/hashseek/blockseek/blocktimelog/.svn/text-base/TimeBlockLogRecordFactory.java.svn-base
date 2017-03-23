package com.o2.cz.cip.hashseek.blockseek.blocktimelog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditBlockLogRecord;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class TimeBlockLogRecordFactory implements BlockRecordFactory {

    @Override
    public AbstractBlockRecord getBlockRecordInstance(String rawData, File logFile, long filePosition) {
        return new TimeBlockLogRecord(rawData, logFile, filePosition);
    }
}
