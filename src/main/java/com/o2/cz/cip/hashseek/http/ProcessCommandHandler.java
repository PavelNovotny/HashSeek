package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.blockseek.AbstractSeekResults;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditSeekResults;
import com.o2.cz.cip.hashseek.blockseek.blockb2blog.B2BSeekResults;
import com.o2.cz.cip.hashseek.blockseek.blockbpmlog.BpmSeekResults;
import com.o2.cz.cip.hashseek.blockseek.blocktimelog.TimeSeekResults;
import com.o2.cz.cip.hashseek.blockseek.noelog.NoeSeekResults;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.RandomSeekableInputStream;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import com.o2.cz.cip.hashseek.logs.AbstractLogRecord;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;
import com.o2.cz.cip.hashseek.logs.auditlog.HashSeekAuditLog;
import com.o2.cz.cip.hashseek.logs.auditlog.LogRecordAuditLog;
import com.o2.cz.cip.hashseek.logs.evaluate.*;
import com.o2.cz.cip.hashseek.logs.timelog.HashSeekTimeLog;
import com.o2.cz.cip.hashseek.remote.client.SingleRemoteSeek;
import com.o2.cz.cip.hashseek.remote.listener.RemoteEsbAuditSeek;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URI;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 29.9.12 12:20
 */
class ProcessCommandHandler implements HttpHandler, Runnable {
    static Logger loggerAccess= LoggerFactory.getLogger("hashseek.access");
    static  Logger LOGGER=LoggerFactory.getLogger(ProcessCommandHandler.class);
    
	private HttpExchange exchange;
	private ProcessCommandHandler parent;
	private ProcessCommandHandler activeChild;
	private int numberOfThreads = 0;
	private static int MAX_THREADS = 1;

	public void handle(HttpExchange exchange) throws IOException {
		if (numberOfThreads < MAX_THREADS) {
			numberOfThreads++;
			ProcessCommandHandler processCommandHandler = new ProcessCommandHandler();
			processCommandHandler.setExchange(exchange);
			processCommandHandler.setParent(this);
			(new Thread(processCommandHandler)).start();
			this.activeChild = processCommandHandler;
		} else {
			URI requestURI = activeChild.getExchange().getRequestURI();
			String query = requestURI.getQuery();
			Session session = Session.sessionMap.get(query);
			OutputStream os = exchange.getResponseBody();
			exchange.sendResponseHeaders(200, 0);
			os.write(String.format("\n\nMaximum number of concurrent seeks '%s' exceeded.", MAX_THREADS).getBytes());
			os.write(String.format("\n\nCurrently %s %s is searching %s.", session.getDomainUser(), session.getUserName(), seekEnv(session)).getBytes());
            os.write(String.format("\nShe/he searches from %s for %s day(s).", session.getSeekDay(), session.getHoursToSeek()).getBytes());
            os.write(String.format("\nShe/he has occupied HashSeek for %s now.", HashSeekConstants.formatedTimeMillis(System.currentTimeMillis() - session.getStart())).getBytes());
			os.write(String.format("\n\nPLEASE TRY AGAIN LATER.", MAX_THREADS).getBytes());
			os.flush();
			os.close();
		}
	}

    public String seekEnv(Session session) {
        StringBuilder stringBuilder = new StringBuilder();
        if (session.isProd()) {
            stringBuilder.append("production");
        }
        if (session.isPredprod()) {
            if (stringBuilder.length()>0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("preprod");
        }
        if (session.isTest()) {
            if (stringBuilder.length()>0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("test");
        }
        if (session.isOnlineEsbSeek()) {
            if (stringBuilder.length()>0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("online esb");
        }
        if (session.isOnlineBPMSeek()) {
            if (stringBuilder.length()>0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("online bmp");
        }
        if (session.isIncludeTimeLogs()) {
            if (stringBuilder.length()>0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("time");
        }
        if (stringBuilder.length()>0) {
            stringBuilder.append(" logs");
        }
        return stringBuilder.toString();
    }

	public HttpExchange getExchange() {
		return exchange;
	}

	public void setExchange(HttpExchange exchange) {
		this.exchange = exchange;
	}

	public ProcessCommandHandler getParent() {
		return parent;
	}

	public void setParent(ProcessCommandHandler parent) {
		this.parent = parent;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	private void writeToBrowser(String query, OutputStream os, BufferedReader input) throws IOException {
		String line;
		while ((line = input.readLine()) != null) {
			checkFileName(query, line);
			if (!line.endsWith("\n") || !line.endsWith("\r")) {
				line = String.format("%s\n", line);
			}
			if (!"".equals(line.trim())) {
				os.write(line.getBytes());
			}
			os.flush();
		}
		os.close();
	}

	private void terminate(OutputStream os, BufferedReader input, Process process) throws IOException {
		os.write("\n\n!!! PROCESS TERMINATED. !!!".getBytes());
		os.flush();
		os.close();
		process.destroy();
	}

	private void checkFileName(String query, String line) {
		String compareString = "Vysledky budou zapsany do souboru";
		if (line.length() > compareString.length() && line.contains(compareString)) {
			Session session = Session.sessionMap.get(query);
			Pattern pattern = Pattern.compile("'(.*)'", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				session.setFileName(matcher.group(1));
			}
		}
	}

	private void runSeek(OutputStream os, String query) throws IOException, ClassNotFoundException, XPathExpressionException, ParserConfigurationException, SAXException {
		Session session = Session.sessionMap.get(query);

		PrintStream detailedOutput;

		if (session.isRuninfo()) {
			detailedOutput = new PrintStream(os);
		} else {
			detailedOutput = new PrintStream(new File("./null"));
		}
		PrintStream output = new PrintStream(os);
		if (session.getDefect() == null
				|| session.getDefect().isEmpty()
				|| session.getHoursToSeek() == null
				|| session.getHoursToSeek().isEmpty()
				|| session.getSeekDay() == null
				|| session.getSeekDay().isEmpty()
				|| session.isSeekStringsEmpty()
				) {
			String message = String.format("Zadejte parametry:\n" +
					"Oznaceni defektu\n" +
					"Den od\n" +
					"Pocet dnu k prohledani\n" +
					"Alespon jedno hledane slovo \n", query);
			HashSeekConstants.outPrintLine(output, message);
			return;
		}
        if (session.isProd() && !session.isEligibleSeekProd()) {
            HashSeekConstants.outPrintLine(output, String.format("You are not eligible to seek Production CIP files. Please uncheck 'Prod' and continue.\nIf you wish to be eligible to seek production CIP files, please contact Radek Vojtíšek or Tomáš Lorenc."));
            return;
        }
		try {
			Date d = HashSeekConstants.dateFormat.parse(session.getSeekDay());
			//HashSeekConstants.outPrintLineSimple(output, HashSeekConstants.dateFormat.format(d));
		} catch (ParseException e) {
			if (!notNum(session.getSeekDay())) {
				HashSeekConstants.outPrintLine(output, "Chybne zadany den od (napr. 18.01.2014): " + session.getSeekDay());
				return;
			}
		}
		if(!session.isProd() && !session.isPredprod() && !session.isTest()) {
			HashSeekConstants.outPrintLine(output, "Vyberte alespon jedno prostredi");
			return;
		}

		AppArguments appArguments = new AppArguments();
		appArguments.setSeekPredprod(session.isPredprod());
		appArguments.setSeekProd(session.isProd());
		appArguments.setSeekTest(session.isTest());
		appArguments.setRuninfo(session.isRuninfo());
		appArguments.setOnlineEsbSeek(session.isOnlineEsbSeek());
		appArguments.setOnlineBPMSeek(session.isOnlineBPMSeek());
        appArguments.setNoeSeek(session.isNoeSeek());
		appArguments.setDayFrom(session.getSeekDay());
		appArguments.setDaysToSeek(session.getHoursToSeek());
        appArguments.setIncludeTimeLogs(session.isIncludeTimeLogs());
		List<String> seekedStrings = new ArrayList<String>();
		for (int i = 0; i < Session.SEEK_STRING_COUNT; i++) {
			String seekString = session.getSeekString(i);
			if (null != seekString && !"".equals(seekString.trim())) {
				seekedStrings.add(seekString);
			}
		}
        List<List<String>> andStrings = new ArrayList<List<String>>();
        int andCondCount = Session.SEEK_STRING_COUNT / Session.AND_CONDITIONS;
        for (int i = 0; i < andCondCount; i++) {
            andStrings.add(new ArrayList<String>());
            for (int j = 0; j < Session.AND_CONDITIONS; j++) {
                String seekString = session.getSeekString(i*Session.AND_CONDITIONS+j);
                if (null != seekString && !"".equals(seekString.trim()) && seekString.length() >= HashSeekConstants.MIN_WORD_SIZE) {
                    andStrings.get(i).add(seekString);
                }
            }
        }
        List<String> filter = null;
        for (int i = 0; i< Session.FILTER_STRING_COUNT; i++) {
            String filterString = session.getFilterString(i);
            if (null != filterString && !"".equals(filterString.trim())) {
                filter = session.getFilter();
                break;
            }
        }
		HashSeekConstants.outPrintLineSimple(output, "Started...");
		HashSeekConstants.outPrintLineSimple(output, appArguments.getDateFrom() + "-" + appArguments.getDateTo());
        session.setFileName(String.format("./reports/%s_%s-%s.txt", session.getDefect(), appArguments.getDateFrom(), appArguments.getDateTo()));
		long start = System.currentTimeMillis();
        session.setStart(start);
        HashVersionEvaluator hashVersionEvaluator = new HashVersionEvaluator();
        int version = hashVersionEvaluator.hashVersion(appArguments);
        if (version == 0) {
            oldSeek(session, detailedOutput, output, appArguments, seekedStrings, start);
        } else if (version == 1) {
            seek_v1(session, detailedOutput, output, appArguments, andStrings, filter, start);
        } else if (version == -1) {
            HashSeekConstants.outPrintLineSimple(output, "Versions of hash files during required period are different!! Please refine your search.");
        } else {
            HashSeekConstants.outPrintLineSimple(output, String.format("Unknown version '%s' of hash files!! This version is not supported yet.", version));
        }
	}

    private void seek_v1(Session session, PrintStream detailedOutput, PrintStream output, AppArguments appArguments, List<List<String>> stringsToSeek, List<String> filter, long start) throws IOException, ClassNotFoundException, ParserConfigurationException, XPathExpressionException, SAXException {
        //todo pn spojení výsledků dependent hledání na úrovni blockrecord, zachovat stávající seek dependency pro předávání parametrů hledání.
        if (!session.isNoeSeek()) {
            AuditFileEvaluator auditFileEvaluator = new AuditFileEvaluator();
            List<File> auditFilesToSeek = auditFileEvaluator.filesToSeek(appArguments);
            AbstractSeekResults auditSeekResults = new AuditSeekResults(auditFilesToSeek);
            auditSeekResults.setSeekLimit(session.getSeekLimit());
            if (session.isIncludeTimeLogs()) {
                TimeFileEvaluator timeFileEvaluator = new TimeFileEvaluator();
                List<File> timeFilesToSeek = timeFileEvaluator.filesToSeek(appArguments);
                AbstractSeekResults timeSeekResults = new TimeSeekResults(timeFilesToSeek);
                timeSeekResults.setSeekLimit(session.getSeekLimit());
                auditSeekResults.addDependentSeekResults(timeSeekResults);
            }
            if (session.isOnlineBPMSeek()) {
                BpmFileEvaluator bpmFileEvaluator = new BpmFileEvaluator();
                List<File> bpmFilesToSeek = bpmFileEvaluator.filesToSeek(appArguments);
                AbstractSeekResults bpmSeekResults = new BpmSeekResults(bpmFilesToSeek);
                bpmSeekResults.setSeekLimit(session.getSeekLimit());
                auditSeekResults.addDependentSeekResults(bpmSeekResults);
            }
            if (session.isB2bSeek()) {
                B2BFileEvaluator b2bFileEvaluator = new B2BFileEvaluator();
                List<File> b2bFilesToSeek = b2bFileEvaluator.filesToSeek(appArguments);
                AbstractSeekResults b2bSeekResults = new B2BSeekResults(b2bFilesToSeek);
                b2bSeekResults.setSeekLimit(session.getSeekLimit());
                auditSeekResults.addDependentSeekResults(b2bSeekResults);
            }
            //remote seek přidán Vojkovo metodou
            boolean append = false;
            if (session.isOnlineEsbSeek()) {
                Set<File> missedLastFiles = FileEvaluatorUtil.nonIndexedFiles(appArguments);
                localSeqSeek(stringsToSeek, missedLastFiles, appArguments, output, session);
                append = true;
            }
            auditSeekResults.runSeek(stringsToSeek, filter, output); //spustí i ostatní dependent
            if (session.isCsvSeek()) {
                auditSeekResults.reportCSV(new File(session.getFileName()), output, session, appArguments, append);
            } else {
                auditSeekResults.reportSortedResults(new File(session.getFileName()), output,session,appArguments, append);
            }
        }
        if (session.isNoeSeek()) {
            NoeFileEvaluator noeFileEvaluator = new NoeFileEvaluator();
            List<File> noeFilesToSeek = noeFileEvaluator.filesToSeek(appArguments);
            AbstractSeekResults noeSeekResults = new NoeSeekResults(noeFilesToSeek);
            noeSeekResults.setSeekLimit(session.getSeekLimit());
            noeSeekResults.runSeek(stringsToSeek, filter, output); //spustí i ostatní dependent
            noeSeekResults.reportSortedResults(new File(session.getFileName()), output,session,appArguments, false);
        }
        output.println(String.format("FINISHED in %s.", HashSeekConstants.formatedTimeMillis(System.currentTimeMillis() - start)));
    }

    private void localSeqSeek(List<List<String>> seekStringList, Set<File> missedFiles, AppArguments appArguments, PrintStream output, Session session) throws IOException {
        HashSeekConstants.outPrintLine(output, String.format("Sekvenční hledání:"));
        Set<String> seekStrings = new HashSet<String>();
        for (List<String> list : seekStringList) {
            for (String seekString : list) {
                seekStrings.add(seekString);
            }
        }
        Set<AbstractLogRecord> remotelogRecords = new HashSet<AbstractLogRecord>();
        for (File file : missedFiles) {
            HashSeekConstants.outPrintLine(output, String.format("Soubor : %s", file.getPath()));
            SeekableInputStream raf = new RandomSeekableInputStream(file, "r");
            RemoteEsbAuditSeek remoteSeek = new RemoteEsbAuditSeek(raf);
            remoteSeek.sequentialSeek(seekStrings, file.getPath());
            for (LogRecordAuditLog logRecordAuditLog : remoteSeek.getResults()) {
                remotelogRecords.add(logRecordAuditLog);
            }
        }
        HashSeekAuditLog hashSeekAuditLog = new HashSeekAuditLog();
        hashSeekAuditLog.setResults(remotelogRecords);
        hashSeekAuditLog.reportSortedResults(new File(session.getFileName()),appArguments, output);
    }

    private void remoteSeek(List<List<String>> seekStrings, Set<String> missedFiles, AppArguments appArguments, PrintStream output, Session session) throws ClassNotFoundException, IOException {
        Set<AbstractLogRecord> remotelogRecords = new HashSet<AbstractLogRecord>();
        HashSeekConstants.outPrintLine(output, String.format("Budou se prohledavat soubory na vzdalenych strojich:"));
        Set<SingleRemoteSeek> singleRemoteSeeks = new HashSet<SingleRemoteSeek>();
        for (String fileName : missedFiles) {
            singleRemoteSeeks.addAll(prepareRemoteSeeks(seekStrings, fileName, appArguments, output));
        }
        for (SingleRemoteSeek singleRemoteSeek : singleRemoteSeeks) {
            HashSeekConstants.outPrintLine(output, String.format("%s:%s <<<- '%s'", singleRemoteSeek.getHost(), singleRemoteSeek.getPort(), singleRemoteSeek.getRemoteMessage().getSeekParameters().getClientFileToSeek()));
        }
        for (SingleRemoteSeek singleRemoteSeek : singleRemoteSeeks) {
            Set<LogRecordAuditLog> logRecords = singleRemoteSeek.remoteSeekByClientFileName(output);
            if (logRecords != null) {
                remotelogRecords.addAll(logRecords);
            }
        }
        HashSeekAuditLog hashSeekAuditLog = new HashSeekAuditLog();
        hashSeekAuditLog.setResults(remotelogRecords);
        hashSeekAuditLog.reportSortedResults(new File(session.getFileName()),appArguments);
    }


    private Set<SingleRemoteSeek> prepareRemoteSeeks(List<List<String>> seekStrings, String fileName, AppArguments appArguments, PrintStream output) throws IOException, ClassNotFoundException {
        fileName = fileName.replaceFirst(".bgz","");
        Set<SingleRemoteSeek> singleRemoteSeeks = new HashSet<SingleRemoteSeek>();
        List<String> remoteSeekStrings = new LinkedList<String>();
        for (List<String> seekStringList : seekStrings) {
            for (String seekString : seekStringList) {
                remoteSeekStrings.add(seekString);
            }
        }
        SingleRemoteSeek singleRemoteSeek = new SingleRemoteSeek();
        if (appArguments.isSeekProd()) {
            singleRemoteSeek.initialize(remoteSeekStrings.toArray(new String[0]), fileName,output, AppProperties.PROD_PREFIX);
        }
        if (appArguments.isSeekPredprod()) {
            singleRemoteSeek.initialize(remoteSeekStrings.toArray(new String[0]), fileName, output, AppProperties.PREDPROD_PREFIX);
        }
        if (appArguments.isSeekTest()) {
            singleRemoteSeek.initialize(remoteSeekStrings.toArray(new String[0]), fileName, output, AppProperties.TEST_PREFIX);
        }
        if (singleRemoteSeek.isInitialized()) {
            singleRemoteSeeks.add(singleRemoteSeek);
        }
        return singleRemoteSeeks;
    }


    private void oldSeek(Session session, PrintStream detailedOutput, PrintStream output, AppArguments appArguments, List<String> seekedStrings, long start) throws IOException, ClassNotFoundException {
        boolean isFound=false;
        List<AbstractLogSeek> seekList=new ArrayList<AbstractLogSeek>();
        List<AbstractLogSeek> finishedSeekList=new ArrayList<AbstractLogSeek>();
        seekList.add(new HashSeekAuditLog(detailedOutput));
        seekList.add(new HashSeekTimeLog(detailedOutput));
        for (int i = 0; i < seekList.size(); i++) {
            AbstractLogSeek abstractLogSeek =  seekList.get(i);
            abstractLogSeek.initBeforeSeek(finishedSeekList);
            if (session.isHashFileSeekOnly()) {
                abstractLogSeek.seekHashOnly(seekedStrings.toArray(new String[0]), appArguments);
            } else {
                abstractLogSeek.seek(seekedStrings.toArray(new String[0]), appArguments);
            }
            isFound=isFound||abstractLogSeek.isFound();
            abstractLogSeek.reportSortedResults(new File(session.getFileName()),appArguments);
            finishedSeekList.add(abstractLogSeek);
        }

        HashSeekConstants.outPrintLineSimple(output, String.format("FINISHED in %s. %s", HashSeekConstants.formatedTime(System.currentTimeMillis() - start), isFound ? "FOUND" : "NOT found"));

        StringBuilder sb=new StringBuilder();
        sb.append("user:").append(session.getDomainUser()).append("-").append(session.getUserName());
        sb.append(";production:").append(session.isProd());
        sb.append(";predprod:").append(session.isPredprod());
        sb.append(";test:").append(session.isTest());
        sb.append(";DayFrom:").append(session.getSeekDay());
        sb.append(";DaysToSeek:").append(session.getHoursToSeek());
        int days=(int)((appArguments.getDateSeekFrom().getTime().getTime() - Calendar.getInstance().getTime().getTime()) / (1000 * 60 * 60 * 24));
        sb.append(";DaysFromNow:").append(days);
        sb.append(";isOnline:").append(session.isOnlineBPMSeek()||session.isOnlineEsbSeek());
        sb.append(";isFound:").append(isFound);
        int countBeaIds=0;
        for (int i = 0; i < seekList.size(); i++) {
            AbstractLogSeek abstractLogSeek =  seekList.get(i);
            if(abstractLogSeek.getBeaIds()!=null){
                for (Iterator<Map.Entry<File,Set<String>>> iterator = abstractLogSeek.getBeaIds().entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<File,Set<String>> next =  iterator.next();
                    if(next.getValue()!=null){
                        countBeaIds+= next.getValue().size();
                    }
                }
            }
        }
        sb.append(";countOfResult:").append(countBeaIds);
        loggerAccess.info(sb.toString());
    }

    private boolean notNum(String string) {
		try {
			Integer.valueOf(string);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean writeInteractiveParam(OutputStream os, BufferedReader bufferedReader, PrintWriter writer, String param) throws IOException, InterruptedException {
		String line = writeParamLineFromProcess(bufferedReader, os);
		if (line.contains("Chyba vstupu.")) {
			return false;
		}
		writeParam(os, writer, param);
		return true;
	}

	private void writeParam(OutputStream os, PrintWriter writer, String param) throws IOException, InterruptedException {
		os.write(String.format("%s\n", param).getBytes());
		os.flush();
		writer.println(param);
		writer.flush();
//        Thread.sleep(500);
	}

	private String writeParamLineFromProcess(BufferedReader input, OutputStream os) throws IOException {
		char[] cbuf = new char[10];
		String line = "";
		while (!line.contains(":")) {
			int numberReads = input.read(cbuf);
			String chunk = new String(cbuf, 0, numberReads);
			line = line.concat(chunk);
		}
		os.write(line.getBytes());
		os.flush();
		return line;
	}

	public static void main(String[] args) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("java");
		Map<String, String> env = pb.environment();
		pb.directory(new File("./"));
		pb.redirectErrorStream(true);
		Process p = pb.start();
		InputStream inputStream = p.getInputStream();
		BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = input.readLine()) != null) {
			LOGGER.info(line);
		}
	}

	public void run() {
		OutputStream os = exchange.getResponseBody();
		try {
			BufferedInputStream httpBin = new BufferedInputStream(exchange.getRequestBody());
			StringBuffer buf = new StringBuffer();
			while (httpBin.available() > 0 && buf.length() < 4096) { //zpracujeme max 4KB
				buf.append((char) httpBin.read());
			}
			URI requestURI = exchange.getRequestURI();
			String query = requestURI.getQuery();
			Session.outPrintLine(String.format("Received process command: '%s'", query));
			Session session = Session.sessionMap.get(query);
			if (session == null) {
				String message = String.format("Session '%s' does not exists, please go to /findFlow page.", query);
				os.write(message.getBytes());
				os.flush();
				os.close();
				return;
			}
			if (!session.isAuthenticated()) {
				AuthenticationHandler.authenticateRedirect(exchange, session);
				return;
			}
			exchange.sendResponseHeaders(200, 0);
            try {
                runSeek(os, query);
            } catch (Exception e) {
                e.printStackTrace();
                HashSeekConstants.outPrintLine(e.getMessage());
                LOGGER.error("run",e);
                LogInfoHandler.sendError(exchange, e);
            }
			os.close();
		} catch (Exception e) {
			try {
				exchange.sendResponseHeaders(200, 0);
				os.close();
			} catch (IOException e1) {
                LOGGER.error("run-catch",e);
                LOGGER.error("run-catch",e1);
                e.printStackTrace();
                e1.printStackTrace();
			}
            LOGGER.error("run-catch",e);
		} finally {
			parent.setNumberOfThreads(parent.getNumberOfThreads() - 1);
		}
	}

	private void deleteFile(String fileName) {
		fileName = fileName.replaceAll("[/\\/]", ""); //opatreni kdyby nekdo chtel neco mazat na filesystemu
		fileName = String.format("./%s.properties", fileName);
		File fileToDelete = new File(fileName);
		fileToDelete.delete();
	}

}
