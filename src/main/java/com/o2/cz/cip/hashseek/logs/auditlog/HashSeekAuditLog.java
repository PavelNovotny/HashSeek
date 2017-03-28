package com.o2.cz.cip.hashseek.logs.auditlog;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.logs.AbstractLogRecord;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;
import com.o2.cz.cip.hashseek.logs.evaluate.FileEvaluatorUtil;
import com.o2.cz.cip.hashseek.remote.client.SingleRemoteSeek;

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
        Pattern pattern = Pattern.compile("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?(.bgz)?$");
        Matcher matcher = pattern.matcher(name);
        if (appArguments.getDatesToScan().isEmpty()) {
            return false;
        } else if (matcher.matches()) {
            return (appArguments.getDatesToScan().contains(matcher.group(2)));
        } else {
            return false;
        }
    }


    @Override
    public void addToMissedFiles(Set<String> missedFiles, AppArguments appArguments, String dateHour) {
        missedFiles.add(String.format("other_s1_alsb_aspect.audit.%s", dateHour));
        missedFiles.add(String.format("other_s2_alsb_aspect.audit.%s", dateHour));
        missedFiles.add(String.format("jms_s1_alsb_aspect.audit.%s", dateHour));
        missedFiles.add(String.format("jms_s2_alsb_aspect.audit.%s", dateHour));
        if (appArguments.isSeekProd()) {
            missedFiles.add(String.format("other_s3_alsb_aspect.audit.%s", dateHour));
            missedFiles.add(String.format("other_s4_alsb_aspect.audit.%s", dateHour));
            missedFiles.add(String.format("jms_s3_alsb_aspect.audit.%s", dateHour));
            missedFiles.add(String.format("jms_s4_alsb_aspect.audit.%s", dateHour));
        }
    }

    public void seek(String[] seekStrings, AppArguments appArguments) throws IOException, ClassNotFoundException {
        Set<File> filesToProcess = FileEvaluatorUtil.filesToSeek(appArguments,this);
        Set<String> missedLastFiles = FileEvaluatorUtil.missedLastFiles(appArguments, filesToProcess, "",this);
        Set<String> missedFiles = FileEvaluatorUtil.missedFiles(appArguments, filesToProcess, "", "audit",this);
        reportSeekedFiles(filesToProcess);
        reportMissedFiles(missedFiles);
        localSeek(seekStrings, filesToProcess, missedLastFiles);
        if (appArguments.isOnlineEsbSeek()) {
            //remoteSeek(seekStrings, missedLastFiles, appArguments);
            seqSeek(seekStrings, missedLastFiles, appArguments);
        }
    }

    private void seqSeek(String[] seekStrings, Set<String> missedFiles, AppArguments appArguments) throws ClassNotFoundException, IOException {
        HashSeekConstants.outPrintLine(output, String.format("Sekvenční hledání:"));
        //todo, tohle se vůbec nepoužívá, smazat.
        for (String fileName : missedFiles) {
            HashSeekConstants.outPrintLine(output, String.format("File : %s", fileName));
        }
        //if (found) {
        //    this.getResults().addAll(logRecords);
        //    setFound(true);
        //}
    }

    private void remoteSeek(String[] seekStrings, Set<String> missedFiles, AppArguments appArguments) throws ClassNotFoundException, IOException {
        HashSeekConstants.outPrintLine(output, String.format("Budou se prohledavat soubory na vzdalenych strojich:"));
        Set<SingleRemoteSeek> singleRemoteSeeks = new HashSet<SingleRemoteSeek>();
        for (String fileName : missedFiles) {
            singleRemoteSeeks.addAll(prepareRemoteSeeks(seekStrings, fileName, appArguments));
        }
        for (SingleRemoteSeek singleRemoteSeek : singleRemoteSeeks) {
            HashSeekConstants.outPrintLine(output, String.format("%s:%s <<<- '%s'", singleRemoteSeek.getHost(), singleRemoteSeek.getPort(), singleRemoteSeek.getRemoteMessage().getSeekParameters().getClientFileToSeek()));
        }
        for (SingleRemoteSeek singleRemoteSeek : singleRemoteSeeks) {
            Set<LogRecordAuditLog> logRecords = singleRemoteSeek.remoteSeekByClientFileName(output);
            if (logRecords != null) {
                this.getResults().addAll(logRecords);
                setFound(true);
            }
        }
    }

    private Set<SingleRemoteSeek> prepareRemoteSeeks(String[] seekStrings, String fileName, AppArguments appArguments) throws IOException, ClassNotFoundException {
        Set<SingleRemoteSeek> singleRemoteSeeks = new HashSet<SingleRemoteSeek>();
        SingleRemoteSeek singleRemoteSeek = new SingleRemoteSeek();
        if (appArguments.isSeekProd()) {
            singleRemoteSeek.initialize(seekStrings, fileName,output, AppProperties.PROD_PREFIX);
        }
        if (appArguments.isSeekPredprod()) {
            singleRemoteSeek.initialize(seekStrings, fileName, output, AppProperties.PREDPROD_PREFIX);
        }
        if (appArguments.isSeekTest()) {
            singleRemoteSeek.initialize(seekStrings, fileName, output, AppProperties.TEST_PREFIX);
        }
        if (singleRemoteSeek.isInitialized()) {
            singleRemoteSeeks.add(singleRemoteSeek);
        }
        return singleRemoteSeeks;
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
