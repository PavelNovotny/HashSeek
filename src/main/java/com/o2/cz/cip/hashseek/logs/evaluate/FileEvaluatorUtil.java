package com.o2.cz.cip.hashseek.logs.evaluate;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mfrydl on 27.2.14.
 */
public class FileEvaluatorUtil {

    public static Set<File> filesToSeek(final AppArguments appArguments,FileEvaluator fileEvaluator) throws IOException {
        Set<File> filesToScan = new HashSet<File>();
        if (appArguments.isSeekPredprod()) {
            addFilesToSeek(appArguments, filesToScan, AppProperties.PREDPROD_LOG_LOCATION_PREFIX,fileEvaluator);
        }
        if (appArguments.isSeekProd()) {
            addFilesToSeek(appArguments, filesToScan, AppProperties.PROD_LOG_LOCATION_PREFIX,fileEvaluator);
        }
        if (appArguments.isSeekTest()) {
            addFilesToSeek(appArguments, filesToScan, AppProperties.TEST_LOG_LOCATION_PREFIX,fileEvaluator);
        }
        return filesToScan;
    }


    private static Set<File> addFilesToSeek(final AppArguments appArguments, Set<File> filesToScan, String logLocationPrefix,final FileEvaluator fileEvaluator) throws IOException{
        for (String logLocation : AppProperties.filteredProperties(logLocationPrefix)) {
            File logDir = new File(logLocation);
            if(fileEvaluator instanceof HashVersionEvaluator){
              logDir=AppProperties.getHashDir(logDir);
            }
            if (logDir.isDirectory()) {
                File[] logs = logDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return fileEvaluator.canBeProcessed(name, appArguments);
                    }
                });
                for (File log : logs) {
                    filesToScan.add(log);
                }
            } else {
                HashSeekConstants.outPrintLine(String.format("'%s' is NOT directory. Please check in '%s'", logDir, AppProperties.HASH_SEEK_PROPERTIES));
            }
        }
        return filesToScan;
    }


    public static Set<String> missedFiles(AppArguments appArguments, Set<File> existingFiles, String fileSuffix, String kindOfLog,FileEvaluator fileEvaluator) {
        Set<String> missedFiles = new HashSet<String>();
        String[] servers=fileEvaluator.getServers(appArguments);
        String[] domains=fileEvaluator.getDomains(appArguments);
        for (String date : appArguments.getDatesToScan()) {
            for (String server : servers) {
                for (String domain : domains) {
                    String fileName = String.format("%s_%s_alsb_aspect.%s.%s%s", domain, server, kindOfLog, date, fileSuffix);
                    boolean found = false;
                    for (File existingFile : existingFiles) {
                        if (existingFile.getName().equals(fileName)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        missedFiles.add(fileName);
                    }
                }
            }
        }
        return  missedFiles;
    }

    private static void filterNonIndexed(String env, int size, Set<File> allNonIndexedFiles, Set<File> nonIndexedFiles) {
        Calendar current = Calendar.getInstance();
        final String today = HashSeekConstants.dateString(current);
        for (int i = 1;i <= size; i++) {
            String bgzFolder = AppProperties.getValue(env + ".logLocation."+i);
            String folder = AppProperties.getValue(env + ".transferLocation."+i);
            if (folder == null) {
                HashSeekConstants.outPrintLine(String.format("folder '%s'. Please check in '%s'", folder, AppProperties.HASH_SEEK_PROPERTIES));
                continue;
            }
            if (bgzFolder == null) {
                HashSeekConstants.outPrintLine(String.format("bgzFolder '%s'. Please check in '%s'", bgzFolder, AppProperties.HASH_SEEK_PROPERTIES));
                continue;
            }
            File dir = new File(folder);
            if (dir.isDirectory()) {
                File[] files = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File file, String name) {
                        if (name.matches("^(other|jms).*"+today+"\\.\\d\\d$") || name.endsWith(".audit")) {
                            return true;
                        }
                        return false;
                    }
                });
                allNonIndexedFiles.addAll(Arrays.asList(files));
                nonIndexedFiles.addAll(Arrays.asList(files));
            } else {
                HashSeekConstants.outPrintLine(String.format("'%s' is NOT directory. Please check in '%s'", dir, AppProperties.HASH_SEEK_PROPERTIES));
                continue;
            }
            File bgzDir = new File(bgzFolder);
            if (bgzDir.isDirectory()) {
                File[] files = bgzDir.listFiles(new FilenameFilter() {
                    public boolean accept(File file, String name) {
                        if (name.matches(".*.\\d\\d\\.bgz$")) {
                            return true;
                        }
                        return false;
                    }
                });
                for (File bgzFile : files) {
                    for (File file : allNonIndexedFiles) {
                        if (bgzFile.getPath().contains(file.getName())) {
                            nonIndexedFiles.remove(file);
                        }
                    }
                }
            } else {
                HashSeekConstants.outPrintLine(String.format("'%s' is NOT directory. Please check in '%s'", dir, AppProperties.HASH_SEEK_PROPERTIES));
                continue;
            }
        }
    }

    public static Set<File> nonIndexedFiles(AppArguments appArguments) {
        final Set<File> nonIndexedFiles = new HashSet<File>();
        final Set<File> allIndexedFiles = new HashSet<File>();
        if (appArguments.isSeekProd()) {
            int size = AppProperties.filteredProperties("prod.logLocation").size();
            filterNonIndexed("prod", size, allIndexedFiles, nonIndexedFiles);
        } else if (appArguments.isSeekPredprod()) {
            int size = AppProperties.filteredProperties("predprod.logLocation").size();
            filterNonIndexed("predprod", size, allIndexedFiles, nonIndexedFiles);
        } else if (appArguments.isSeekTest()) {
            int size = AppProperties.filteredProperties("test.logLocation").size();
            filterNonIndexed("test", size, allIndexedFiles, nonIndexedFiles);
        }
        return nonIndexedFiles;
    }

    public static Set<String> missedLastFiles(AppArguments appArguments, Set<File> existingFiles, String fileSuffix,FileEvaluator fileEvaluator) {
        Set<String> missedFiles = new HashSet<String>();
        Calendar current = Calendar.getInstance();
        long currentTime = System.currentTimeMillis();
        current.setTimeInMillis(currentTime);
        current.set(Calendar.HOUR_OF_DAY,0);
        String today = HashSeekConstants.dateString(current);
        while (current.getTimeInMillis() < currentTime) {
            String dateHour = String.format("%s.%02d.bgz", today, current.get(Calendar.HOUR_OF_DAY));
            fileEvaluator.addToMissedFiles(missedFiles, appArguments, dateHour);
            current.add(Calendar.HOUR_OF_DAY,1);
        }
        if (appArguments.getDatesToScan().contains(today)) { // remote seek jenom pro dnesni soubory
            for (File existingFile : existingFiles) {
                Pattern pattern = Pattern.compile(String.format("\\d{8}\\.\\d{2}.bgz%s$", fileSuffix));
                Matcher matcher = pattern.matcher(existingFile.getName());
                if (matcher.find()) { //hodinovy log
                    String dateHour = matcher.group();
                    if (missedFiles.contains(existingFile.getName())) {
                        missedFiles.remove(existingFile.getName());
                    }
                }
            }
            for (String missedFile : missedFiles) {
                missedFile.replaceFirst(".bgz","");
            }
            fileEvaluator.addToMissedFiles(missedFiles, appArguments, ""); // posledni logy
        } else {
            missedFiles.removeAll(missedFiles);
        }
        return  missedFiles;
    }


/*    private static boolean canBeProcessed(String name, AppArguments appArguments) {
        Pattern pattern = Pattern.compile("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?(.bgz)?$");
        Matcher matcher = pattern.matcher(name);
        if (appArguments.getDatesToScan().isEmpty()) {
            return false;
        } else if (matcher.matches()) {
            return (appArguments.getDatesToScan().contains(matcher.group(2)));
        } else {
            return false;
        }
    }*/

}
