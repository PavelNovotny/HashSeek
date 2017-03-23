package com.o2.cz.cip.hashseek.blockseek;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.CsvParams;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditBlockLogRecord;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditSeekResults;
import com.o2.cz.cip.hashseek.core.BlockSeek;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.http.Session;
import com.o2.cz.cip.hashseek.io.BgzSeekableInputStream;
import com.o2.cz.cip.hashseek.io.SeekableInputStream;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public abstract class AbstractSeekResults {
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractSeekResults.class);
    static org.slf4j.Logger loggerAccess= LoggerFactory.getLogger("hashseek.access");
    private List<AbstractBlockRecord> blockList;
    private SortInfo sortInfo; //určuje jak budou záznamy v této kolekci tříděny, zároveň je instance této třídy nasetována do členů kolekce blockList, aby mohli tito členové vyrobit správný porovnávací klíč.
    private List<AbstractSeekResults> dependentSeekResults; //pokud je závislost od předchozích výsledků hledáni, které se použijí pro nové hledání, a sort závislost (výsledky by měly být u sebe) např. Time logy jsou závislé od hledání v Audit (závislost hledání podle beaId audit logů)
    private AbstractSeekResults parentSeekResults; //v případě že je aktuální instance závislé hledání, tak je to jeho parent, pro lepší procházení stromu.
    public abstract BlockRecordFactory createBlockRecordFactory(); //poděděná třída vytvoří odpovídající blockRecordFactory pro vytvoření blockRecords pro zápis vyhledaných záznamů.
    public abstract List<List<String>> decorateSeekedStrings(List<List<String>> seekedStrings); //poděděná třída vytvoří odpovídající blockRecordFactory pro vytvoření blockRecords pro zápis vyhledaných záznamů.
    private BlockRecordFactory blockRecordFactory;
    private List<File> seekedFiles;
    private int seekLimit = 100;

    public AbstractSeekResults(List<File> seekedFiles) {
        this.blockList = new LinkedList<AbstractBlockRecord>();
        this.sortInfo = new SortInfo(SortInfo.SortBy.ENTRYTIME, SortInfo.SortDirection.ASC);
        this.blockRecordFactory = createBlockRecordFactory();
        this.seekedFiles = seekedFiles;
    }

    public void runSeek(List<List<String>> seekedStrings, List<String> filter, PrintStream output) {
        if (parentSeekResults != null) {
            throw new RuntimeException("Only seek by parent is allowed in dependent SeekResults, pls call the method on parent");
        }
        runSeekByParent(decorateSeekedStrings(seekedStrings), filter, output); //pokud mám prázdného parenta tak hledám sám místo něj.
    }

    protected void runSeekByParent(List<List<String>> seekedStrings, List<String> filter, PrintStream output) { //protected je proto, aby se zabránilo přímému volání, a volalo se jenom přes parenta. Není to úplně na 100%, ale předpokládáme, že volající třída bude v jiné package.
        HashSeekConstants.outPrintLineSimple(output, String.format("The results of '%s' can be restricted.  Maximum processed blocks is %s", this.getClass().getName(),  seekLimit));
        BlockSeek blockSeek = new BlockSeek();
        for (File seekedFile : seekedFiles) {
            try {
                Map<Long,Integer> positions = blockSeek.seekForPositions(seekedStrings, seekedFile, seekLimit, output);
                HashSeekConstants.outPrintLineSimple(output, String.format("Found %s positions for '%s'", positions.size(), seekedFile.getAbsolutePath()));
                //RandomAccessFile raf = new RandomAccessFile(seekedFile,"r");
                SeekableInputStream raf = new BgzSeekableInputStream(seekedFile);
                for (Long position : positions.keySet()) {
                    Integer blockSize = positions.get(position);
                    raf.seek(position);
                    AbstractBlockRecord blockRecord = blockRecordFactory.getBlockRecordInstance(raf.readRaw(blockSize), seekedFile, position);
                    blockRecord.setSortInfo(this.sortInfo);
                    addFilteredRecord(blockRecord, filter, output);
                }
                raf.close();
            } catch (IOException e) {
                LOGGER.error("error during seek in file:"+seekedFile.getAbsoluteFile(),e);
                e.printStackTrace();
            }
        }
        if (dependentSeekResults != null) {
            for (AbstractSeekResults dependentSeekResult : dependentSeekResults) {
                dependentSeekResult.runSeekByParent(dependentSeekResult.decorateSeekedStrings(seekedStrings), filter, output); //dependent je zodpovědný za získání dalších seek stringů z výsledku hledání parenta.
            }
        }
    }

    protected void addFilteredRecord(AbstractBlockRecord blockRecord, List<String> filter, PrintStream output) {
        if (filter == null) {
            this.blockList.add(blockRecord);
        } else {
            for (String filterString : filter) {
                if (filterString!= null && !"".equals(filterString.trim())) {
                    Pattern pattern;
                    try {
                        pattern = Pattern.compile(filterString);
                    } catch (Exception e) {
                        HashSeekConstants.outPrintLineSimple(output, String.format("Pattern '%s' cannot be compiled.  Please check it. The filter for this pattern cannot be applied.", filterString));
                        continue;
                    }
                    Matcher matcher = pattern.matcher(blockRecord.rawData);
                    if (matcher.find()) {
                        this.blockList.add(blockRecord);
                        return;
                    }
                }
            }
        }
    }

    public void sort() {
        Collections.sort(blockList);
    }

    public void reportData() {
        for (AbstractBlockRecord blockRecord : blockList) {
            LOGGER.debug(String.format("%s", blockRecord.getRawData()));
//            System.out.println(String.format("%s", blockRecord.getRawData()));
        }
        if (dependentSeekResults != null) {
            for (AbstractSeekResults dependentSeekResult : dependentSeekResults) {
                dependentSeekResult.reportData();
            }
        }
    }

    public void reportTimeStamps() {
        for (AbstractBlockRecord blockRecord : blockList) {
            LOGGER.debug(String.format("%s", blockRecord.getTimeStamp()));
//            System.out.println(String.format("%s", blockRecord.getRawData()));
        }
    }

    public void reportCSV(File report, PrintStream messageOutput,Session session,AppArguments appArguments, boolean append) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        int resultSize=0;
        PrintStream out;
        if (append) {
            out = new PrintStream(new FileOutputStream(report, append));
        } else {
            out = new PrintStream(report,"UTF-8");
        }
        sort();
        reportCSVResults(out, session.getCsvParams(), session.getNamespaces());
        out.close();
        boolean isFound=false;
        StringBuilder foundText=new StringBuilder();
        if (!blockList.isEmpty()) {
            foundText.append("CSV results").append(":").append(blockList.size()).append("\n");
            resultSize+=blockList.size();
            isFound=true;
        }
        if(isFound){
            messageOutput.println("FOUND");
        }else{
            messageOutput.println("NOT found");
        }
        messageOutput.println(foundText);
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
        sb.append(";countOfResult:").append(resultSize);
        loggerAccess.info(sb.toString());
    }

    public void reportSortedResults(File report, PrintStream messageOutput,Session session,AppArguments appArguments, boolean append) throws IOException {
        int resultSize=0;
        PrintStream out;
        if (append) {
            out = new PrintStream(new FileOutputStream(report, append));
        } else {
            out = new PrintStream(report,"UTF-8");
        }
        sort();
        reportHeaderResults(out);
        out.printf("%s\n", new String(new char[80]).replace("\0", "-"));
        out.printf("%s\n", "");
        reportBodyResults(out);
        out.close();
        boolean isFound=false;
        StringBuilder foundedText=new StringBuilder();
        if (!blockList.isEmpty()) {
            foundedText.append(this.getClass().getSimpleName()).append(":").append(blockList.size()).append("\n");
            resultSize+=blockList.size();
            isFound=true;
        }
        if(dependentSeekResults!=null && dependentSeekResults.size()>0){
            for (int i = 0; i < dependentSeekResults.size(); i++) {
                AbstractSeekResults abstractSeekResults =  dependentSeekResults.get(i);
                if(!abstractSeekResults.blockList.isEmpty()){
                    foundedText.append(abstractSeekResults.getClass().getSimpleName()).append(":").append(abstractSeekResults.blockList.size()).append("\n");
                    resultSize+=abstractSeekResults.blockList.size();
                    isFound=true;
                }
            }
        }
        if(isFound){
            messageOutput.println("FOUND");
        }else{
            messageOutput.println("NOT found");
        }
        messageOutput.println(foundedText);
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
        sb.append(";countOfResult:").append(resultSize);
        loggerAccess.info(sb.toString());
    }

    protected void reportHeaderResults(PrintStream out) throws FileNotFoundException {
        for (AbstractBlockRecord blockRecord : blockList) {
            out.println(blockRecord.getHeaderData());
        }
        if (dependentSeekResults != null) {
            for (AbstractSeekResults dependentSeekResult : dependentSeekResults) {
                dependentSeekResult.reportHeaderResults(out);
            }
        }
    }

    protected void reportCSVResults(PrintStream out, List<CsvParams> csvParams, Map<String, String> namespaces) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        StringBuilder outputLine= new StringBuilder();
        outputLine.append("date;time;beaid");
        for (CsvParams csvParam : csvParams) {
            if (!"".equals(csvParam.getService().trim())) {
                outputLine.append(String.format(";%s", csvParam.getService()));
            }
            for (String xPathExpr : csvParam.getxPath()) {
                if (!"".equals(xPathExpr.trim())) {
                    outputLine.append(String.format(";%s", xPathExpr));
                }
            }
        }
        out.println(outputLine);
        for (AbstractBlockRecord blockRecord : blockList) {
            Scanner scanner = new Scanner(blockRecord.rawData);
            StringBuilder xml = new StringBuilder();
            List<CsvParams> alreadyProcessed = new ArrayList<CsvParams>();
            outputLine= new StringBuilder();
            String[] prevTokens = new String[9];
            boolean first = true;
            boolean print = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(";", 9);
                if (tokens.length == 9 && line.matches("^\\d{8};\\d{2}:\\d{2}:\\d{2}.*$")) { //bea id line, můžeme zpracovat předchozí xml a beaId line
                    if (first) {
                        outputLine.append(String.format("%s;%s;%s", tokens[0], tokens[1], tokens[3]));
                        first = false;
                    } else {
                        print = buildCsvLine(csvParams, namespaces, xml, outputLine, prevTokens, alreadyProcessed) || print;
                    }
                    xml = new StringBuilder();
                    xml.append(tokens[8]);
                    prevTokens = tokens;
                } else {
                    xml.append(line);
                }
            }
            print = buildCsvLine(csvParams, namespaces, xml, outputLine, prevTokens, alreadyProcessed) || print;
            if (print) {
                out.println(outputLine);
            }
            scanner.close();
        }
    }

    private boolean buildCsvLine(List<CsvParams> csvParams, Map<String, String> namespaces, StringBuilder xml, StringBuilder outputLine, String[] prevTokens, List<CsvParams> processedParams) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        String service;
        boolean print = false;
        service = prevTokens[5].replaceAll("[$]", "/");
        for (CsvParams csvParam : csvParams) {
            if (processedParams.contains(csvParam) || "".equals(csvParam.getService().trim())) { //already processed or empty param
                continue;
            }
            if (service.contains(csvParam.getService()) && (("REQUEST".equals(prevTokens[7]) == csvParam.isRequest()))) {
                outputLine.append(";").append(prevTokens[7]);
                print = true;
                processedParams.add(csvParam);
                for (String xPathExpr : csvParam.getxPath()) {
                    if (!"".equals(xPathExpr.trim())) {
                        try {
                            outputLine.append(";").append(XPathUtil.xPath(xPathExpr, xml.toString(), namespaces));
                        } catch (Exception e) {
                            outputLine.append(";").append("!!ERROR!!");
                        }
                    }
                }
            }
        }
        return print;
    }

    public static void main (String[] args) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        test();
    }

    public static void test() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        PrintStream out = System.out;
        List<AbstractBlockRecord> blockList = new ArrayList<AbstractBlockRecord>();
        String xml =
                "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;BusinessService$RADIUS_RoamingDataPackagesManagement$1.0$BusinessServices$bs_RADIUS_RoamingDataPackagesManagement_WS;unknown;RESPONSE;<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><queryPackageResponse xmlns=\"http://cz.o2.com/systems/provisioning/RADIUS/RADIUS_RoamingDataPackagesManagement/1.0\"><response><errorCode>200</errorCode><errorCategory>OK</errorCategory><errorDescription>Request accepted.</errorDescription></response><package><packageID>985786</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>250</valueMB><activationDate>2015-03-22T04:55:40.373</activationDate><enableDate>2015-03-22T04:58:01.743</enableDate><expirationDate>2015-04-21T05:58:01.743</expirationDate><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>258566</currentAmountKB></package><package><packageID>977201</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>150</valueMB><activationDate>2015-03-15T14:21:20.324</activationDate><enableDate>2015-03-15T14:29:32.861</enableDate><expirationDate>2015-04-14T15:29:32.861</expirationDate><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>155140</currentAmountKB></package><package><packageID>975059</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>25</valueMB><activationDate>2015-03-12T21:23:14.360</activationDate><enableDate>2015-03-13T15:53:56.750</enableDate><expirationDate>2015-04-12T16:53:56.750</expirationDate><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>26600</currentAmountKB></package><package><packageID>1027384</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>250</valueMB><activationDate>2015-04-29T11:10:09.517</activationDate><enableDate>2015-04-29T11:14:45.165</enableDate><expirationDate>2015-05-29T11:14:45.165</expirationDate><packageConsumptionStatus>IN_CONSUMPTION</packageConsumptionStatus><currentAmountKB>170461</currentAmountKB></package><package><packageID>1002683</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>250</valueMB><activationDate>2015-04-04T12:55:33.345</activationDate><enableDate>2015-04-04T12:57:31.062</enableDate><expirationDate>2015-05-04T12:57:31.062</expirationDate><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>258568</currentAmountKB></package><package><packageID>1017514</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>250</valueMB><activationDate>2015-04-18T06:41:58.387</activationDate><enableDate>2015-04-18T06:46:01.797</enableDate><expirationDate>2015-05-18T06:46:01.797</expirationDate><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>258566</currentAmountKB></package><package><packageID>1025459</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><expirationDays>30</expirationDays><zone>WORLD</zone><valueMB>250</valueMB><activationDate>2015-04-27T10:41:52.515</activationDate><enableDate>2015-04-27T10:46:41.248</enableDate><expirationDate>2015-05-27T10:46:41.248</expirationDate><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>258562</currentAmountKB></package><package><packageID>1035876</packageID><provisioningID>MSP005234874</provisioningID><IMSI>230023701284557</IMSI><packageType>RoamingDataPackage</packageType><packageSubtype>oneOff</packageSubtype><notification>true</notification><zone>WORLD</zone><valueMB>250</valueMB><activationDate>2015-05-04T11:16:02.272</activationDate><packageConsumptionStatus>NEW</packageConsumptionStatus><currentAmountKB>0</currentAmountKB></package><package><packageID>230023701284557</packageID><provisioningID>230023701284557</provisioningID><IMSI>230023701284557</IMSI><packageType>EUCap</packageType><zone>EU</zone><valueMB>250</valueMB><packageConsumptionStatus>CONSUMED</packageConsumptionStatus><currentAmountKB>256012</currentAmountKB></package></queryPackageResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>\n" +
                        "20150511;00:00:00;{ '87' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;BusinessService$MobileContractInfo$2.0$BusinessServices$bs_MobileContractInfo_WS;unknown;REQUEST;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:GetMSISDNWorldRequest xmlns:ns=\"http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileContractInfo/2.0\"><int:requestHeader xmlns:int=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\"><int:messageId>1-AYUFMK-31-1431295199</int:messageId><int:timestamp>2015-05-11T00:00:00.120+02:00</int:timestamp><int:correlationId/><int:trackingInfo><int:businessId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:businessId><int:conversationId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:conversationId><int:userId><int:value>JO008957</int:value><int:meaning>Login Id</int:meaning></int:userId></int:trackingInfo><int:consumerId>IOM-SIEBEL</int:consumerId><int:providerId>CIPESB</int:providerId></int:requestHeader><ns:requestBody><ns:imsi>230023701284557</ns:imsi></ns:requestBody></ns:GetMSISDNWorldRequest></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;BusinessService$MobileContractInfo$2.0$BusinessServices$bs_MobileContractInfo_WS;unknown;RESPONSE;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:GetMSISDNWorldResponse xmlns:ns=\"http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileContractInfo/2.0\"><int:responseHeader xmlns:int=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\"><int:messageId>1-AYUFMK-31-1431295199</int:messageId><int:timestamp>2015-05-11T00:00:00.218+02:00</int:timestamp><int:correlationId/><int:trackingInfo><int:businessId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:businessId><int:conversationId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:conversationId><int:userId><int:value>JO008957</int:value><int:meaning>Login Id</int:meaning></int:userId></int:trackingInfo><int:providerId>MODS</int:providerId><int:consumerId>IOM-SIEBEL</int:consumerId><int:trackingStatus><int:code>0</int:code><int:message>postpaid_SBL (New world)</int:message></int:trackingStatus></int:responseHeader><ns:responseBody><ns:resultCode>4</ns:resultCode><ns:resultMessage>postpaid_SBL (New world)</ns:resultMessage><ns:servicePointId>MSP005234874</ns:servicePointId><ns:msisdn>721190947</ns:msisdn><ns:imsi>230023701284557</ns:imsi></ns:responseBody></ns:GetMSISDNWorldResponse></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '87' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;BusinessService$MODS_p_get_lbo_history$1.0$BusinessServices$bs_p_get_lbo_history_DBT;unknown;REQUEST;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><mods:dbTransportRequest xmlns:mods=\"http://cz.o2.com/cip/1.0/MODS_p_get_lbo_history\"><mods:inputParams><mods:S_IMSI>230023701284557</mods:S_IMSI></mods:inputParams></mods:dbTransportRequest></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;BusinessService$MODS_p_get_lbo_history$1.0$BusinessServices$bs_p_get_lbo_history_DBT;unknown;RESPONSE;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header/><soapenv:Body><ns:dbTransportResponse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns=\"http://cz.o2.com/cip/1.0/MODS_p_get_lbo_history\"><ns:outputParams><ns:T_LBO_HISTORY/><ns:N_RESULT_CODE>1</ns:N_RESULT_CODE><ns:S_RESULT_MSG>NO_DATA_FOUND</ns:S_RESULT_MSG></ns:outputParams></ns:dbTransportResponse></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;ProxyService$DataPackagesInfo$1.0$ProxyServices$getRoamingDataPackagesInfo_Local;unknown;RESPONSE;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ns:GetRoamingDataPackagesInfoResponse xmlns:ns=\"http://cz.o2.com/cip/svc/servicemgmt/DataPackagesInfo/1.0\"><int:responseHeader xmlns:int=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\"><int:messageId>1-AYUFMK-31-1431295199</int:messageId><int:timestamp>2015-05-10T23:59:59Z</int:timestamp><int:correlationId/><int:trackingInfo><int:businessId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:businessId><int:conversationId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:conversationId><int:userId><int:value>JO008957</int:value><int:meaning>Login Id</int:meaning></int:userId></int:trackingInfo><int:providerId>RADIUS</int:providerId><int:consumerId>IOM-SIEBEL</int:consumerId><int:trackingStatus><int:code>0</int:code></int:trackingStatus></int:responseHeader><ns:responseBody><ns:errorCode>200</ns:errorCode><ns:errorCategory>OK</ns:errorCategory><ns:errorDescription>Request accepted.</ns:errorDescription><ns:dataPackagesInfoList><ns:dataPackageInfo><ns:packageId>985786</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-03-22T04:55:40.373</ns:activationDate><ns:enableDate>2015-03-22T04:58:01.743</ns:enableDate><ns:expirationDate>2015-04-21T05:58:01.743</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258566</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>977201</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>150</ns:valueMB><ns:activationDate>2015-03-15T14:21:20.324</ns:activationDate><ns:enableDate>2015-03-15T14:29:32.861</ns:enableDate><ns:expirationDate>2015-04-14T15:29:32.861</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>155140</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>975059</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>25</ns:valueMB><ns:activationDate>2015-03-12T21:23:14.360</ns:activationDate><ns:enableDate>2015-03-13T15:53:56.750</ns:enableDate><ns:expirationDate>2015-04-12T16:53:56.750</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>26600</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1027384</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-29T11:10:09.517</ns:activationDate><ns:enableDate>2015-04-29T11:14:45.165</ns:enableDate><ns:expirationDate>2015-05-29T11:14:45.165</ns:expirationDate><ns:packageConsumptionStatus>V u≈æ√≠v√°n√≠</ns:packageConsumptionStatus><ns:currentAmountKB>170461</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1002683</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-04T12:55:33.345</ns:activationDate><ns:enableDate>2015-04-04T12:57:31.062</ns:enableDate><ns:expirationDate>2015-05-04T12:57:31.062</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258568</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1017514</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-18T06:41:58.387</ns:activationDate><ns:enableDate>2015-04-18T06:46:01.797</ns:enableDate><ns:expirationDate>2015-05-18T06:46:01.797</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258566</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1025459</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-27T10:41:52.515</ns:activationDate><ns:enableDate>2015-04-27T10:46:41.248</ns:enableDate><ns:expirationDate>2015-05-27T10:46:41.248</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258562</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1035876</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-05-04T11:16:02.272</ns:activationDate><ns:packageConsumptionStatus>Nov√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>0</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>230023701284557</ns:packageId><ns:provisioningId>230023701284557</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>EUCap</ns:packageType><ns:zone>EU</ns:zone><ns:valueMB>250</ns:valueMB><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>256012</ns:currentAmountKB></ns:dataPackageInfo></ns:dataPackagesInfoList></ns:responseBody></ns:GetRoamingDataPackagesInfoResponse>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27dd;1-AYUFMK-31-1431295199;ProxyService$DataPackagesInfo$1.0$ProxyServices$DataPackagesInfo_WS;unknown;RESPONSE;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:GetRoamingDataPackagesInfoResponse xmlns:ns=\"http://cz.o2.com/cip/svc/servicemgmt/DataPackagesInfo/1.0\"><int:responseHeader xmlns:int=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\"><int:messageId>1-AYUFMK-31-1431295199</int:messageId><int:timestamp>2015-05-10T23:59:59Z</int:timestamp><int:correlationId/><int:trackingInfo><int:businessId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:businessId><int:conversationId><int:value>230023701284557</int:value><int:meaning>Imsi</int:meaning></int:conversationId><int:userId><int:value>JO008957</int:value><int:meaning>Login Id</int:meaning></int:userId></int:trackingInfo><int:providerId>RADIUS</int:providerId><int:consumerId>IOM-SIEBEL</int:consumerId><int:trackingStatus><int:code>0</int:code></int:trackingStatus></int:responseHeader><ns:responseBody><ns:errorCode>200</ns:errorCode><ns:errorCategory>OK</ns:errorCategory><ns:errorDescription>Request accepted.</ns:errorDescription><ns:dataPackagesInfoList><ns:dataPackageInfo><ns:packageId>985786</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-03-22T04:55:40.373</ns:activationDate><ns:enableDate>2015-03-22T04:58:01.743</ns:enableDate><ns:expirationDate>2015-04-21T05:58:01.743</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258566</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>977201</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>150</ns:valueMB><ns:activationDate>2015-03-15T14:21:20.324</ns:activationDate><ns:enableDate>2015-03-15T14:29:32.861</ns:enableDate><ns:expirationDate>2015-04-14T15:29:32.861</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>155140</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>975059</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>25</ns:valueMB><ns:activationDate>2015-03-12T21:23:14.360</ns:activationDate><ns:enableDate>2015-03-13T15:53:56.750</ns:enableDate><ns:expirationDate>2015-04-12T16:53:56.750</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>26600</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1027384</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-29T11:10:09.517</ns:activationDate><ns:enableDate>2015-04-29T11:14:45.165</ns:enableDate><ns:expirationDate>2015-05-29T11:14:45.165</ns:expirationDate><ns:packageConsumptionStatus>V u≈æ√≠v√°n√≠</ns:packageConsumptionStatus><ns:currentAmountKB>170461</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1002683</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-04T12:55:33.345</ns:activationDate><ns:enableDate>2015-04-04T12:57:31.062</ns:enableDate><ns:expirationDate>2015-05-04T12:57:31.062</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258568</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1017514</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-18T06:41:58.387</ns:activationDate><ns:enableDate>2015-04-18T06:46:01.797</ns:enableDate><ns:expirationDate>2015-05-18T06:46:01.797</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258566</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1025459</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:expirationDays>30</ns:expirationDays><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-04-27T10:41:52.515</ns:activationDate><ns:enableDate>2015-04-27T10:46:41.248</ns:enableDate><ns:expirationDate>2015-05-27T10:46:41.248</ns:expirationDate><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>258562</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>1035876</ns:packageId><ns:provisioningId>MSP005234874</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>RoamingDataPackage</ns:packageType><ns:packageSubtype>oneOff</ns:packageSubtype><ns:notification>true</ns:notification><ns:zone>WORLD</ns:zone><ns:valueMB>250</ns:valueMB><ns:activationDate>2015-05-04T11:16:02.272</ns:activationDate><ns:packageConsumptionStatus>Nov√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>0</ns:currentAmountKB></ns:dataPackageInfo><ns:dataPackageInfo><ns:packageId>230023701284557</ns:packageId><ns:provisioningId>230023701284557</ns:provisioningId><ns:customerIdentifier><ns:imsi>230023701284557</ns:imsi></ns:customerIdentifier><ns:packageType>EUCap</ns:packageType><ns:zone>EU</ns:zone><ns:valueMB>250</ns:valueMB><ns:packageConsumptionStatus>Vyƒçerpan√Ω</ns:packageConsumptionStatus><ns:currentAmountKB>256012</ns:currentAmountKB></ns:dataPackageInfo></ns:dataPackagesInfoList></ns:responseBody></ns:GetRoamingDataPackagesInfoResponse></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27d7;null;ProxyService$MobileCustomerInfo$1.0$ProxyServices$MobileCustomerInfo_HTTS;null;REQUEST;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:VerifyMobileCustomerRequest xmlns=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\" xmlns:ns2=\"http://cz.o2.com/cip-b2b/svc/customermgmt/contractmgmt/CIP-B2B_MobileCustomerInfo/1.0\" xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileCustomerInfo/1.0\"><requestHeader><messageId>76721f4f83468af976f34ee55edbe63b</messageId><timestamp>2015-05-11T00:00:00.471+02:00</timestamp><consumerId>o2ExtraVyhody</consumerId></requestHeader><ns:requestBody><ns:MSISDN>+420606119595</ns:MSISDN></ns:requestBody></ns:VerifyMobileCustomerRequest></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27d7;76721f4f83468af976f34ee55edbe63b;ProxyService$MobileCustomerInfo$1.0$ProxyServices$verifyMobileCustomer_Local;unknown;REQUEST;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ns:VerifyMobileCustomerRequest xmlns=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\" xmlns:ns2=\"http://cz.o2.com/cip-b2b/svc/customermgmt/contractmgmt/CIP-B2B_MobileCustomerInfo/1.0\" xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\"http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileCustomerInfo/1.0\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><requestHeader><messageId>76721f4f83468af976f34ee55edbe63b</messageId><timestamp>2015-05-11T00:00:00.471+02:00</timestamp><consumerId>o2ExtraVyhody</consumerId></requestHeader><ns:requestBody><ns:MSISDN>+420606119595</ns:MSISDN></ns:requestBody></ns:VerifyMobileCustomerRequest>\n" +
                        "20150511;00:00:00;{ '99' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27d7;76721f4f83468af976f34ee55edbe63b;BusinessService$MobileContractInfo$2.0$BusinessServices$bs_MobileContractInfo_WS;unknown;REQUEST;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:GetMSISDNWorldRequest xmlns:ns=\"http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileContractInfo/2.0\"><int:requestHeader xmlns:int=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\"><int:messageId>76721f4f83468af976f34ee55edbe63b</int:messageId><int:timestamp>2015-05-11T00:00:00.471+02:00</int:timestamp><int:consumerId>o2ExtraVyhody</int:consumerId></int:requestHeader><ns:requestBody><ns:msisdn>+420606119595</ns:msisdn></ns:requestBody></ns:GetMSISDNWorldRequest></soapenv:Body></soapenv:Envelope>\n" +
                        "20150511;00:00:00;{ '60' for queue: 'weblogic.kernel.Default (self-tuning)',5,Pooled Threads};4010671447608958602--5c62c265.14d3f98e861.-27d7;76721f4f83468af976f34ee55edbe63b;BusinessService$MobileContractInfo$2.0$BusinessServices$bs_MobileContractInfo_WS;unknown;RESPONSE;<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:GetMSISDNWorldResponse xmlns:ns=\"http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileContractInfo/2.0\"><int:responseHeader xmlns:int=\"http://cz.o2.com/cip/svc/IntegrationMessage-2.0\"><int:messageId>76721f4f83468af976f34ee55edbe63b</int:messageId><int:timestamp>2015-05-11T00:00:00.776+02:00</int:timestamp><int:providerId>MODS</int:providerId><int:consumerId>o2ExtraVyhody</int:consumerId><int:trackingStatus><int:code>0</int:code><int:message>postpaid_SBL (New world)</int:message></int:trackingStatus></int:responseHeader><ns:responseBody><ns:resultCode>4</ns:resultCode><ns:resultMessage>postpaid_SBL (New world)</ns:resultMessage><ns:servicePointId>MSP002591078</ns:servicePointId><ns:msisdn>606119595</ns:msisdn><ns:imsi>230022900116845</ns:imsi></ns:responseBody></ns:GetMSISDNWorldResponse></soapenv:Body></soapenv:Envelope>\n"
        ;

        AbstractBlockRecord blockRecord = new AuditBlockLogRecord(xml, null, 0L);
        blockList.add(blockRecord);
        List<CsvParams> csvParamsList = new ArrayList<CsvParams>();
        CsvParams csvParam = new CsvParams();
        csvParamsList.add(csvParam);
        csvParam.setRequest(true);
        csvParam.setService("bs_MobileContractInfo_WS");
        csvParam.setXPath(0, "//int:messageId");
        csvParam.setXPath(1, "//nsp:imsi");
        csvParam = new CsvParams();
        csvParamsList.add(csvParam);
        csvParam.setRequest(false);
        csvParam.setService("DataPackagesInfo_WS");
        csvParam.setXPath(0, "//int:messageId");
        csvParam.setXPath(1, "//pack:errorCategory");
        AbstractSeekResults seekResults = new AuditSeekResults(null);
        seekResults.blockList = blockList;
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("int", "http://cz.o2.com/cip/svc/IntegrationMessage-2.0");
        namespaces.put("nsp", "http://cz.o2.com/cip/svc/customermgmt/contractmgmt/MobileContractInfo/2.0");
        namespaces.put("pack", "http://cz.o2.com/cip/svc/servicemgmt/DataPackagesInfo/1.0");
        seekResults.reportCSVResults(out, csvParamsList, namespaces);
    }

    protected void reportBodyResults(PrintStream out) throws FileNotFoundException {
        for (AbstractBlockRecord blockRecord : blockList) {
            out.println(blockRecord.getFormattedData());
        }
        if (dependentSeekResults != null) {
            for (AbstractSeekResults dependentSeekResult : dependentSeekResults) {
                dependentSeekResult.reportBodyResults(out);
            }
        }
    }

    public void reportFormattedData() {
        for (AbstractBlockRecord blockRecord : blockList) {
            LOGGER.debug(String.format("%s", blockRecord.getFormattedData()));
//            System.out.println(String.format("%s", blockRecord.getRawData()));
        }
        if (dependentSeekResults != null) {
            for (AbstractSeekResults dependentSeekResult : dependentSeekResults) {
                dependentSeekResult.reportFormattedData();
            }
        }
    }

    public void reportHeaderData() {
        for (AbstractBlockRecord blockRecord : blockList) {
            LOGGER.debug(String.format("%s", blockRecord.getHeaderData()));
//            System.out.println(String.format("%s", blockRecord.getRawData()));
        }
        if (dependentSeekResults != null) {
            for (AbstractSeekResults dependentSeekResult : dependentSeekResults) {
                dependentSeekResult.reportHeaderData();
            }
        }
    }

    public void addDependentSeekResults(AbstractSeekResults dependentSeekResult) {
        if (this.dependentSeekResults == null) {
            this.dependentSeekResults = new LinkedList<AbstractSeekResults>();
        }
        dependentSeekResults.add(dependentSeekResult);
        dependentSeekResult.setParentSeekResults(this);
    }

    public void setParentSeekResults(AbstractSeekResults parentSeekResults) {
        this.parentSeekResults = parentSeekResults;
    }

    public AbstractSeekResults getParentSeekResults() {
        return parentSeekResults;
    }

    public List<AbstractBlockRecord> getBlockList() {
        return blockList;
    }

    public void setSeekLimit(int seekLimit) {
        this.seekLimit = seekLimit;
    }

}