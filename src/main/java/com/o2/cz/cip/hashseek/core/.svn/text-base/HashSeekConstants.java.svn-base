package com.o2.cz.cip.hashseek.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashSeekConstants {
    static final Logger LOGGER= LoggerFactory.getLogger(HashSeekConstants.class);

	public static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    public static final int HASH_TABLE_SIZE = 50000000; //ok pro predprod i prod
    public static final int MIN_WORD_SIZE = 5;
    public static int MAX_WORD_OCCURENCES_TO_PROCESS = 50; //10 je minimum, 50 asi optimalni.
    public static final int BUFFER_COUNT = 1000;
    public static final int MAX_COLLISIONS = 10;
    public static final int BUFFER_SIZE = HASH_TABLE_SIZE / BUFFER_COUNT;
    public static final String HASH_FILE_SUFFIX = ".hash";
    public static final String HASH_TMP_FILE = "./tmp/hash.tmp";
    public static String HASH_FILE_LOCATION = "./hash";
    public static String BGZ_INDEX_FILE_LOCATION = "./bgz";
    public static final String BUFFER_LOCATION = "./tmp/";
    public static final String GZ_FILE_SUFFIX = ".gz";
    public static final String BLOCKS_FILE_SUFFIX = ".blocks";
    public static final String BGZ_FILE_SUFFIX = ".bgz";
    public static final int MAX_REMOTE_COUNT = 4; //s1, s2, s3....
    public static final int MAX_LOG_LOCATION_COUNT = 2; //other a jms

    private static Calendar current = Calendar.getInstance();
    public static final String PREDPROD = "predprod";
    public static final String PROD = "prod";
    public static final String TEST = "test";
    public static final String OTHER = "other";

    public static final String OLD_INDEXER_TYPE = "old";
    public static final String BLOCK_INDEXER_TYPE = "block";

    public static final char END_LINE_CHARACTER = '\n';

    public static int javaHashCodeTableIndex(String hashed) {
        return (hashed.hashCode() & 0x7fffffff) % HASH_TABLE_SIZE;
    }

    public static void outPrintLine(String line) {
        outPrintLine(System.out,line);
    }

    public static void outPrintLine(PrintStream output, String line) {
        if(System.out==output){
            LOGGER.info(String.format("%s   %s", formatedDateTime(System.currentTimeMillis()), line));
        }
        output.println(String.format("%s   %s", formatedDateTime(System.currentTimeMillis()), line));
        output.flush();
    }

    public static void outPrintLineSimple(PrintStream output, String line) {
        LOGGER.info(line);
        if(System.out==output){
            LOGGER.info(line);
        }else{
            output.println(line);
            output.flush();
        }
    }

    public static String formatedDateTime(long time) {
        current.setTimeInMillis(time);
        return String.format("%1$tY.%1$tm.%1$td %1$tH:%1$tM:%1$tS.%1$tL", current);
    }

    public static String formatedTime(long timeInMillis) {
        long time = timeInMillis/1000;
        return String.format("%5d:%02d:%02d", time /60 /60, (time / 60) % 60, time % 60);
    }

    public static String formatedTimeMillis(long timeInMillis) {
        long time = timeInMillis/1000;
        return String.format("%5d:%02d:%02d.%03d", time /60 /60, (time / 60) % 60, time % 60, timeInMillis % 1000);
    }


    public static String getFileName(String originalFilePath) {
        String[] split = originalFilePath.split("[\\\\/]");
        return split[split.length-1];
    }

    public static String getHashFileEnv(String originalFilePath) {
        if (originalFilePath.contains("_predpr_")) {
            return PREDPROD;
        } else if (originalFilePath.contains("esb_logs")) {
            return PROD;
        } else if (originalFilePath.contains("test")) {
            return TEST;
        } else {
           return OTHER;
        }
    }

    public static String dateString(Calendar cal) {
        return String.format("%s%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
    }
}
