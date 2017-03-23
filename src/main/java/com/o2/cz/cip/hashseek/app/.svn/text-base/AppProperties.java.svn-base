package com.o2.cz.cip.hashseek.app;

import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Pavel
 * Date: 29.3.13 14:01
 */
public class AppProperties {
    static Logger logger= LoggerFactory.getLogger(AppProperties.class);

    public static final String HASH_SEEK_PROPERTIES = "./HashSeek.properties";
    private static final String FILES_TO_HASH_LOCATION_PREFIX = "filesTohash.location";

    public static final String PROD_PREFIX = "prod";
    public static final String PREDPROD_PREFIX = "predprod";
    public static final String TEST_PREFIX = "test";

    public static final String LOG_LOCATION ="logLocation";
    public static final String BGZ_LOCATION ="bgzLocation";
    public static final String HASH_LOCATION ="hashLocation";

    public static final String PREDPROD_LOG_LOCATION_PREFIX = PREDPROD_PREFIX+"."+LOG_LOCATION;
    public static final String PROD_LOG_LOCATION_PREFIX = PROD_PREFIX+"."+LOG_LOCATION;
    public static final String TEST_LOG_LOCATION_PREFIX = TEST_PREFIX+"."+LOG_LOCATION;

    public static final String LOG_LOCATION_PREFIX = "log."+LOG_LOCATION;
    private static final String LOG_SERVER_NUMBER_PREFIX = "log.server.number";
    private static final String INDEX_PROCESSS_XMS = "indexProcesssXms";
    private static final String INDEX_PROCESSS_XMX = "indexProcesssXmx";
    private static final String PREDPROD_MAX_INDEX_DEPTH = "predprodMaxIndexDepth";
    private static final String DEFAULT_MAX_INDEX_DEPTH = "defaultMaxIndexDepth";
    private static final String HASH_FILE_LOCATION = "hashFileLocation";
    private static final String SERVER_REMOTE_PORT = "remote.port";
    private static final String ESB_REMOTE_PORT_PREFIX = "esb.remote.port";
    private static final String ESB_REMOTE_HOST_PREFIX = "esb.remote.host";
    public static final String ESB_SERVER_NUMBER_PREFIX = "esb.server.number";
    private static final String LOG_DOMAIN_PREFIX = "log.domain";
    public static final String LAST_LOG = "lastLog_s9(from all servers)";

    public static final String REMOTE_HOST_SUFFIX = "host";
    public static final String REMOTE_PORT_SUFFIX = "port";

    private static Properties properties;
    private static long lastTimeofLoadingProperties=0;
    private static final long REFRESH_TIMEOF_PROPERTIES = 10*60*1000; //5 minut
    private static Calendar fileDate = Calendar.getInstance();
    private static final long MAX_DAYS_TO_INDEX_BACKWARDS = 90; //dny
    private static final long MAX_TIME_TO_INDEX_BACKWARDS = MAX_DAYS_TO_INDEX_BACKWARDS*24*60*60*1000; //dny v milisekundach

    public final static String BGZ_BLOCK_SIZE ="bgz.block.size";
    public final static String BGZ_INDEX_BUFFER_SIZE ="bgz.index.buffer.size";
    public static final String PROD_ELIGIBLE_PREFIX = "prod.eligible";
    public static final String LIMIT_ELIGIBLE_PREFIX = "seekLimit.eligible";
    public static final String NOE_ELIGIBLE_PREFIX = "noe.eligible";
    public static final String SESSION_ELIGIBLE_PREFIX = "showSession.eligible";

    /**
     * mozne hodnoty
     * @link com.o2.cz.cip.hashseek.core.HashSeekConstants.OLD_INDEXER_TYPE
     * @link com.o2.cz.cip.hashseek.core.HashSeekConstants.BLOCK_INDEXER_TYPE
     */
    public final static String INDEXER_TYPE_KEY ="indexer";

    public final static String TRANSFORMER_NIO_BUFFER_SIZE_KEY ="transformer.nio.buffer.size";
    public final static String TRANSFORMER_MAX_TMP_FILES_KEY ="transformer.max.tmp.files";

    //obsahuje adresare z klicu obsahujicich v nazvu 'logLocation'
    public static Map<String,String> locationKeyMap=new HashMap<String, String>();

    public AppProperties() throws IOException {
    }

    public static String getLogLocation(String domain) {
        if (HashSeekConstants.PREDPROD.equals(domain)) {
            return getValue(PREDPROD_LOG_LOCATION_PREFIX + ".1");
        }
        if (HashSeekConstants.PROD.equals(domain)) {
            return getValue(PROD_LOG_LOCATION_PREFIX + ".1");
        }
        if (HashSeekConstants.TEST.equals(domain)) {
            return getValue(TEST_LOG_LOCATION_PREFIX + ".1");
        }
        throw new RuntimeException("Bad domain prefix: "+ domain + " ["+ HashSeekConstants.PREDPROD +","+HashSeekConstants.PROD +","+ HashSeekConstants.TEST +"]");
    }

    public static String getHashLocation(String domain) {
        if (HashSeekConstants.PREDPROD.equals(domain)) {
            return getValue(HASH_FILE_LOCATION) + "/" + HashSeekConstants.PREDPROD;
        }
        if (HashSeekConstants.PROD.equals(domain)) {
            return getValue(HASH_FILE_LOCATION) + "/" + HashSeekConstants.PROD;
        }
        if (HashSeekConstants.TEST.equals(domain)) {
            return getValue(HASH_FILE_LOCATION) + "/" + HashSeekConstants.TEST;
        }
        throw new RuntimeException("Bad domain prefix: "+ domain + " ["+ HashSeekConstants.PREDPROD +","+HashSeekConstants.PROD +","+ HashSeekConstants.TEST +"]");
    }

    synchronized static void loadProperties(){
        File propsFile=new File(HASH_SEEK_PROPERTIES);
        try {
            properties = new Properties();
            properties.load(new FileInputStream(propsFile));

            //vyplnuju mapu locationKeyMap
            Enumeration e = properties.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (key.contains(LOG_LOCATION)) {
                    locationKeyMap.put(properties.getProperty(key), key);
                }
            }

            lastTimeofLoadingProperties=System.currentTimeMillis();
        } catch (Throwable t) {
            throw new RuntimeException("Nelze nahrat properties :"+propsFile.getAbsolutePath(),t);
        } finally {
        }
    }

    public static File getBgzDir(File originalFile) throws IOException{
        return getFileLocation(originalFile,BGZ_LOCATION);
    }

    public static File getHashDir(File originalFile) throws IOException{
        return getFileLocation(originalFile,HASH_LOCATION);
    }

    private static File getFileLocation(File originalFile,String typeOfLocation) throws IOException {
        String parentDir=originalFile.getAbsolutePath().replaceAll("\\\\","/");
        String key=locationKeyMap.get(parentDir);
        if(key!=null){
            key=key.replace(LOG_LOCATION,typeOfLocation);
            String path=getValue(key);
            if(path!=null){
                return new File(path);
            }else {
              throw new IOException("Nenalezena konfigurace pro: "+key);
            }

        }else{
            throw new IOException("Nenalezena konfigurace obsahujici adresar: "+parentDir);
        }
    }

    private static Enumeration getPropertyNames() {
        if(properties==null ||lastTimeofLoadingProperties<System.currentTimeMillis()-REFRESH_TIMEOF_PROPERTIES){
            loadProperties();
        }
        return properties.propertyNames();
    }

    public static String getValue(String key) {
        if(properties==null ||lastTimeofLoadingProperties<System.currentTimeMillis()-REFRESH_TIMEOF_PROPERTIES){
            loadProperties();
        }
        return properties.getProperty(key);
    }

    public static List<File> filesToHash() {
        List<File> files = new ArrayList<File>();
        for (String logLocation : filteredProperties(FILES_TO_HASH_LOCATION_PREFIX)) {
            File logDir = new File(logLocation);
            if (logDir.isDirectory()) {
                File[] logs = logDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return canBeHashed(dir ,name);
                    }
                });
                for (File log : logs) {
                    files.add(log);
                }
            } else {
                HashSeekConstants.outPrintLine(String.format("'%s' is NOT directory. Please check in '%s'", logDir, HASH_SEEK_PROPERTIES));
            }
        }
        return prioritize(files);
    }

    public static boolean isProdEligible(String name) {
        return filteredProperties(PROD_ELIGIBLE_PREFIX).contains(name);
    }

    public static boolean isSeekLimitEligible(String name) {
        return filteredProperties(LIMIT_ELIGIBLE_PREFIX).contains(name);
    }

    public static boolean isNoeSeekEligible(String name) {
        return filteredProperties(NOE_ELIGIBLE_PREFIX).contains(name);
    }

    public static boolean isSeekShowSessionEligible(String name) {
        return filteredProperties(SESSION_ELIGIBLE_PREFIX).contains(name);
    }

    private static List<File> prioritize(List<File> files) {
        //pak nekdy implementovat co ma jit v indexovani prvni, zatim tak jak je
        return files;
    }

    public static  boolean canBeHashed(File dir, String name) {
        File file = new File(dir, name);
        Pattern auditPattern;
        Pattern timePattern;
        Pattern bpmPattern;
        Pattern b2bPattern;
        Pattern noePattern;
        if (dir.getAbsolutePath().contains("_predpr_") || dir.getAbsolutePath().contains("test")) {
            auditPattern = Pattern.compile("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?$");
            timePattern = Pattern.compile("^(other|jms)_s._alsb_aspect.time\\.(\\d{8}|part)$");
            bpmPattern = Pattern.compile("^cip_bpm_s.\\.log(\\.)?(\\d{8})(\\.\\d{2})?$");
            b2bPattern = Pattern.compile("^b2b_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?$");
            noePattern = Pattern.compile("^noe_s.\\.log(\\.)?(\\d{8})(\\.\\d{2})?$");
        } else { //prod
            auditPattern = Pattern.compile("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?$");
            //auditPattern = Pattern.compile("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})$");
            timePattern = Pattern.compile("^(other|jms)_s._alsb_aspect.time\\.(\\d{8}|part)$");
            //timePattern = Pattern.compile("^(other|jms)_s._alsb_aspect.time\\.(\\d{8})$");
            bpmPattern = Pattern.compile("^cip_bpm_s.\\.log(\\.)?(\\d{8}|part)(\\.\\d{2})?$");
            noePattern = Pattern.compile("^noe_s.\\.log(\\.)?(\\d{8}|part)(\\.\\d{2})?$");
            //bpmPattern = Pattern.compile("^cip_bpm_s.\\.log(\\.)?(\\d{8})?$");
            b2bPattern = Pattern.compile("^b2b_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?$");
        }
        Matcher auditMatcher = auditPattern.matcher(name);
        Matcher b2bMatcher = b2bPattern.matcher(name);
        Matcher timeMatcher = timePattern.matcher(name);
        Matcher bpmMatcher = bpmPattern.matcher(name);
        Matcher noeMatcher = noePattern.matcher(name);
        if (auditMatcher.matches() || timeMatcher.matches() || bpmMatcher.matches() || b2bMatcher.matches() || noeMatcher.matches()) {
            File hashFile = new File(String.format("%s/%s/%s%s", getHashLocation(), HashSeekConstants.getHashFileEnv(dir.getPath()), name, HashSeekConstants.HASH_FILE_SUFFIX));
            File hashFileBGZ = new File(String.format("%s/%s/%s%s%s", getHashLocation(), HashSeekConstants.getHashFileEnv(dir.getPath()), name, HashSeekConstants.BGZ_FILE_SUFFIX, HashSeekConstants.HASH_FILE_SUFFIX));
            File hashFileGZ = new File(String.format("%s/%s/%s%s%s", getHashLocation(), HashSeekConstants.getHashFileEnv(dir.getPath()), name, HashSeekConstants.HASH_FILE_SUFFIX, HashSeekConstants.GZ_FILE_SUFFIX));
            if ((hashFile.exists() && !canBeReplaced(hashFile, file,".part.hash")) || hashFileGZ.exists() ||(hashFileBGZ.exists()&& !canBeReplaced(hashFileBGZ, file,".part.bgz.hash"))|| tooOld(name)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean canBeReplaced(File hashFile, File file,String suffix) {
        if (hashFile.getName().endsWith(suffix)) { //napr. .part timelogy
            if (hashFile.lastModified() < file.lastModified() ) { //soubor je novejsi nez hash
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private static boolean tooOld(String name) {
        Pattern pattern = Pattern.compile("\\d{8}");
        Matcher matcher = pattern.matcher(name);
        long current = System.currentTimeMillis();
        fileDate.setTimeInMillis(current);
        if (matcher.find()) {
            String date = matcher.group();
            fileDate.set(Calendar.DAY_OF_MONTH, Integer.valueOf(date.substring(6,8)));
            fileDate.set(Calendar.MONTH, Integer.valueOf(date.substring(4,6))-1);
            fileDate.set(Calendar.YEAR, Integer.valueOf(date.substring(0,4)));
            if (MAX_TIME_TO_INDEX_BACKWARDS > current - fileDate.getTimeInMillis()) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public static Set<String> filteredProperties (String prefixFilter) {
        Enumeration e = getPropertyNames();
        Set<String> filteredProperties = new HashSet<String>();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(prefixFilter)) {
                filteredProperties.add(getValue(key));
            }
        }
        return filteredProperties;
    }



    public static String getIndexProcesssXms() {
        return getValue(INDEX_PROCESSS_XMS);
    }

    public static String getIndexProcesssXmx() {
        return getValue(INDEX_PROCESSS_XMX);
    }

    public Integer getEsbLogsRemotePort(int index) {
        String property = getIndexedProperty(ESB_REMOTE_PORT_PREFIX, index);
        if (property != null) {
            return Integer.parseInt(property);
        } else {
            return null;
        }
    }

    public String getEsbLogsRemoteHost(int index) {
        return getIndexedProperty(ESB_REMOTE_HOST_PREFIX, index);
    }

    public static String getEsbServerNumber(int index) {
        return getIndexedProperty(ESB_SERVER_NUMBER_PREFIX, index);
    }

    public String getLogDomain(int index) {
        return getIndexedProperty(LOG_DOMAIN_PREFIX, index);
    }

    public static  String getLogLocation(int index) {
        return getIndexedProperty(LOG_LOCATION_PREFIX, index);
    }

    public String getLogServerNumber(int index) {
        return getIndexedProperty(LOG_SERVER_NUMBER_PREFIX, index);
    }

    public static String getIndexedProperty(String prefix, int index) {
        return getValue(String.format("%s.%s", prefix, index));
    }

    public static int getRemotePort() {
        return Integer.parseInt(getValue(SERVER_REMOTE_PORT));
    }

    public static void main(String[] args) throws IOException {
//        AppArguments appArguments = new AppArguments();
//        appArguments.setDayFrom("11");
//        appArguments.setDateFrom("20130320");
//        appArguments.setDaysToSeek("3");
        boolean bbb=AppProperties.canBeHashed(new File ("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test"),"other_s2_alsb_aspect.time.part");
//        HashSeekConstants.outPrintLine(appProperties.filesToSeek(appArguments).toString());
        HashSeekConstants.outPrintLine(String.format("%s",AppProperties.tooOld("jms_s1_alsb_aspect.audit.20130422")));
        HashSeekConstants.outPrintLine(String.format("%s",AppProperties.tooOld("jms_s1_alsb_aspect.audit.20130412")));
    }

    public static String getPredprodMaxIndexDepth() {
        return getValue(PREDPROD_MAX_INDEX_DEPTH);
    }

    public static String getDefaultMaxIndexDepth() {
        return getValue(DEFAULT_MAX_INDEX_DEPTH);
    }

    public static String getHashLocation() {
        return getValue(HASH_FILE_LOCATION);
    }

    public static String getIndexerType() {
        return getValue(INDEXER_TYPE_KEY);
    }

    public static int getTransformerNioBufferSize() {
        String value=getValue(TRANSFORMER_NIO_BUFFER_SIZE_KEY);
        int result=1024000;
        try {
            result = Integer.parseInt(value);
        } catch (Throwable t) {
            logger.error("Could not retrieve "+TRANSFORMER_NIO_BUFFER_SIZE_KEY+" from properties.",t);
        }
        return result;
    }

    public static int getTransformerMaxTmpFiles() {
        String value=getValue(TRANSFORMER_MAX_TMP_FILES_KEY);
        int result=10;
        try {
            result = Integer.parseInt(value);
        } catch (Throwable t) {
            logger.error("Could not retrieve "+TRANSFORMER_MAX_TMP_FILES_KEY+" from properties.",t);
        }
        return result;
    }

    public static int getBgzBlockSize() {
        String value=getValue(BGZ_BLOCK_SIZE);
        int result=200000;
        try {
            result = Integer.parseInt(value);
        } catch (Throwable t) {
            logger.error("Could not retrieve "+BGZ_BLOCK_SIZE+" from properties.",t);
        }
        return result;
    }

    public static int getBgzIndexBufferSize() {
        String value=getValue(BGZ_INDEX_BUFFER_SIZE);
        int result=300;
        try {
            result = Integer.parseInt(value);
        } catch (Throwable t) {
            logger.error("Could not retrieve "+BGZ_INDEX_BUFFER_SIZE+" from properties.",t);
        }
        return result;
    }
    public static Set<File> hashFilesToSeek(AppArguments appArguments) {
        Set<File> filesToScan = new HashSet<File>();
        if (appArguments.isSeekPredprod()) {
            File hashDir = new File(String.format("%s/%s", HashSeekConstants.HASH_FILE_LOCATION, HashSeekConstants.PREDPROD));
            addHashToSeek(appArguments, filesToScan, hashDir);
        }
        if (appArguments.isSeekProd()) {
            File hashDir = new File(String.format("%s/%s", HashSeekConstants.HASH_FILE_LOCATION, HashSeekConstants.PROD));
            addHashToSeek(appArguments, filesToScan, hashDir);
        }
        if (appArguments.isSeekTest()) {
            File hashDir = new File(String.format("%s/%s", HashSeekConstants.HASH_FILE_LOCATION, HashSeekConstants.TEST));
            addHashToSeek(appArguments, filesToScan, hashDir);
        }
        return filesToScan;
    }

    private static void addHashToSeek(final AppArguments appArguments, Set<File> filesToScan, File hashDir) {
        if (hashDir.isDirectory()) {
            File[] hashes = hashDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return hashToProcess(name, appArguments);
                }
            });
            for (File hash : hashes) {
                filesToScan.add(hash);
            }
        } else {
            HashSeekConstants.outPrintLine(String.format("'%s' is NOT directory. Please check in '%s'", hashDir, HASH_SEEK_PROPERTIES));
        }
    }

    private static boolean hashToProcess(String name, AppArguments appArguments) {
        Pattern pattern = Pattern.compile(String.format("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?%s$", HashSeekConstants.HASH_FILE_SUFFIX));
        Matcher matcher = pattern.matcher(name);
        if (appArguments.getDatesToScan().isEmpty()) {
            return false;
        } else if (matcher.matches()) {
            return (appArguments.getDatesToScan().contains(matcher.group(2)));
        } else {
            return false;
        }
    }

}
