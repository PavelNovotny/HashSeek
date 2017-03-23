package com.o2.cz.cip.hashseek.remote.client;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.remote.RemoteMessage;
import com.o2.cz.cip.hashseek.remote.RemoteSeekParameters;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 28.5.13 15:05
 */
public class SingleRemoteSeek {
    private String host;
    private Integer port;
    private RemoteMessage remoteMessage;
    private boolean initialized = false;

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public RemoteMessage getRemoteMessage() {
        return remoteMessage;
    }

    public void initialize(String[] seekStrings, String fileName, PrintStream output, String envPrefix) throws IOException, ClassNotFoundException, IOException {
        Pattern pattern = Pattern.compile("_(s\\d)");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            String server = matcher.group(1);
            String serverHostPropertyKey = String.format("%s.%s.%s", envPrefix, server, AppProperties.REMOTE_HOST_SUFFIX);
            String serverPortPropertyKey = String.format("%s.%s.%s", envPrefix, server, AppProperties.REMOTE_PORT_SUFFIX);
            this.host = (AppProperties.getValue(serverHostPropertyKey));
            String port = AppProperties.getValue(serverPortPropertyKey);
            if (this.host == null || port == null) {
                HashSeekConstants.outPrintLine(output, String.format("Non existing property for remote seek. Please check properties '%s', '%s' ", serverHostPropertyKey, serverPortPropertyKey));
                return;
            }
            this.port = (Integer.valueOf(port));
            this.remoteMessage = new RemoteMessage();
            RemoteSeekParameters remoteSeekParameters = new RemoteSeekParameters();
            remoteSeekParameters.setClientFileToSeek(fileName);
            remoteSeekParameters.setStringsToSeek(new HashSet<String>(Arrays.asList(seekStrings)));
            this.remoteMessage.setSeekParameters(remoteSeekParameters);
            this.initialized = true;
        } else {
            HashSeekConstants.outPrintLine(output, String.format("file name '%s' does not contain server ('_s1','_s2'..apod. )", fileName));
        }
    }

    public Set<LogRecordAuditLog> remoteSeekByClientFileName(PrintStream output) throws IOException, ClassNotFoundException, IOException {
        return HashSeekClient.seekByClientFileHandshake(this.remoteMessage, this.host, this.port, output);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
