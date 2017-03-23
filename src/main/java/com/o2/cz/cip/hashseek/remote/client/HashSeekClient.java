package com.o2.cz.cip.hashseek.remote.client;

import com.o2.cz.cip.hashseek.logs.auditlog.HashSeekAuditLog;
import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.remote.RemoteMessage;
import com.o2.cz.cip.hashseek.remote.RemoteSeekParameters;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Pavel
 * Date: 26.4.13 10:16
 */
public class HashSeekClient {

    private Socket socket;

    public boolean connect(String remoteHost, int remotePort, PrintStream output) throws IOException {
        try {
            this.socket = new Socket(remoteHost, remotePort);
        } catch (ConnectException e) {
            return false;
        }
        return true;
    }

    public void close() throws IOException {
        socket.close();
    }

    public void send(RemoteMessage remoteMessage) throws IOException {
        HashSeekConstants.outPrintLine(String.format("SENT remote message to %s:%s", socket.getInetAddress().getHostAddress(), socket.getPort()));
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(remoteMessage);
        oos.flush();
        RemoteMessage.reportRemoteMessage(remoteMessage);
    }

    public RemoteMessage retrieveAnswer() throws IOException, ClassNotFoundException {
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        RemoteMessage remoteMessage = (RemoteMessage) ois.readObject();
        HashSeekConstants.outPrintLine(String.format("RECEIVED remote message from %s:%s", socket.getInetAddress().getHostAddress(), socket.getLocalPort()));
        RemoteMessage.reportRemoteMessage(remoteMessage);
        is.close();
        socket.close();
        return remoteMessage;
    }

    public static synchronized RemoteMessage askEsbLogServer(RemoteMessage remoteMessage, String host, int port, PrintStream output) throws IOException, ClassNotFoundException {
        HashSeekClient hashSeekClient = new HashSeekClient();
        if (hashSeekClient.connect(host, port, output)) {
            hashSeekClient.send(remoteMessage);
            RemoteMessage answer = hashSeekClient.retrieveAnswer();
            return answer;
        } else {
            RemoteMessage answer = new RemoteMessage();
            answer.setMessage(RemoteMessage.CONNECTION_NOT_AVAILABLE);
            return answer;
        }
    }

    public static synchronized boolean basicHandshake(RemoteMessage remoteMessage, String host, int port, PrintStream output) throws IOException, ClassNotFoundException {
        remoteMessage.setMessage(RemoteMessage.READY_QUESTION);
        HashSeekConstants.outPrintLine(String.format("%s:%s <<<- '%s'", host, port, remoteMessage.getMessage()));
        RemoteMessage answer = askEsbLogServer(remoteMessage, host, port, output);
        if (RemoteMessage.READY_ANSWER.equals(answer.getMessage())) {
            HashSeekConstants.outPrintLine(String.format("Ocekavana odpoved:'%s', obdrzeno '%s'", RemoteMessage.READY_ANSWER, answer.getMessage()));
            HashSeekConstants.outPrintLine(String.format("%s:%s ->>> '%s', OK basic handshake", answer.getMessage()));
            return true;
        } else {
            HashSeekConstants.outPrintLine(String.format("Ocekavana odpoved:'%s', obdrzeno '%s'", RemoteMessage.READY_ANSWER, answer.getMessage()));
            return false;
        }
    }

    public static synchronized Set<LogRecordAuditLog> seekByClientFileHandshake(RemoteMessage remoteMessage, String host, int port, PrintStream output) throws IOException, ClassNotFoundException {
        remoteMessage.setMessage(RemoteMessage.SEEK_BY_CLIENT_PLEASE);
        HashSeekConstants.outPrintLine(output, String.format("%s:%s <<<- '%s' '%s'", host, port, remoteMessage.getMessage(), remoteMessage.getSeekParameters().getClientFileToSeek()));
        RemoteMessage answer = askEsbLogServer(remoteMessage, host, port, output);
        HashSeekConstants.outPrintLine(output, String.format("%s:%s ->>> '%s'", host, port, answer.getMessage()));
        return answer.getLogRecords();
    }

    public static void main(String args[]){
        try{
            RemoteMessage remoteMessage = new RemoteMessage();
            RemoteSeekParameters seekParameters = new RemoteSeekParameters();
            Set<String> stringsToSeek = new HashSet<String>();
            stringsToSeek.add("xxxx");
//            stringsToSeek.add("yyyy");
            stringsToSeek.add("generated-r2-hgsuha0w-jacm");
            seekParameters.setStringsToSeek(stringsToSeek);
//            seekParameters.setDateHourFrom("20130517.06");
//            seekParameters.setDateHourTo("20130517.06");
            remoteMessage.setSeekParameters(seekParameters);
            HashSeekAuditLog hashSeek = new HashSeekAuditLog();
            //hashSeek.reportSortedResults(new File("./result.txt"));
            remoteMessage = new RemoteMessage();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
