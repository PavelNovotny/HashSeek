package com.o2.cz.cip.hashseek.remote.listener;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.io.RandomSeekableInputStream;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;
import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.remote.RemoteMessage;
import com.o2.cz.cip.hashseek.remote.RemoteSeekParameters;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 26.4.13 9:57
 */
public class HashSeekListener {
    ServerSocket serverSocket;

    public void createSocket(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void closeSocket() throws IOException {
        serverSocket.close();
    }

    public void startListening() throws IOException, ClassNotFoundException {
        HashSeekConstants.outPrintLine(String.format("Starting listening on %s:%s", serverSocket.getInetAddress().getHostName(), serverSocket.getLocalPort()));
        do {
            Socket socket = serverSocket.accept(); //blocking
            try {
                InputStream is = socket.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                RemoteMessage remoteMessage = (RemoteMessage) ois.readObject();
                HashSeekConstants.outPrintLine(String.format("RECEIVED remote message from %s:%s", socket.getInetAddress().getHostAddress(), socket.getLocalPort()));
                processRemoteMessage(remoteMessage, socket);
                socket.close();
            } catch (SocketException e) {
                HashSeekConstants.outPrintLine(String.format("%s",e));
            }
        } while (true);
    }

    private void processRemoteMessage(RemoteMessage remoteMessage, Socket clientSocket) throws IOException {
        RemoteMessage.reportRemoteMessage(remoteMessage);
        if (RemoteMessage.READY_QUESTION.equals(remoteMessage.getMessage())) { //basic handshaking
            RemoteMessage answer = new RemoteMessage();
            answer.setMessage(RemoteMessage.READY_ANSWER);
            sendToClient(answer, clientSocket);
        } else if (RemoteMessage.SEEK_BY_CLIENT_PLEASE.equals(remoteMessage.getMessage())){
            RemoteMessage answer = seekByClientFile(remoteMessage);
            sendToClient(answer, clientSocket);
        } else {
            RemoteMessage answer = new RemoteMessage();
            answer.setMessage(RemoteMessage.DONT_UNDERSTAND);
            sendToClient(answer, clientSocket);
        }
    }

    private RemoteMessage seekClientFile(RemoteMessage remoteMessage) throws IOException {
        RemoteSeekParameters seekParameters = remoteMessage.getSeekParameters();
        String clientFileToSeek = seekParameters.getClientFileToSeek();
        RemoteMessage answer = new RemoteMessage();
        if (seekParameters != null && seekParameters.getStringsToSeek() != null && clientFileToSeek !=null) {
            Pattern pattern = Pattern.compile(String.format("(%s|%s)", AbstractLogSeek.DOMAIN_JMS, AbstractLogSeek.DOMAIN_OTHER));
            Matcher matcher = pattern.matcher(clientFileToSeek);
            if (matcher.find()) {
                String domain = matcher.group(1);
                String logLocation = AppProperties.getValue(String.format("%s.%s", AppProperties.LOG_LOCATION_PREFIX, domain));
                Pattern serverPattern = Pattern.compile("_(s\\d)");
                Matcher serverMatcher = serverPattern.matcher(clientFileToSeek);
                if (serverMatcher.find()) {
                    String server = serverMatcher.group(1);
                    String localName = String.format("aspect_alsb_%s.audit.%s", server, clientFileToSeek.replaceFirst(".*audit\\.", "")).replaceFirst("\\.$","");
                    File fileToSeek = new File(new File(logLocation), localName);
                    if (fileToSeek.exists()) {
                        SeekableInputStream raf = new RandomSeekableInputStream(fileToSeek, "r");
                        RemoteEsbAuditSeek remoteSeek = new RemoteEsbAuditSeek(raf);
                        remoteSeek.sequentialSeek(seekParameters.getStringsToSeek(), seekParameters.getClientFileToSeek());
                        Set<LogRecordAuditLog> results = remoteSeek.getResults();
                        if (results == null || results.isEmpty()) {
                            answer.setMessage(String.format("'%s' '%s' > '%s'",RemoteMessage.NOT_FOUND, clientFileToSeek, fileToSeek.getPath()));
                        } else {
                            answer.setMessage(String.format("'%s' '%s' > '%s'",RemoteMessage.FOUND, clientFileToSeek, fileToSeek.getPath()));
                            answer.setLogRecords(results);
                        }
                    } else {
                        answer.setMessage(String.format("%s '%s' > '%s'",RemoteMessage.FILE_DOESNT_EXISTS, clientFileToSeek, fileToSeek.getPath()));
                    }
                } else {
                    answer.setMessage(RemoteMessage.NO_SERVER_IN_NAME);
                }
            } else {
                answer.setMessage(RemoteMessage.NO_DOMAIN_IN_NAME);
            }
        } else {
            answer.setMessage(RemoteMessage.NO_SEEK_PARAMS);
        }
        return answer;
    }

    private RemoteMessage seekLastLog(RemoteMessage remoteMessage, File fileToSeek) throws IOException {
        HashSeekConstants.outPrintLine("seeking last log");
        RemoteSeekParameters seekParameters = remoteMessage.getSeekParameters();
        RemoteMessage answer = new RemoteMessage();
        answer.setLogRecords(new HashSet<LogRecordAuditLog>());
        if (seekParameters != null && seekParameters.getStringsToSeek() != null && fileToSeek !=null) {
            String domainLogLocationKey = String.format("", AbstractLogSeek.DOMAIN_JMS);
            for (int i=1; i< HashSeekConstants.MAX_LOG_LOCATION_COUNT; i++) {
                String location = AppProperties.getLogLocation(i); //other, jms
                if (location == null) {
                    break;
                }
                File logDir = new File(location);
                File logFile = new File(logDir, String.format("aspect.alsb_%s.audit", AppProperties.getEsbServerNumber(i)));
//                remoteMessage.getSeekParameters().setFileToSeek(logFile);
                RemoteMessage singleAnswer = seekByLocalFile(remoteMessage);
                if (singleAnswer.getLogRecords() != null) {
                    answer.getLogRecords().addAll(singleAnswer.getLogRecords());
                }
            }
            Set<LogRecordAuditLog> results = answer.getLogRecords();
            if (results == null || results.isEmpty()) {
                answer.setMessage(RemoteMessage.NOT_FOUND);
            } else {
                answer.setMessage(RemoteMessage.FOUND);
                answer.setLogRecords(results);
            }
        } else {
            answer.setMessage(RemoteMessage.NO_SEEK_PARAMS);
        }
        return answer;
    }

    private RemoteMessage seekByClientFile(RemoteMessage remoteMessage) throws IOException {
        HashSeekConstants.outPrintLine(String.format("Seeking by client file '%s'", remoteMessage.getSeekParameters().getClientFileToSeek()));
        RemoteSeekParameters seekParameters = remoteMessage.getSeekParameters();
        return seekClientFile(remoteMessage);
    }

    private RemoteMessage seekByLocalFile(RemoteMessage remoteMessage) throws IOException {
        RemoteSeekParameters seekParameters = remoteMessage.getSeekParameters();
        RemoteMessage answer = new RemoteMessage();
//        if (seekParameters != null && seekParameters.getStringsToSeek() != null && seekParameters.getFileToSeek() !=null) {
//            HashSeekConstants.outPrintLine(String.format("seeking file '%s'", seekParameters.getFileToSeek().getPath()));
//            RandomAccessFile raf = new RandomAccessFile(seekParameters.getFileToSeek(), "r");
//            RemoteEsbAuditSeek remoteSeek = new RemoteEsbAuditSeek(raf);
//            remoteSeek.seek(seekParameters);
//            Set<LogRecord> results = remoteSeek.getResults();
//            if (results == null || results.isEmpty()) {
//                answer.setMessage(RemoteMessage.NOT_FOUND);
//            } else {
//                answer.setMessage(RemoteMessage.FOUND);
//                answer.setLogRecords(results);
//            }
//        } else {
//            HashSeekConstants.outPrintLine(String.format("no seeking file or seek params"));
//            answer.setMessage(RemoteMessage.NO_SEEK_PARAMS);
//        }
        return answer;
    }

    private void sendToClient(RemoteMessage remoteMessage, Socket clientSocket) throws IOException {
        HashSeekConstants.outPrintLine(String.format("SENT remote message to %s:%s", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort()));
        OutputStream os = clientSocket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(remoteMessage);
        oos.flush();
        RemoteMessage.reportRemoteMessage(remoteMessage);
    }

    public static void main(String args[]) {
        try {
            HashSeekListener hashSeekListener = new HashSeekListener();
            hashSeekListener.createSocket(AppProperties.getRemotePort());
            hashSeekListener.startListening();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


}
