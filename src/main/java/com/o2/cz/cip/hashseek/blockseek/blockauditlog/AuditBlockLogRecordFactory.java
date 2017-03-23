package com.o2.cz.cip.hashseek.blockseek.blockauditlog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class AuditBlockLogRecordFactory implements BlockRecordFactory {

    @Override
    public AbstractBlockRecord getBlockRecordInstance(String rawData, File logFile, long filePosition) {
        return new AuditBlockLogRecord(rawData, logFile, filePosition);
    }
}
