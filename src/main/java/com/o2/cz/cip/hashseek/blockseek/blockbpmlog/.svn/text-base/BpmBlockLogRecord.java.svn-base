package com.o2.cz.cip.hashseek.blockseek.blockbpmlog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.SortInfo;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class BpmBlockLogRecord extends AbstractBlockRecord {

    private String markerPrefix;

    public BpmBlockLogRecord(String rawData, File logFile, long filePosition) {
        super(rawData, logFile, filePosition);
    }

    @Override
    public String format() {
        scan();
        return formattedData;
/*        StringBuilder data = new StringBuilder();
        data.append(createMarker(logFile, filePosition, 1)).append(" ").append("BPM").append(" ");
        data.append(rawData);
        return data.toString();*/
    }

    @Override
    public String timeStamp() {
        String rawData = getRawData();
        return rawData.substring(0,17);
    }

    @Override
    public String headerData() {
        scan();
        return headerData;
/*        StringBuilder headerData = new StringBuilder();
        headerData.append(createMarker(logFile, filePosition, 1)).append(" ").append("BPM");
//        headerData.append("BPM:\n").append(createMarker(logFile, filePosition, 1));
        headerData.append("   ").append(getTimeStamp()).append("   ").append(logFile.getName()).append("   ").append(getProcess());
        return headerData.toString();*/
    }

    @Override
    public String sortKey() { //sortuje se po blokách, uvnitř bloku je už sesortováno tudíž můžeme vynechat beaId apod..
        if (SortInfo.SortBy.ENTRYTIME.equals(sortInfo.getSortBy())) {
            return String.format("%s#%s#%020d", getTimeStamp(), normalizedFileName(), filePosition);
        } else { //default sort, no sort
            return "";
        }
    }

    private String createMarker(File logFile, long filePosition, int count) {
        return String.format("%s%012d%04d", getMarkerPrefix(logFile),filePosition, count);
    }

    private String getMarkerPrefix(File logFile) {
        if (markerPrefix ==null) {
            Pattern pattern = Pattern.compile(".*_s\\d"); //other_s1, jms_s2, etc...
            Matcher matcher = pattern.matcher(logFile.getName());
            if (matcher.find()) {
                this.markerPrefix = matcher.group().substring(0,1) + matcher.group().substring(matcher.group().length()-1);
            } else {
                this.markerPrefix = "xx";
            }
        }
        return markerPrefix;
    }

    public String getProcess() {
        String[] split = rawData.split(";",5);
        if(split.length>=4){
            return split[3];
        }
        return "???";
    }

    private void scan() {
        Scanner scanner = new Scanner(rawData);
        StringBuilder formattedData = new StringBuilder();
        StringBuilder headerData = new StringBuilder();
        StringBuilder xml = new StringBuilder();
        String reqResp = null;
        String service=null;
        String shortInfo=null;
        String marker=null;
        String firstLine=null;
        int markerCount =0;
        headerData.append(getTimeStamp()).append("   ").append(logFile.getName());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] tokens = line.split(";", 8);
            if (tokens.length == 8) { //bea id line
                markerCount++;
                if (firstLine != null) { //zpracujeme předchozí záznam
                    formattedData.append("\n\n").append(marker).append("\n").append(service).append("\n").append(firstLine).append("\n").append(formatXML(xml.toString(),2));
                    headerData.append("\n").append(marker).append("  ").append(service).append("  -  ").append(shortInfo);
                    xml = new StringBuilder();
                }
                service = tokens[3];
                shortInfo=getShortInfo(tokens[7]);
                firstLine = String.format("%s;%s;%s;%s;%s;%s;%s",tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
                marker = createMarker(logFile, filePosition, markerCount);
                xml.append(tokens[7]);
            } else {
                xml.append(line);
            }
        }
        formattedData.append("\n").append(marker).append("\n").append(service).append("\n").append(firstLine).append("\n").append(formatXML(xml.toString(),2));
        headerData.append("\n").append(marker).append("  ").append(service).append("   ").append(reqResp).append("\n\n");
        scanner.close();
        this.formattedData = formattedData.toString();
        this.headerData = headerData.toString();
    }

    private String formatXML(String unformattedXml, int indent) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(null);
            InputSource is = new InputSource(new StringReader(unformattedXml));
            final org.w3c.dom.Document document = db.parse(is);
            com.sun.org.apache.xml.internal.serialize.OutputFormat format = new com.sun.org.apache.xml.internal.serialize.OutputFormat(document);
            format.setLineWidth(0); //nowrap
            format.setIndenting(true);
            format.setIndent(indent);
            Writer out = new StringWriter();
            com.sun.org.apache.xml.internal.serialize.XMLSerializer serializer = new com.sun.org.apache.xml.internal.serialize.XMLSerializer(out, format);
            serializer.serialize(document);
            return out.toString();
        } catch (Exception e) {
            return unformattedXml;
        }

    }

    private String getShortInfo(String str){
       int maxSize=50;
       if(str!=null){
           if(str.length()>maxSize){
               return str.substring(0,maxSize)+"...";
           }else{
               return str;
           }
       }else{
           return "";
       }
    }
}
