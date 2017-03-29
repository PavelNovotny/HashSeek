package com.o2.cz.cip.hashseek.remote.listener;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.io.RandomSeekableInputStream;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.logs.AbstractLogRecord;
import com.o2.cz.cip.hashseek.logs.auditlog.HashSeekAuditLog;
import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by pavelnovotny on 20.02.17.
 */
public class ListenerTest {

    public static void main(String args[]) {
        try {
            String searchedFile = "/Users/pavelnovotny/Downloads/transfer/other_s1_alsb_aspect.audit";
            Set<String> searchStrings = new HashSet<String>();
            searchStrings.add("1100778504SFA-ACCCOR-492");
            //searchStrings.add("97541095");
            SeekableInputStream raf = new RandomSeekableInputStream(new File(searchedFile), "r");
            RemoteEsbAuditSeek remoteSeek = new RemoteEsbAuditSeek(raf);
            remoteSeek.sequentialSeek(searchStrings, searchedFile);
            Set<AbstractLogRecord> remotelogRecords = new HashSet<AbstractLogRecord>();
            for (LogRecordAuditLog logRecordAuditLog : remoteSeek.getResults()) {
                System.out.println(logRecordAuditLog.getBeaId());
                System.out.println(logRecordAuditLog.getLogFile());
                System.out.println(logRecordAuditLog.getMarkerPrefix());
                System.out.println(logRecordAuditLog.getRawData());
                remotelogRecords.add(logRecordAuditLog);
            }
            HashSeekAuditLog hashSeekAuditLog = new HashSeekAuditLog();
            hashSeekAuditLog.setResults(remotelogRecords);
            AppArguments appArguments = new AppArguments();
            hashSeekAuditLog.reportSortedResults(new File("/Users/pavelnovotny/Downloads/transfer/result.txt"), appArguments);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
