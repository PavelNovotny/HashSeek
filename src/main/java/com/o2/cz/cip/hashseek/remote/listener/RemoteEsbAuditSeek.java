package com.o2.cz.cip.hashseek.remote.listener;

import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.logs.auditlog.AbstractAuditSeek;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;

/**
 * User: Pavel
 * Date: 21.5.13 8:46
 */
public class RemoteEsbAuditSeek extends AbstractAuditSeek {

    private Map<String, List<Long>> foundBeaIds = new HashMap<String, List<Long>>();
    private Map<String, List<Long>> allFileBeaIds = new HashMap<String, List<Long>>();
    private SeekableInputStream raf;

    byte UTF8_NEXT_BYTES_2_BITS=(byte)0xc0;//11000000
    byte UTF8_NEXT_BYTES_1_BIT=(byte)0x80; //10000000

    public RemoteEsbAuditSeek(SeekableInputStream raf, PrintStream out) {
        super(out);
        this.raf = raf;
    }

    public RemoteEsbAuditSeek(SeekableInputStream raf) {
        this.raf = raf;
    }

    private void addFoundBeaId(String beaId, long pointer) {
        List<Long> pointers = foundBeaIds.get(beaId);
        if (pointers == null) {
            pointers = new ArrayList<Long>();
            foundBeaIds.put(beaId, pointers);
        }
        pointers.add(pointer);
    }

    public void addFoundBeaId(SeekableInputStream raf, long position) throws IOException {
        raf.seek(position);
        if (nearDateTimeBackward(raf, "", 0)) {
            long dateTimePosition = raf.getFilePointer();
            String record = raf.readRecord();
            Matcher matcher = beaIdPattern.matcher(record);
            if (matcher.find()) {
                String beaId = matcher.group();
                int startPos=matcher.start()+1;
                startPos=record.substring(0,startPos).getBytes("UTF-8").length;
                addFoundBeaId(trimBeaId(beaId), dateTimePosition + matcher.start()+1);
            } else {
                HashSeekConstants.outPrintLine(output, "no beaId found before position");
            }
        }
    }

    private void readLogRecordsSeq(String beaId, String clientFileToSeek) throws IOException {
        for (Long pointer : allFileBeaIds.get(beaId)) {
            raf.seek(pointer);
            if (nearDateTimeBackward(raf, "",0)) {
                readLogRecord(raf, beaId, pointer, clientFileToSeek);
            }
        }
    }

    public void readAllLogRecordsSeq (String clientFileToSeek) throws IOException {
        if (foundBeaIds.isEmpty()) {
            HashSeekConstants.outPrintLine(output, String.format("no beaIds found in '%s'", raf.getFile().getPath()));
            return;
        }
        for (String beaId : foundBeaIds.keySet()) {
            readLogRecordsSeq(beaId, clientFileToSeek);
        }
    }

    public List<Long> getFoundPointers(Set<String> seekedStrings) throws IOException {
        List<Long> foundPointers = new ArrayList<Long>();
        String line;
        while ((line = raf.readRecord()) != null) {
            for (String seekedString: seekedStrings) {
                int offset = line.indexOf(seekedString);
                if (offset > -1) {
                    offset=line.substring(0,offset).getBytes("UTF-8").length;
                    foundPointers.add(raf.getFilePointer() - line.getBytes("UTF-8").length + offset);
                }
            }
            Matcher matcher = beaIdPattern.matcher(line);
            if (matcher.find()) {
                String beaId = matcher.group();
                int startPosition=matcher.start()+1;
                startPosition=line.substring(0,startPosition).getBytes("UTF-8").length;
                addToAllBeaIds(trimBeaId(beaId), raf.getFilePointer() - line.getBytes("UTF-8").length + startPosition);
            }
        }
        return foundPointers;
    }

    private String trimBeaId(String beaId) {
        return beaId.substring(1, beaId.length() - 1);
//        return beaId;
    }


    private void addToAllBeaIds(String beaId, long pointer) {
        List<Long> pointers = allFileBeaIds.get(beaId);
        if (pointers == null) {
            pointers = new ArrayList<Long>();
            allFileBeaIds.put(beaId, pointers);
        }
        pointers.add(pointer);
    }

    public void sequentialSeek(Set<String> stringsToSeek, String clientFileToSeek) throws IOException {
        for (Long pointer : getFoundPointers(stringsToSeek)) {
            addFoundBeaId(raf, pointer);
        }
        readAllLogRecordsSeq(clientFileToSeek);
    }

    public boolean nearDateTimeBackward(RandomAccessFile raf, String prevChunk, int counter) throws IOException {
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
        while (matcher.find()) { //posledni
            String dateTime = matcher.group();
            offset = matcher.start();
            offset=line.substring(0,offset).getBytes("UTF-8").length;
        }
        if (offset > -1) { //nalezeno
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

}
