package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.core.BlockHashFileCreator;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by pavelnovotny on 14.01.14.
 */
public class HashSeekTest {

    @Test
    public void testCreateIndex() throws Exception {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File hashFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        HashSeekConstants.outPrintLine("started testCreateIndex");
        BlockHashFileCreator hashCreator = new BlockHashFileCreator();
        hashCreator.createHashFile(file, hashFile, null);
        HashSeekConstants.outPrintLine("ended testCreateIndex");
    }

}
