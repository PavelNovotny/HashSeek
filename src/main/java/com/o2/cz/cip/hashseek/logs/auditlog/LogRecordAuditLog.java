package com.o2.cz.cip.hashseek.logs.auditlog;


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
public class LogRecordAuditLog extends AbstractLogRecord {

    public void reportService(PrintStream out) {
        if (firstInBeaId) {
            out.printf("\n%s  %s %s\n", timeStamp, beaId, logFile.getName());
        }
        out.printf("%s %s   -->%s (+%d)\n", lineMarker(), prettyFormatService(), requestOrResponse(), getTimeFromBeginning());
    }


}
