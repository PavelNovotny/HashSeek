package com.o2.cz.cip.hashseek.logs.auditlog;

import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.logs.timelog.LogRecordTimeLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 21.5.13 9:06
 */
public abstract class AbstractAuditSeek {
    protected Pattern beaIdPattern = Pattern.compile(";[0-9a-f\\-\\.]{32,50};");
    protected Pattern dateStartLinePattern = Pattern.compile("^\\d{8};\\d{2}:\\d{2}:\\d{2}", Pattern.MULTILINE);
    protected Set<LogRecordAuditLog> results = new HashSet<LogRecordAuditLog>();

    byte UTF8_NEXT_BYTES_2_BITS=(byte)0xc0;//11000000
    byte UTF8_NEXT_BYTES_1_BIT=(byte)0x80; //10000000

    public static final int BACKWARD_DEPTH = 4096;
    protected PrintStream output;

    public AbstractAuditSeek() {
        output = System.out;
    }

    public AbstractAuditSeek(PrintStream output) {
        this.output = output;
    }

    public Set<LogRecordAuditLog> getResults() {
        return results;
    }

    public void setResults(Set<LogRecordAuditLog> results) {
        this.results = results;
    }

    /**
     * hleda zacatek logu. Zacatek je identifikovan dateTime patternem
     * @param raf
     * @param prevChunk
     * @param counter
     * @return
     * @throws IOException
     */
    public boolean nearDateTimeBackward(SeekableInputStream raf, String prevChunk, int counter) throws IOException {
        long position = raf.getFilePointer()- BACKWARD_DEPTH;
        if (position < 0) {
            position = 0;
        }
        byte[] bytesToRead = new byte[BACKWARD_DEPTH];
        raf.seek(position);
        raf.read(bytesToRead);

        //toto je zde kvuli tomu, kdyz prvni z 3 bytu je cast UTF-8 znaku viz. http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/sun/nio/cs/UTF_8.java
        for (int i = 0; i < 3; i++) {
            if ((bytesToRead[i]&UTF8_NEXT_BYTES_2_BITS)==UTF8_NEXT_BYTES_1_BIT){
                //nahrazuju ho A
                bytesToRead[i]=(byte)65;
            } else{
                break;
            }
        }


        String line = (new String(bytesToRead,"UTF-8")).concat(prevChunk);
        Matcher matcher = dateStartLinePattern.matcher(line);
        int offset = -1;
        while (matcher.find()) {
            String dateTime = matcher.group();
            offset = matcher.start();
            offset=line.substring(0,offset).getBytes("UTF-8").length;
        }
        if (offset > -1) {
            raf.seek(position + offset);
            return true;
        } else {
            if (position == 0 || counter > 10000) { //na zacatku souboru nebo nelze dohledat v rozumne hloubce.
                HashSeekConstants.outPrintLine(output, "no date at beginning of the line was found backwards.");
                return false;
            } else {
                raf.seek(position);
                return nearDateTimeBackward(raf, line.substring(0, 20), ++counter); //prevChunk kvuli prekryvu.
            }
        }
    }

    public void reportSortedResults(File report) throws FileNotFoundException {
        if (report.exists()) {
            report.delete();
        }
        PrintStream out = new PrintStream(report);
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.BEAID_LINE;
        List<LogRecordAuditLog> sortedResults = new ArrayList<LogRecordAuditLog>(results);
        Collections.sort(sortedResults);
        String previousBeaId = null;
        String beaIdEnterTime = "unknown";
        for (LogRecordAuditLog logRecord : sortedResults) {
            if (!logRecord.getBeaId().equals(previousBeaId)) {
                previousBeaId = logRecord.getBeaId();
                beaIdEnterTime = logRecord.getTimeStamp();
                logRecord.setFirstInBeaId(true);
            }
            logRecord.setBeaIdEnterTime(beaIdEnterTime);
        }
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.ENTRYTIME_BEAID_LINE;
        Collections.sort(sortedResults);
        for (LogRecordAuditLog logRecord : sortedResults) {
            logRecord.reportService(out);
        }
        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
        out.printf("%s\n", "");
        for (LogRecordAuditLog logRecord : sortedResults) {
            logRecord.reportData(out);
        }
        out.close();
    }

    public void reportSortedResults(File report, Set<LogRecordTimeLog> logRecordTimeLogs) throws FileNotFoundException {
        if (report.exists()) {
            report.delete();
        }
        PrintStream out = new PrintStream(report);
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.BEAID_LINE;
        List<LogRecordAuditLog> sortedResults = new ArrayList<LogRecordAuditLog>(results);
        Collections.sort(sortedResults);
        LogRecordAuditLog previousLogRecord = new LogRecordAuditLog(); //empty, na zacatku mimo collection
        String beaIdEnterTime = "unknown";
        for (LogRecordAuditLog logRecord : sortedResults) {
            if (!logRecord.getBeaId().equals(previousLogRecord.getBeaId())) {
                previousLogRecord.setLastInBeaId(true); //pro prvni je mimo kolekci
                beaIdEnterTime = logRecord.getTimeStamp();
                logRecord.setFirstInBeaId(true);
            }
            previousLogRecord = logRecord;
            logRecord.setBeaIdEnterTime(beaIdEnterTime);
        }
        previousLogRecord.setLastInBeaId(true);
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.ENTRYTIME_BEAID_LINE;
        Collections.sort(sortedResults);
        for (LogRecordAuditLog logRecord : sortedResults) {
            logRecord.reportService(out);
            reportTimeLog(out, logRecord, logRecordTimeLogs);
        }
        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
        LogRecordTimeLog.sortBy = LogRecordTimeLog.SortBy.BEAID_LINE;
        List<LogRecordTimeLog> sortedTimeResults = new ArrayList<LogRecordTimeLog>(logRecordTimeLogs);
        LogRecordTimeLog previousTimeLogRecord = new LogRecordTimeLog(); //empty, na zacatku mimo collection
        beaIdEnterTime = "unknown";
        for (LogRecordTimeLog logRecord : sortedTimeResults) {
            if (!logRecord.getBeaId().equals(previousLogRecord.getBeaId())) {
                previousTimeLogRecord = logRecord;
                beaIdEnterTime = logRecord.getTimeStamp();
                logRecord.setFirstInBeaId(true);
            }
            logRecord.setBeaIdEnterTime(beaIdEnterTime);
        }
        LogRecordTimeLog.sortBy = LogRecordTimeLog.SortBy.ENTRYTIME_BEAID_LINE;
        Collections.sort(sortedTimeResults);
        for (LogRecordTimeLog timeLogRecord : sortedTimeResults) {
            timeLogRecord.reportData(out);
        }
        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
        out.printf("%s\n", "");
        for (LogRecordAuditLog logRecord : sortedResults) {
            logRecord.reportData(out);
        }
        out.close();
    }

    private void reportTimeLog(PrintStream out, LogRecordAuditLog auditLogRecord, Set<LogRecordTimeLog> logRecordTimeLogs) {
        if (auditLogRecord.isLastInBeaId()) {
           for (LogRecordTimeLog logRecordTimeLog : logRecordTimeLogs) {
               if (logRecordTimeLog.getBeaId().equals(auditLogRecord.getBeaId()) && logRecordTimeLog.getMarkerPrefix().equals(auditLogRecord.getMarkerPrefix())) {
                   logRecordTimeLog.reportData(out);
               }
           }
        }
    }

    protected void readLogRecord(SeekableInputStream raf, String beaId, long pointer, String logFileName) throws IOException {
        String line;
        String wholeRecord="";
        String recordTimestamp="20200101;00:00:00";
        LogRecordAuditLog logRecord = new LogRecordAuditLog();
        logRecord.setBeaId(beaId);
        logRecord.setFilePosition(pointer);
        if ((line = raf.readRecord()) != null) { //prvni radek
            Matcher matcher = dateStartLinePattern.matcher(line);
            if (matcher.find()) {
                recordTimestamp = matcher.group();
            }
            wholeRecord = line;
        }
        logRecord.setTimeStamp(recordTimestamp); // i pro default, pokud je chyba.
        while ((line = raf.readRecord()) != null) {
            Matcher matcher = dateStartLinePattern.matcher(line);
            if (matcher.find()) { //dalsi datum, koncime
                break;
            }
            wholeRecord = wholeRecord.concat(line);
        }
        logRecord.setRawData(wholeRecord);
        logRecord.setLogFile(raf.getFile());
        logRecord.setMarkerPrefix(logFileName);
        results.add(logRecord);
    }

}
