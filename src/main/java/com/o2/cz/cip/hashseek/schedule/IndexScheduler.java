package com.o2.cz.cip.hashseek.schedule;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.BlockHashFileCreator;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.core.TransformAndIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Pavel
 * Date: 2.4.13 14:10
 */
public class IndexScheduler implements Runnable {
    static final Logger LOGGER= LoggerFactory.getLogger(IndexScheduler.class);

    private static final int SLEEP_TIME = 90000;
    private static final int FILE_NEW = 0;
    private static final int FILE_CHANGED = 1;
    private static final int FILE_PASSED_CHECK = 2;
    Map<File, Long> fileChangeTimes = new HashMap<File, Long>();

    public IndexScheduler(String[] args) {
    }

    public static void main (String[] args) {
        IndexScheduler indexScheduler = new IndexScheduler(args);
        (new Thread(indexScheduler)).start();
    }

    public void run() {
        while(true) {
            HashSeekConstants.outPrintLine("looked for files to index");
            indexFiles(TransformAndIndex.class.getName());
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                LOGGER.error("IndexScheduler-run",e);
            }
        }
    }


    private void indexFiles(String indexClass) {
        try {
            List<File> filesToHash=AppProperties.filesToHash();
            int counter=filesToHash.size();
            for (File fileToHash :filesToHash ) {
                HashSeekConstants.outPrintLine(String.format("Files waiting for indexing: '%s'", counter--));
                int fileTimeCheck = fileTimeCheck(fileToHash);
                if (fileTimeCheck == FILE_NEW) {
                    HashSeekConstants.outPrintLine(String.format("File '%s' just arrived. I will try index next time. File info: size '%s', modified:'%s'", fileToHash.getAbsolutePath(), fileToHash.length(), HashSeekConstants.formatedDateTime(fileToHash.lastModified())));
                    continue;
                } else if (fileTimeCheck == FILE_CHANGED) {
                    HashSeekConstants.outPrintLine(String.format("File '%s' continues copying. I will try index next time.. File info: size '%s', modified:'%s'", fileToHash.getAbsolutePath(), fileToHash.length(), HashSeekConstants.formatedDateTime(fileToHash.lastModified())));
                    continue;
                }
                fileChangeTimes.remove(fileToHash); //bude se indexovat, proto ho dál nepotřebujeme, udržujeme mapu aktuální.
                HashSeekConstants.outPrintLine(String.format("Started separate process for indexing '%s' File info: size '%s', modified:'%s'", fileToHash.getAbsolutePath(), fileToHash.length(), HashSeekConstants.formatedDateTime(fileToHash.lastModified())));
                ProcessBuilder pb = new ProcessBuilder("nice","-20","java", AppProperties.getIndexProcesssXmx(), AppProperties.getIndexProcesssXms(), "-cp", "./HashSeek.jar:./log4j-1.2.17.jar:./slf4j-api-1.7.5.jar:./slf4j-log4j12-1.7.5.jar", indexClass, fileToHash.getPath());
                pb.directory(new File("./"));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                InputStream inputStream = p.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String processOutput;
                while ((processOutput = bufferedReader.readLine()) != null) {
                    HashSeekConstants.outPrintLine(processOutput);
                }
                bufferedReader.close();
                int exitStatus = p.waitFor();
                HashSeekConstants.outPrintLine(String.format("ended indexing process with exit value '%s'", exitStatus));
            }
        } catch (IOException e) {
            LOGGER.error("indexFiles",e);
        } catch (InterruptedException e) {
            LOGGER.error("indexFiles", e);
        }
    }

    private int fileTimeCheck(File fileToHash) {
        if (!fileChangeTimes.containsKey(fileToHash)) {
            fileChangeTimes.put(fileToHash, fileToHash.lastModified());
            return FILE_NEW;
        } else if (fileChangeTimes.get(fileToHash) < fileToHash.lastModified()) {
            fileChangeTimes.put(fileToHash, fileToHash.lastModified());
            return FILE_CHANGED;
        }
        return FILE_PASSED_CHECK;
    }

}
