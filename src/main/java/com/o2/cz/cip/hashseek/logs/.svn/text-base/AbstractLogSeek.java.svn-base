package com.o2.cz.cip.hashseek.logs;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.core.SingleFileHashSeek;
import com.o2.cz.cip.hashseek.io.BgzSeekableInputStream;
import com.o2.cz.cip.hashseek.io.BgzUtil;
import com.o2.cz.cip.hashseek.io.RandomSeekableInputStream;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;
import com.o2.cz.cip.hashseek.logs.evaluate.FileEvaluator;
import com.o2.cz.cip.hashseek.logs.evaluate.FileEvaluatorUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mfrydl on 24.2.14.
 */
public abstract class AbstractLogSeek  implements FileEvaluator {
    public static final String[] TEST_ESB_Servers = {"s1", "s2"};
    public static final String[] TEST_NOE_Servers = {"s1", "s2"};
    public static final String[] TEST_BPM_Servers = {"s1", "s2"};
    public static final String[] PREDPROD_ESB_Servers = {"s1", "s2"};
    public static final String[] PREDPROD_NOE_Servers = {"s1", "s2"};
    public static final String[] PREDPROD_BPM_Servers = {"s1", "s2"};
    public static final String[] PROD_ESB_Servers = {"s1", "s2", "s3", "s4"};
    public static final String[] PROD_NOE_Servers = {"s1", "s2", "s3", "s4"};
    public static final String[] PROD_B2B_Servers = {"s1", "s2"};
    public static final String[] PREDPROD_B2B_Servers = {"s1", "s2"};
    public static final String[] TEST_B2B_Servers = {"s1", "s2"};
    public static final String[] PROD_BPM_Servers = {"s1", "s2"};
    public static final String DOMAIN_JMS = "jms";
    public static final String DOMAIN_OTHER = "other";
    public static final String DOMAIN_B2B = "b2b";
    public static final String[] ESB_Domains = {DOMAIN_OTHER, DOMAIN_JMS};
    public static final String[] B2B_Domains = {DOMAIN_B2B};
    /**
     * kolekce obsahujici unikatni beaId nalezena v danem souboru pro provedene hledani
     */
    private Map<File, Set<String>> beaIds = new HashMap<File, Set<String>>();

    protected Pattern beaIdPattern = Pattern.compile(";[0-9a-f\\-\\.]{32,50};");
    protected Pattern dateStartLinePattern = Pattern.compile("^\\d{8};\\d{2}:\\d{2}:\\d{2}", Pattern.MULTILINE);
    protected Set<AbstractLogRecord> results = new HashSet<AbstractLogRecord>();

    byte UTF8_NEXT_BYTES_2_BITS=(byte)0xc0;//11000000
    byte UTF8_NEXT_BYTES_1_BIT=(byte)0x80; //10000000

    public static final int BACKWARD_DEPTH = 4096;

    protected PrintStream output;

    private boolean found;


    public AbstractLogSeek() {
        output = System.out;
    }

    public AbstractLogSeek(PrintStream output) {
        this.output = output;
        this.found = false;
    }

    public  Pattern getBeaIdPattern() {
        return beaIdPattern;
    }

    public Set<AbstractLogRecord> getResults() {
        return results;
    }

    public void setResults(Set<AbstractLogRecord> results) {
        this.results = results;
    }

    /**
     * Tato metoda je volana pred samotnym dohledanim, umoznuje nastavit informace pro prohledavac z vystupu predchozich hledani.
     * Napriklad timelogy si nastavi nalezena beaid ziskana z hledani auditlogu
     * @param finishedSeekList
     */
    public void initBeforeSeek(List<AbstractLogSeek> finishedSeekList){

    }
    /**
     * Zjistuje k nalezene pozici, beaId (identifikator flow) a vklada ho Set pro dany soubor
     * @param raf
     * @param position
     * @throws IOException
     */
    public void addCorrelatingBeaId(SeekableInputStream raf, long position) throws IOException {
        raf.seek(position);
        if (nearDateTimeBackward(raf, "", 0)) {
            String record = raf.readRecord();
            Matcher matcher = beaIdPattern.matcher(record);
            if (matcher.find()) {
                String beaId = matcher.group();
                addBeaId(beaId.substring(1, beaId.length() - 1), raf);
            } else {
                HashSeekConstants.outPrintLine(output, String.format("no beaId found before position '%s'", position));
            }
        }
    }

    /**
     * vklada beaId do seznamu nalezenych beaId pro dany soubor
     * @param beaId
     * @param file
     */
    public void addBeaId(String beaId, File file) {
        Set<String> fileBeaIds = beaIds.get(file);
        if (fileBeaIds == null) {
            fileBeaIds = new HashSet<String>();
            beaIds.put(file, fileBeaIds);
        }
        fileBeaIds.add(beaId);
    }

    protected void addBeaId(String beaId, SeekableInputStream raf) {
        addBeaId(beaId,raf.getFile());
    }

    /**
     * Nacte cele flow pro dane beaId
     * @param beaId
     * @param singleFileHashSeek
     * @param raf
     * @throws IOException
     */
    private void readLogRecords (String beaId, SingleFileHashSeek singleFileHashSeek, SeekableInputStream raf ) throws IOException {
        for (Long pointer : singleFileHashSeek.getNoCollisionPointers(beaId, raf)) {
            raf.seek(pointer);
            if (nearDateTimeBackward(raf, "",0)) {
                readLogRecord(raf, beaId, pointer, raf.getFile().getName());
            }
        }
    }

    /**
     * Nacte kompletni flow pro drive nalezena beaID
     * @param singleFileHashSeek
     * @param raf
     * @throws IOException
     */
    public void readAllLogRecords (SingleFileHashSeek singleFileHashSeek, SeekableInputStream raf ) throws IOException {
        Set<String> fileBeaIds = beaIds.get(raf.getFile());
        if (fileBeaIds == null) {
            HashSeekConstants.outPrintLine(output, String.format("no beaIds found in '%s'", raf.getFile().getPath()));
            return;
        }
        for (String beaId : fileBeaIds) {
            readLogRecords(beaId, singleFileHashSeek, raf);
        }
    }

    /**
     * Od dane pozice se hleda misto kde zacina datum/cas resp. zacatek zaznamu
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


    /**
     * Nacita log record, resp kus textu z logu do zacatku dalsiho zaznamu
     * @param raf
     * @param beaId
     * @param pointer
     * @param logFileName
     * @throws IOException
     */
    protected void readLogRecord(SeekableInputStream raf, String beaId, long pointer, String logFileName) throws IOException {
        String line;
        String wholeRecord="";
        String recordTimestamp="20300101;00:00:00";
        AbstractLogRecord logRecord = createInstanceOfLogRecord();
        logRecord.setBeaId(beaId);
        logRecord.setFilePosition(pointer);
        if ((line = raf.readRecord()) != null) { //prvni radek
            Matcher matcher = dateStartLinePattern.matcher(line);
            if (matcher.find()) {
                recordTimestamp = matcher.group();
            }else{
                int a =1;
            }
            wholeRecord = line;
        }
        logRecord.setTimeStamp(recordTimestamp); //i pro default, pokud je chyba
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

    protected void localSeek(String[] seekStrings, Set<File> filesToProcess, Set<String> missedFiles) throws IOException, ClassNotFoundException {
        for (File seekFile : filesToProcess) {
            seek(seekStrings, seekFile, missedFiles); //upravi i missedLastFiles pokud se nezdari lokalni hledani
        }
    }

    abstract public void seek(String[] seekStrings, AppArguments appArguments) throws IOException, ClassNotFoundException;
    public void seek(String[] seekStrings, File file, Set<String> missedFiles) throws IOException, ClassNotFoundException {
        SingleFileHashSeek singleFileHashSeek = new SingleFileHashSeek(file, output);
        SeekableInputStream raf = null;
        if(BgzUtil.isBgzFile(file)){
            raf=new BgzSeekableInputStream(file);
        }else{
            raf=new RandomSeekableInputStream(file,"r",BACKWARD_DEPTH);
        }

        for (String seekString : seekStrings) {
            for (Long pointer : singleFileHashSeek.getNoCollisionPointers(seekString, raf)) {
                addCorrelatingBeaId(raf, pointer);
            }
        }
        readAllLogRecords(singleFileHashSeek, raf);
        raf.close();
        if (singleFileHashSeek.isProblemFile()) {
            missedFiles.add(file.getName());
        }
        this.found = this.found || singleFileHashSeek.isFoundInFile();
    }


    public void seekHashOnly(String[] seekStrings, File file, Set<String> missedFiles) throws IOException, ClassNotFoundException {
        SingleFileHashSeek singleFileHashSeek = new SingleFileHashSeek(file, output);
        for (String seekString : seekStrings) {
            singleFileHashSeek.checkPointersExists(seekString, file);
        }
        this.found = this.found || singleFileHashSeek.isFoundInFile();
    }

    protected void reportSeekedFiles(Set<File> filesToProcess) {
        HashSeekConstants.outPrintLine(output, String.format("Budou se prohledavat lokalni soubory:"));
        for (File seekFile : filesToProcess) {
            HashSeekConstants.outPrintLine(output, String.format("'%s'",seekFile.getPath()));
        }
    }

    public void seekHashOnly(String[] seekStrings, AppArguments appArguments) throws IOException, ClassNotFoundException {
        Set<File> filesToProcess = AppProperties.hashFilesToSeek(appArguments);
        Set<String> missedFiles = FileEvaluatorUtil.missedFiles(appArguments, filesToProcess, HashSeekConstants.HASH_FILE_SUFFIX, "audit",this);
        reportSeekedFiles(filesToProcess);
        reportMissedFiles(missedFiles);
        localSeekHashOnly(seekStrings, filesToProcess, missedFiles);
    }

    protected void reportMissedFiles(Set<String> missedFiles) {
        HashSeekConstants.outPrintLine(output, String.format("Soubory ktere chybi:"));
        for (String missedFile : missedFiles) {
            HashSeekConstants.outPrintLine(output, String.format("'%s'", missedFile));
        }
    }

    protected void localSeekHashOnly(String[] seekStrings, Set<File> filesToProcess, Set<String> missedFiles) throws IOException, ClassNotFoundException {
        for (File seekFile : filesToProcess) {
            seekHashOnly(seekStrings, seekFile, missedFiles);
        }
    }


    public void reportSortedResults(File report,AppArguments appArguments) throws IOException {
        PrintStream out = new PrintStream(report,"UTF-8");
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.BEAID_LINE;
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
        LogRecordAuditLog.sortBy = LogRecordAuditLog.SortBy.ENTRYTIME_BEAID_LINE;
        Collections.sort(sortedResults);
        for (AbstractLogRecord logRecord : sortedResults) {
            logRecord.reportService(out);
        }
        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
        out.printf("%s\n", "");
        for (AbstractLogRecord logRecord : sortedResults) {
            logRecord.reportData(out);
        }
        out.close();
    }
    public boolean isFound() {
        return found;
    }

    public Map<File, Set<String>> getBeaIds() {
        return beaIds;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public abstract AbstractLogRecord createInstanceOfLogRecord();

    @Override
    public String[] getServers(AppArguments appArguments) {
        if (appArguments.isSeekProd()) {
            return PROD_ESB_Servers;
        } else {
            if(appArguments.isSeekPredprod()){
                return PREDPROD_ESB_Servers;
            }else{
                return TEST_ESB_Servers;
            }
        }
    }

    @Override
    public String[] getDomains(AppArguments appArguments) {
        return ESB_Domains;
    }
}

