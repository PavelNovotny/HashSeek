package com.o2.cz.cip.hashseek.logs.auditlog;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.logs.AbstractLogRecord;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;
import com.o2.cz.cip.hashseek.logs.evaluate.FileEvaluatorUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashSeekAuditLog extends AbstractLogSeek{

    private Map<File, Set<String>> beaIds = new HashMap<File, Set<String>>();


    public HashSeekAuditLog(PrintStream output) {
        super(output);

    }

    public HashSeekAuditLog() {
    }

    @Override
    public boolean canBeProcessed(String name, AppArguments appArguments) {
        return false;
    }


    @Override
    public void addToMissedFiles(Set<String> missedFiles, AppArguments appArguments, String dateHour) {
    }

    public void seek(String[] seekStrings, AppArguments appArguments) throws IOException, ClassNotFoundException {
    }

    public void reportSortedResults(File report,AppArguments appArguments, PrintStream messageOutput) throws IOException {
        if (report.exists()) {
            report.delete();
        }
        PrintStream out = new PrintStream(report,"UTF-8");
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.BEAID_LINE;
        List<AbstractLogRecord> sortedResults = new ArrayList<AbstractLogRecord>(results);
        if (results.size()>0) {
            messageOutput.println("FOUND");
        }else{
            messageOutput.println("NOT found");
        }
        Collections.sort(sortedResults);
        AbstractLogRecord previousLogRecord=createInstanceOfLogRecord(); ;
        String beaIdEnterTime = "unknown";
        for (AbstractLogRecord logRecord : sortedResults) {
            if (!logRecord.getBeaId().equals(previousLogRecord.getBeaId())) {
                previousLogRecord.setLastInBeaId(true);
                beaIdEnterTime = logRecord.getTimeStamp();
                logRecord.setFirstInBeaId(true);
            }
            previousLogRecord=logRecord;
            logRecord.setBeaIdEnterTime(beaIdEnterTime);
        }
        if (previousLogRecord!=null){
            previousLogRecord.setLastInBeaId(true);
        }
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.ENTRYTIME_BEAID_LINE;
        Collections.sort(sortedResults);
        for (AbstractLogRecord logRecord : sortedResults) {
            logRecord.reportService(out);
            if (appArguments.isIncludeTimeLogs() && logRecord.isLastInBeaId()) {
                out.println("");
                out.println("TIME|"+logRecord.getBeaId());
            }
        }
        if (appArguments.isIncludeTimeLogs()){
            out.println("");
            out.println("REMAINING_TIMES");
        }
        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
        out.printf("%s\n", "");
        for (AbstractLogRecord logRecord : sortedResults) {
            logRecord.reportData(out);

        }
        out.close();
    }

    public AbstractLogRecord createInstanceOfLogRecord(){
        return new LogRecordAuditLog();
    }

}
