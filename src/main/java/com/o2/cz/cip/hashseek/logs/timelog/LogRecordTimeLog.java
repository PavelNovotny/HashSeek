package com.o2.cz.cip.hashseek.logs.timelog;


import com.o2.cz.cip.hashseek.logs.AbstractLogRecord;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 18.8.12 11:43
 */
public class LogRecordTimeLog extends AbstractLogRecord {
    private boolean alreadyReported = false;


    public void setMarkerPrefix(String logFileName) {
        Pattern pattern = Pattern.compile(".*_s\\d"); //other_s1, jms_s2, etc...
        Matcher matcher = pattern.matcher(logFileName);
        if (matcher.find()) {
            this.markerPrefix = matcher.group().substring(0,1) + matcher.group().substring(matcher.group().length()-1);
        } else {
            this.markerPrefix = "??";
        }
    }
    public String lineMarker() {
        return String.format("%s%012d",markerPrefix, filePosition);
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
//        Pattern pattern = Pattern.compile(";[0-9a-f\\-\\.]{32,50};"); //bea id pattern
        Matcher matcher = Pattern.compile("^\\[([^;]+;){14}[^;]+(;[0-9a-f\\-\\.]{32,50};)").matcher(rawData);
        if (matcher.find()) {
            setBeaId(matcher.group(2));
        } else {
            setBeaId("bea_id");
        }
    }
    private String extractService(String part) {
        Pattern pattern = Pattern.compile(";\\w+\\$[^;]+");
        Matcher matcher = pattern.matcher(part);
        if (matcher.find()) {
            return matcher.group().substring(1).replaceAll("\\$","/");
        }
        return "";
    }

    public void reportData(PrintStream out) {
        if (! alreadyReported) {
//        out.println(lineMarker());
            out.println(logFile.getName());
            out.println(parseRawData());
            alreadyReported = true;
        }
    }

    public void reportService(PrintStream out) {
        if (firstInBeaId) {
            out.printf("\n%s  %s %s\n", timeStamp, beaId, logFile.getName());
        }
        out.printf("%s %s   -->%s (+%d)\n", lineMarker(), prettyFormatService(), requestOrResponse(), getTimeFromBeginning());
    }
    
    protected String prettyFormatService() {
        String service = extractService(rawData).replaceAll("BusinessService/|BusinessServices/|ProxyService/|ProxyServices/", "");
        Pattern pattern = Pattern.compile("/[0-9]"); //cislo verze
        Matcher matcher = pattern.matcher(service);
        String[] serviceInfo = service.split("/");
        int l = serviceInfo.length;
        if (matcher.find()) { //has version
            return String.format("%s-%s        %s",l>0?serviceInfo[0]:"??????", l>1?serviceInfo[1]:"??", l>2?serviceInfo[2]:"?????????????????" );
        } else {
            return String.format("%s        %s",l>0?serviceInfo[0]:"??????", l>1?serviceInfo[1]:"?????????????????" );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LogRecordTimeLog) {
            LogRecordTimeLog logRecord = (LogRecordTimeLog) o;
            return logFile.equals(logFile) && beaId.equals(logRecord.getBeaId());
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return beaId.hashCode() ^ logFile.hashCode();
    }

}
