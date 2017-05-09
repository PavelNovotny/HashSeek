package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.core.BlockSeek;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.BlockSeekUtil;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.*;


/**
 * Created by pavelnovotny on 14.01.14.
 */
public class HashSeekTest {
    private static Logger LOGGER = Logger.getLogger(HashSeekTest.class);


    public void testSeek(String seekedString, File seekedFile) throws IOException {
        BlockSeek blockSeek = new BlockSeek();
        List<List<String>> toSeek = new LinkedList<List<String>>();
        List<String> strings = new LinkedList<String>();
        toSeek.add(strings);
        strings.add(seekedString);
        Map<Long, Integer> positions = blockSeek.seekForPositions(toSeek, seekedFile, 100, System.out); //pozice a délka
        RandomAccessFile raf = new RandomAccessFile(seekedFile, "r");
        byte[] seekedBytes = seekedString.getBytes();
        for (Long position : positions.keySet()) { //některé jsou falešné (kolizní), ale alespoň jedna tam musí být.
            raf.seek(position);
            byte[] rawBytes = raf.readRawBytes(positions.get(position));
            int index = BlockSeekUtil.indexOf(rawBytes, seekedBytes);
            if (index > -1) {
                System.out.println(new String(rawBytes, "UTF-8"));
            }
        }
    }


    @Test
    public void testNewSeek() throws IOException {
        File file = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        testSeek("00000000000000000120700486", file);
    }



}
