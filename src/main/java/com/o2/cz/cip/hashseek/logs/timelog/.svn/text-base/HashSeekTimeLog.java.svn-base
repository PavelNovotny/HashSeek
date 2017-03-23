package com.o2.cz.cip.hashseek.logs.timelog;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.*;
import com.o2.cz.cip.hashseek.logs.AbstractLogRecord;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;
import com.o2.cz.cip.hashseek.logs.evaluate.FileEvaluatorUtil;
import com.o2.cz.cip.hashseek.util.CloseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashSeekTimeLog extends AbstractLogSeek {

    private static final int BYTES_TO_READ_BEAID = 400;
    static final Logger LOGGER= LoggerFactory.getLogger(HashSeekTimeLog.class);
    
    public HashSeekTimeLog() {
        output = System.out;
    }

    public HashSeekTimeLog(PrintStream output) {
        beaIdPattern = Pattern.compile("^\\[([^;]+;){14}[^;]+(;[0-9a-f\\-\\.]{32,50};)"); //bea id na pozici 15;; pro time logy
        dateStartLinePattern = Pattern.compile("^\\[\\d{8} \\d{2}:\\d{2}:\\d{2}", Pattern.MULTILINE);
        this.output = output;
    }

    @Override
    public boolean canBeProcessed(String name, AppArguments appArguments) {
        Pattern pattern = Pattern.compile("^(other|jms)_s._alsb_aspect.time\\.(\\d{8}|part)(.bgz)?$");
        Matcher matcher = pattern.matcher(name);
        if (appArguments.getDatesToScan().isEmpty()) {
            return false;
        } else if (matcher.matches()) {
            Calendar current = Calendar.getInstance();
            current.setTimeInMillis(System.currentTimeMillis());
            if (appArguments.getDatesToScan().contains(HashSeekConstants.dateString(current)) && name.endsWith("part")) {
                return true;
            }
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
    public void addCorrelatingBeaId(SeekableInputStream raf, long position) throws IOException {
        raf.seek(position);
        if (nearDateTimeBackward(raf, "", 0)) {
            String record = raf.readRaw(BYTES_TO_READ_BEAID);
            Matcher matcher = beaIdPattern.matcher(record);
            if (matcher.find()) {
                String beaId = matcher.group(2);
                addBeaId(beaId.substring(1, beaId.length() - 1), raf);
            } else {
                HashSeekConstants.outPrintLine(output, String.format("no beaId found before position '%s'", position));
            }
        }
    }

    public void seek(String[] seekStrings, AppArguments appArguments) throws IOException, ClassNotFoundException {
        if (appArguments.isIncludeTimeLogs()){
            Set<File> filesToProcess = FileEvaluatorUtil.filesToSeek( appArguments, this);
            Set<String> missedLastFiles = new HashSet<String>();
            Set<String> missedFiles = FileEvaluatorUtil.missedFiles(appArguments, filesToProcess, "", "time",this);
            reportSeekedFiles(filesToProcess);
            reportMissedFiles(missedFiles);
            localSeek(seekStrings, filesToProcess, missedLastFiles);
        }
    }


    public void seekHashOnly(String[] seekStrings, AppArguments appArguments) throws IOException, ClassNotFoundException {
        AppProperties appProperties = new AppProperties();
        Set<File> filesToProcess = appProperties.hashFilesToSeek(appArguments);
        Set<String> missedFiles = FileEvaluatorUtil.missedFiles(appArguments, filesToProcess, HashSeekConstants.HASH_FILE_SUFFIX, "time",this);
        reportSeekedFiles(filesToProcess);
        reportMissedFiles(missedFiles);
        localSeekHashOnly(seekStrings, filesToProcess, missedFiles);
    }

    protected void readLogRecord(SeekableInputStream raf, String beaId, long pointer, String logFileName) throws IOException {
        String line;
        String wholeRecord="";
        String recordTimestamp="20400101;00:00:00";
        AbstractLogRecord logRecord = createInstanceOfLogRecord();
        logRecord.setBeaId(beaId);
        logRecord.setFilePosition(pointer);
        if ((line = raf.readRecord()) != null) { //prvni radek
            Matcher matcher = dateStartLinePattern.matcher(line);
            if (matcher.find()) {
                recordTimestamp = matcher.group();
            }
            wholeRecord = line.concat("\n");
        }
        logRecord.setTimeStamp(recordTimestamp); //i pro default, pokud je chyba
        while ((line = raf.readRecord()) != null) {
            Matcher matcher = dateStartLinePattern.matcher(line);
            if (matcher.find()) { //dalsi datum, koncime
                break;
            }
            wholeRecord = wholeRecord.concat(line).concat("\n");
        }
        logRecord.setRawData(wholeRecord);
        logRecord.setLogFile(raf.getFile());
        logRecord.setMarkerPrefix(logFileName);
        results.add(logRecord);
    }

    @Override
    public void reportSortedResults(File report,AppArguments appArguments) throws FileNotFoundException { //appends to the end
        if(results!=null && results.size()>0 && results.size()<1000){
            BufferedReader bufferedReader=null;
            PrintStream out=null;
            File reportWithTimelog = new File(report.getParentFile(), report.getName() + ".time");
            try {

                bufferedReader = new BufferedReader(new FileReader(report));
                out = new PrintStream(reportWithTimelog);
                Map<String,AbstractLogRecord> helpMap=new HashMap<String, AbstractLogRecord>();
                for (Iterator<AbstractLogRecord> iterator = results.iterator(); iterator.hasNext(); ) {
                    AbstractLogRecord next =  iterator.next();
                    helpMap.put("TIME|"+next.getBeaId(),next);
                }
                String line=bufferedReader.readLine();
                while(line!=null){
                    if(line.startsWith("TIME|")){
                       AbstractLogRecord logRecord=helpMap.get(line);
                       if(logRecord!=null){
                           results.remove(helpMap.get(logRecord));
                           logRecord.reportData(out);
                       }
                    }
                    if("REMAINING_TIMES".equals(line)){
                        LogRecordTimeLog.sortBy = LogRecordTimeLog.SortBy.BEAID_LINE;
                        List<AbstractLogRecord> sortedResults = new ArrayList<AbstractLogRecord>(results);
                        Collections.sort(sortedResults);
                        String previousBeaId = null;
                        String beaIdEnterTime = "unknown";
                        for (AbstractLogRecord logRecord : sortedResults) {
                            if (!logRecord.getBeaId().equals(previousBeaId)) {
                                previousBeaId = logRecord.getBeaId();
                                beaIdEnterTime = logRecord.getTimeStamp();
                                logRecord.setFirstInBeaId(true);
                            }
                            logRecord.setBeaIdEnterTime(beaIdEnterTime);
                        }
                        LogRecordTimeLog.sortBy = LogRecordTimeLog.SortBy.ENTRYTIME_BEAID_LINE;
                        Collections.sort(sortedResults);
                        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
                        out.printf("%s\n", "");
                        for (AbstractLogRecord logRecord : sortedResults) {
                            logRecord.reportData(out);
                        }
                    }
                    out.println(line);
                    line=bufferedReader.readLine();
                }

            } catch (Throwable t) {
                LOGGER.error("reportSortedResults",t);
            } finally {
                CloseUtil.close(out);
                CloseUtil.close(bufferedReader);
                report.delete();
                boolean isRenamed= reportWithTimelog.renameTo(report);
                LOGGER.info("isRenamed:" + isRenamed);
            }
        }
    }

    public AbstractLogRecord createInstanceOfLogRecord(){
        return new LogRecordTimeLog();
    }

    @Override
    public void initBeforeSeek(List<AbstractLogSeek> finishedSeekList) {
        HashSeekConstants.outPrintLine(output, "BeaIds taken for time logs from audit log search:");
        for (int i = 0; i < finishedSeekList.size(); i++) {
            AbstractLogSeek abstractLogSeek =  finishedSeekList.get(i);
            for (File auditFile : abstractLogSeek.getBeaIds().keySet()) {
                Set<String> beaIds = abstractLogSeek.getBeaIds().get(auditFile);
                File timeFile = timeLogSibling(auditFile);
                for (String beaId : beaIds) {
                    HashSeekConstants.outPrintLine(output, String.format("'%s' -> '%s' = '%s'", auditFile.getName(), timeFile.getName(), beaId));
                    addBeaId(beaId, timeFile);
                }
            }
        }

    }

    private File timeLogSibling(File auditFile) {
        String auditHashFileName = auditFile.getName();
        String timeLogSiblingName = auditHashFileName.replaceFirst("\\.\\d{2}$", ".part");
        timeLogSiblingName = timeLogSiblingName.replaceFirst("audit", "time");
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());
        String currentDate = HashSeekConstants.dateString(current);
        timeLogSiblingName = timeLogSiblingName.replaceFirst("\\." + currentDate, "");
        File timeLogSibling = new File(auditFile.getParent(), timeLogSiblingName);
        return timeLogSibling;
    }
}
