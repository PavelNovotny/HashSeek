package com.o2.cz.cip.hashseek.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Calendar;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashSeekConstants {
    static final Logger LOGGER= LoggerFactory.getLogger(HashSeekConstants.class);

    public static final int MIN_WORD_SIZE = 5;

    private static Calendar current = Calendar.getInstance();


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

    public static String formatedDateTime(long time) {
        current.setTimeInMillis(time);
        return String.format("%1$tY.%1$tm.%1$td %1$tH:%1$tM:%1$tS.%1$tL", current);
    }

}
