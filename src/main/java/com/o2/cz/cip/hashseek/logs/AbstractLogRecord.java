package com.o2.cz.cip.hashseek.logs;


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
public abstract class AbstractLogRecord implements Comparable<AbstractLogRecord>, Serializable {
    public static enum SortBy {ENTRYTIME_BEAID_LINE, BEAID_LINE};
    public static SortBy sortBy;
    protected String rawData;
    protected String timeStamp;
    protected File logFile;
    protected String beaId;
    protected String beaIdEnterTime;
    protected long filePosition;
    protected String markerPrefix;
    protected boolean firstInBeaId = false;
    protected boolean lastInBeaId = false;


    public boolean isFirstInBeaId() {
        return firstInBeaId;
    }

    public void setFirstInBeaId(boolean firstInBeaId) {
        this.firstInBeaId = firstInBeaId;
    }

    public boolean isLastInBeaId() {
        return lastInBeaId;
    }

    public void setLastInBeaId(boolean lastInBeaId) {
        this.lastInBeaId = lastInBeaId;
    }

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    public void setMarkerPrefix(String logFileName) {
        Pattern pattern = Pattern.compile(".*_s\\d"); //other_s1, jms_s2, etc...
        Matcher matcher = pattern.matcher(logFile.getName());
        if (matcher.find()) {
            String domain = matcher.group().substring(0,1);
            if (logFile.getPath().contains("gf")) { //maintest
                domain = domain + "m";
            } else if (logFile.getPath().contains("e2e")) { //E2E
                domain = domain + "e";
            } else if (logFile.getPath().contains("datamig")) {
                domain = domain + "d";
            }
            this.markerPrefix = domain + matcher.group().substring(matcher.group().length()-1);
        } else {
            this.markerPrefix = "xx";
        }
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
        Pattern pattern = Pattern.compile(";[0-9a-f\\-\\.]{32,50};"); //bea id pattern
        Matcher matcher = pattern.matcher(rawData);
        if (matcher.find()) {
            setBeaId(matcher.group());
        } else {
            setBeaId("bea_id");
        }
    }

    public String getBeaIdEnterTime() {
        return beaIdEnterTime;
    }

    public void setBeaIdEnterTime(String beaIdEnterTime) {
        this.beaIdEnterTime = beaIdEnterTime;
    }

    public String getBeaId() {
        return beaId;
    }

    public void setBeaId(String beaId) {
        this.beaId = beaId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getFilePosition() {
        return filePosition;
    }

    public void setFilePosition(long filePosition) {
        this.filePosition = filePosition;
    }

    protected String parseRawData() {
        if (rawData.contains(";<")) {
            String[] parts = rawData.split(";<",2);
            String xml = "<".concat(parts[1]);
            xml = prettyFormat(xml, 2);
            String service = extractService(parts[0]);
            return service.concat("\n").concat(parts[0]).concat("\n").concat(xml).concat("\n");
        }
        return rawData;
    }

    private String getSortedKey() {
        if (SortBy.BEAID_LINE.equals(sortBy)) {
            return beaId.concat("#").concat(lineMarker());
        } else if (SortBy.ENTRYTIME_BEAID_LINE.equals(sortBy)){
            return beaIdEnterTime.concat("#").concat(beaId).concat("#").concat(lineMarker());
        } else { //default sort, no sort
            return "";
        }
    }


    public String lineMarker() {
        return String.format("%s%016d",getMarkerPrefix(), filePosition);
    }

    private String extractService(String part) {
        Pattern pattern = Pattern.compile(";\\w+\\$[^;]+");
        Matcher matcher = pattern.matcher(part);
        if (matcher.find()) {
            return matcher.group().substring(1).replaceAll("\\$","/");
        }
        return "";
    }

    public int compareTo(AbstractLogRecord logRecord) {
        return getSortedKey().compareTo(logRecord.getSortedKey());
    }

    public void reportData(PrintStream out) {
        out.println(lineMarker());
        out.println(parseRawData());
    }


    protected String requestOrResponse() {
        if (rawData.contains(";REQUEST")) {
            return "REQUEST";
        } else if (rawData.contains(";RESPONSE")) {
            return "RESPONSE";
        }
        return "";
    }

    private String onlyTime() {
        return timeStamp.replaceFirst(".*;","");
    }

    protected int getTimeFromBeginning() {
        String[] timeParts = timeStamp.split(";|:");
        String[] beginningParts = beaIdEnterTime.split(";|:");
        int begin = Integer.parseInt(beginningParts[1])/60/60 + Integer.parseInt(beginningParts[2])/60 + Integer.parseInt(beginningParts[3]);
        int time = Integer.parseInt(timeParts[1])/60/60 + Integer.parseInt(timeParts[2])/60 + Integer.parseInt(timeParts[3]);
        return time - begin;
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
        if (o instanceof AbstractLogRecord) {
            AbstractLogRecord logRecord = (AbstractLogRecord) o;
            return logFile.equals(logFile) && filePosition == logRecord.getFilePosition();
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return logFile.hashCode() ^ (int)filePosition;
    }

    public static String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Throwable e) {
            return input;
        }
    }

    public String getMarkerPrefix() {
        if (markerPrefix ==null) {
            setMarkerPrefix(logFile.getPath());
        }
        return markerPrefix;
    }

    public void reportService(PrintStream out){}
}
