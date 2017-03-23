package com.o2.cz.cip.hashseek.remote;

import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;

import java.io.Serializable;
import java.util.Set;

/**
 * User: Pavel
 * Date: 26.4.13 10:02
 */
public class RemoteMessage implements Serializable {
    private String message; //request
    private RemoteSeekParameters seekParameters;
    private Set<LogRecordAuditLog> logRecords; //response for LogRecords found (in case of ESB seek)

    public static final String READY_QUESTION = "Ready?";
    public static final String READY_ANSWER = "Ready!";
    public static final String SEEK_BY_CLIENT_PLEASE = "Seek by my file!";
    public static final String FOUND = "Seek Results: FOUND";
    public static final String NOT_FOUND = "Seek Results: NOT found";
    public static final String DONT_UNDERSTAND = "??? Don't understand !!!";
    public static final String NO_SEEK_PARAMS = "Please specify seek params";
    public static final String NO_DOMAIN_IN_NAME = "No domain (other, jms) in file name, cannot determine local file location.";
    public static final String NO_SERVER_IN_NAME = "No server (s1, s2) in file name, cannot determine local file name";
    public static final String FILE_DOESNT_EXISTS = "File does not exists";
    public static final String CONNECTION_NOT_AVAILABLE = "CONNECTION NOT AVAILABLE";

    public RemoteSeekParameters getSeekParameters() {
        return seekParameters;
    }

    public void setSeekParameters(RemoteSeekParameters seekParameters) {
        this.seekParameters = seekParameters;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<LogRecordAuditLog> getLogRecords() {
        return logRecords;
    }

    public void setLogRecords(Set<LogRecordAuditLog> logRecords) {
        this.logRecords = logRecords;
    }


//report section
    public static final short REPORT_LEVEL_NONE = 0;
    public static final short REPORT_LEVEL_REMOTEMESSAGE = 1;
    public static final short REPORT_LEVEL_PARAMS = 2;
    public static final short REPORT_LEVEL_LOGRECORDS = 4;
    public static short REPORT_LEVEL = REPORT_LEVEL_REMOTEMESSAGE | REPORT_LEVEL_LOGRECORDS | REPORT_LEVEL_PARAMS;
//    public static short REPORT_LEVEL = REPORT_LEVEL_NONE;
//    public static short REPORT_LEVEL = REPORT_LEVEL_REMOTEMESSAGE;

    public static void reportRemoteMessage(RemoteMessage remoteMessage) {
        if (remoteMessage != null){
            if ((REPORT_LEVEL & REPORT_LEVEL_REMOTEMESSAGE) != 0) {
                HashSeekConstants.outPrintLine(String.format("remote message, message: %s", remoteMessage.getMessage()));
            }
            if ((REPORT_LEVEL & REPORT_LEVEL_LOGRECORDS) != 0) {
                if (remoteMessage.getLogRecords() != null) {
                    for (LogRecordAuditLog logRecord : remoteMessage.getLogRecords()) {
                        HashSeekConstants.outPrintLine(String.format("remote message, logRecord, beaId: %s",logRecord.getBeaId()));
                    }
                } else {
                    HashSeekConstants.outPrintLine(String.format("remote message, logRecords: empty"));
                }
            }
            if ((REPORT_LEVEL & REPORT_LEVEL_PARAMS) != 0) {
                if (remoteMessage.getSeekParameters() != null) {
                    remoteMessage.getSeekParameters().reportParams();
                } else {
                    HashSeekConstants.outPrintLine(String.format("remote message, params: empty"));
                }
            }
        } else {
            HashSeekConstants.outPrintLine(String.format("remote message is EMPTY"));
        }
    }



}
