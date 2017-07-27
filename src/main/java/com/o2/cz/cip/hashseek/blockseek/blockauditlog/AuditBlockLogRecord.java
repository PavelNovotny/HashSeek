package com.o2.cz.cip.hashseek.blockseek.blockauditlog;

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
public class AuditBlockLogRecord extends AbstractBlockRecord {

    private String markerPrefix;
    private String beaId;

    public AuditBlockLogRecord(String rawData, File logFile, long filePosition) {
        super(rawData, logFile, filePosition);
    }

    @Override
    public String format() {
        if (formattedData == null) {
            scan();
        }
        return formattedData;
    }

    @Override
    public String timeStamp() {
        String rawData = getRawData();
        return rawData.substring(0,17);
    }

    @Override
    public String headerData() {
        if (headerData==null) {
            scan();
        }
        return headerData;
    }

    @Override
    public String sortKey() { //sortuje se po blokách, uvnitř bloku je už sesortováno tudíž můžeme vynechat beaId apod..
        if (SortInfo.SortBy.ENTRYTIME.equals(sortInfo.getSortBy())) {
            return String.format("%s#%s#%020d", getTimeStamp(), normalizedFileName(), filePosition);
        } else { //default sort, no sort
            return "";
        }
    }

    private void scan() {
        Scanner scanner = new Scanner(rawData);
        StringBuilder formattedData = new StringBuilder();
        StringBuilder headerData = new StringBuilder();
        StringBuilder xml = new StringBuilder();
        String reqResp = null;
        String service=null;
        String marker=null;
        String firstLine=null;
        int markerCount =0;
        headerData.append(getTimeStamp()).append("   ").append(logFile.getName());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] tokens = line.split(";", 9);
            if (tokens.length == 9 && line.matches("^\\d{8};\\d{2}:\\d{2}:\\d{2}.*$")) { //bea id line
                markerCount++;
                if (firstLine != null) { //zpracujeme předchozí záznam
                    formattedData.append("\n").append(marker).append("\n").append(service).append("\n").append(firstLine).append("\n").append(formatXML(xml.toString(),2));
                    headerData.append("\n").append(marker).append("  ").append(service).append("   ").append(reqResp);
                    xml = new StringBuilder();
                }
                firstLine = String.format("%s;%s;%s;%s;%s;%s;%s;%s",tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6], tokens[7]);
                this.beaId = tokens[3];
                service = tokens[5].replaceAll("[$]", "/");
                reqResp = tokens[7];
                marker = createMarker(logFile, filePosition, markerCount);
                xml.append(tokens[8]);
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

    private String createMarker(File logFile, long filePosition, int count) {
        return String.format("%s%012d%04d", getMarkerPrefix(logFile),filePosition, count);
    }

    private String getMarkerPrefix(File logFile) {
        if (markerPrefix ==null) {
            Pattern pattern = Pattern.compile(".*_s\\d"); //other_s1, jms_s2, etc...
            Matcher matcher = pattern.matcher(logFile.getName());
            if (matcher.find()) {
                String domain = matcher.group().substring(0,1);
                String server = matcher.group().substring(matcher.group().length()-1);
                String env;
                if (logFile.getPath().contains("gf")) { //maintest
                    env =  "m";
                } else if (logFile.getPath().contains("e2e")) { //E2E
                    env = "e2";
                } else if (logFile.getPath().contains("e3e")) {
                    env =  "e3";
                } else if (logFile.getPath().contains("datamig")) {
                    env =  "d";
                } else {
                    env =  "";
                }
                this.markerPrefix = domain + server + env;
            } else {
                this.markerPrefix = "xx";
            }
        }
        return markerPrefix;
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

    public String getBeaId() {
        if (beaId ==null) {
            scan();
        }
        return beaId;
    }

}
