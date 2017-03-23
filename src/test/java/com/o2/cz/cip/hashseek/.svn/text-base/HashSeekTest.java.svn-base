package com.o2.cz.cip.hashseek;

import com.o2.cz.cip.hashseek.blockseek.AbstractSeekResults;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditSeekResults;
import com.o2.cz.cip.hashseek.blockseek.blockbpmlog.BpmBlockLogRecord;
import com.o2.cz.cip.hashseek.blockseek.blocktimelog.TimeBlockLogRecord;
import com.o2.cz.cip.hashseek.blockseek.blocktimelog.TimeSeekResults;
import com.o2.cz.cip.hashseek.core.BlockHashFileCreator;
import com.o2.cz.cip.hashseek.core.BlockSeek;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.io.BlockHashReader;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.io.ReadOnlyFileChannel;
import com.o2.cz.cip.hashseek.util.BlockSeekUtil;
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
    private static Logger LOGGER = Logger.getLogger(HashSeekTest.class);

    @Test
    public void testNioBlockHashFileCreator() throws Exception {
        //BlockHashReader bhr = new BlockHashReader(new File("/Users/pavelnovotny/temp/logData/jms_s3_alsb_aspect.audit.20140120.09"), 100, null);
        ReadOnlyFileChannel bhr = new ReadOnlyFileChannel(new File("/Users/pavelnovotny/temp/logData/jms_s3_alsb_aspect.audit.20140120.09"));
        int end, i=0;
        while ((end = bhr.readWords()) > 0 && i < 100) {
            i++;
            switch (end) {
                case BlockHashReader.END_ALL:
                    LOGGER.debug(String.format("%d %d", bhr.javaHashBig, bhr.javaHashSmall));
                    if (bhr.smallWordLength != bhr.bigWordLength) {
                        LOGGER.debug(String.format("%d %d", bhr.javaHashBig, bhr.javaHashSmall));
                    }
                    bhr.javaHashBig=0;
                    bhr.bigWordLength=0;
                    break;
                case BlockHashReader.END_SMALL:
                    LOGGER.debug(String.format("%d %d", bhr.javaHashBig, bhr.javaHashSmall));
                    break;
                default:
                    break;
            }
        }
        bhr.close();
    }

    @Test
    public void testLength() throws Exception {
        Pattern pattern = Pattern.compile("2013");
        byte[] bytesToRead = "př2013fffřřř".getBytes("UTF-8");
        int middle =1;
        byte[] inTheMiddle = new byte[bytesToRead.length-middle];
        int j=0;
        for (int i = middle; i< bytesToRead.length; i++) {
            String s1 = String.format("Original:%8s", Integer.toBinaryString(bytesToRead[i] & 0xFF)).replace(' ', '0');
            LOGGER.debug(s1);
            inTheMiddle[j++] = bytesToRead[i];
            s1 = String.format("Changed:%8s", Integer.toBinaryString(inTheMiddle[j-1] & 0xFF)).replace(' ', '0');
            LOGGER.debug(s1);
        }

        String s = String.format("IntheMiddle:%8s", Integer.toBinaryString((inTheMiddle[1] >>6 &0x02)& 0xFF)).replace(' ', '0');
        LOGGER.debug(s);

        byte UTF8_NEXT_BYTES=(byte)128;
        //toto je zde kvuli tomu, kdyz prvni z 3 bytu je cast UTF-8 znaku viz. http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/sun/nio/cs/UTF_8.java
        for (int i = 0; i < 4; i++) {
            //if ((inTheMiddle[0]&UTF8_NEXT_BYTES)==UTF8_NEXT_BYTES){
                if ((inTheMiddle[0] & 0xC0)==0x80){
                //nahrazuju ho A
                String s1 = String.format("Milos:%8s", Integer.toBinaryString(inTheMiddle[i] & 0xFF)).replace(' ', '0');
                LOGGER.debug(s1);
                inTheMiddle[0]=(byte)65;
                s1 = String.format("Milos:%8s", Integer.toBinaryString(inTheMiddle[i] & 0xFF)).replace(' ', '0');
                LOGGER.debug(s1);
                s1 = String.format("Milos:%8s", Integer.toBinaryString(bytesToRead[i] & 0xFF)).replace(' ', '0');
                LOGGER.debug(s1);
            } else{
                break;
            }
        }


        String line = (new String(inTheMiddle,"UTF-8"));
        Matcher matcher = pattern.matcher(line);
        int offset = -1;
        while (matcher.find()) {
            String dateTime = matcher.group();
            offset = matcher.start();
            LOGGER.debug(String.format("Offset %s", offset));
            offset=line.substring(0,offset).getBytes("UTF-8").length;
            LOGGER.debug(String.format("Offset %s", offset));
        }
    }



    @Test
    public void testHashes() throws Exception {
        RandomAccessFile raf = new RandomAccessFile(new File("/Users/pavelnovotny/soapui-settings.xml"), "r");
        StringBuilder sb = new StringBuilder("12345FGvrttyy4444");
//        LOGGER.info(String.format("'%s', '%s'", raf.javaHash(sb), sb.toString().hashCode()));
//        sb = new StringBuilder("1sdfh asf asfoweee44444řčřščřč=éščřččerdkkkk2345FGvrttyy4444");
//        LOGGER.info(String.format("'%s', '%s'", raf.javaHash(sb), sb.toString().hashCode()));
//        LOGGER.info(String.format("'%s', '%s', '%s'", sb.toString(), raf.javaHash(sb), raf.djb2Hash(sb)));
    }

    @Test
    public void testWords() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/logData/jms_s3_alsb_aspect.audit.20140120.09");
        LOGGER.info(String.format("started testCountWords in '%s'", file.getPath()));
        File reportFile = new File("/Users/pavelnovotny/temp/logData/words.txt");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream(reportFile));
        BlockHashReader raf = new BlockHashReader(file, 0, null);
        String word;
        int count = 0;
        while ((raf.readWords()) != 0) {
            report.write(String.format("'%s'",raf.javaHashSmall).getBytes());
            report.write(String.format("'%s'",raf.javaHashSmall).getBytes());
            count++;
        }
        LOGGER.info(String.format("Pocet slov '%s'.", count));
        raf.close();
        report.close();
        LOGGER.info("ended testCountWords");
    }



    @Test
    public void testDictionary() throws Exception {
//        File file = new File("/Users/pavelnovotny/temp/logData/jms_s3_alsb_aspect.audit.20140120.09");
//        File file = new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20140123");
//        File file = new File("/Users/pavelnovotny/temp/logData/other_s4_alsb_aspect.audit.20131001");
//        File file = new File("/Users/pavelnovotny/temp/logData/b2b_s1_alsb_aspect.audit.20140120.08");
        File file = new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20130401");
        File reportFile = new File("/Users/pavelnovotny/temp/logData/dictionary.txt");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream(reportFile));
        LOGGER.info(String.format("started testDictionary in '%s' to '%s'", file.getPath(), reportFile.getPath()));
        RandomAccessFile raf = new RandomAccessFile(file,"r");
        long count = 0;
        Map<String, Integer> words = new HashMap<String, Integer>(HashSeekConstants.HASH_TABLE_SIZE);
        Map<String, Integer> priorityWords = new HashMap<String, Integer>(HashSeekConstants.HASH_TABLE_SIZE);
        String end;
        while ((end = raf.readWord()) != null) {
            if (end.length() < HashSeekConstants.MIN_WORD_SIZE) {
                continue;
            }
            Integer wordCountInteger = words.get(end);
            int wordCount = wordCountInteger==null?0:wordCountInteger.intValue();
            Integer priorityCountInteger = priorityWords.get(end);
            int priorityCount = priorityCountInteger==null?0:priorityCountInteger.intValue();
//            if (raf.isPriorityWord()) {
//                priorityWords.put(end, priorityCount + 1);
//            } else {
//                words.put(end, wordCount + 1);
//            }
            words.put(end, wordCount + 1);
            count++;
            String[] subWords = end.split("[-@_.]");
            for (String subWord : subWords) {
                if (subWord.length() < HashSeekConstants.MIN_WORD_SIZE) {
                    continue;
                }
                wordCountInteger = words.get(subWord);
                wordCount = wordCountInteger==null?0:wordCountInteger.intValue();
                priorityCountInteger = priorityWords.get(end);
                priorityCount = priorityCountInteger==null?0:priorityCountInteger.intValue();
//                if (raf.isPriorityWord()) {
//                    priorityWords.put(end, priorityCount + 1);
//                } else {
//                    words.put(end, wordCount + 1);
//                }
                words.put(end, wordCount + 1);
                count++;
            }
        }
        ValueMapComparator bvc =  new ValueMapComparator(words);
        TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(bvc);
        sortedMap.putAll(words);
        for (String readWord : sortedMap.keySet()) {
            report.write(String.format("% 9d %s\n", words.get(readWord), readWord).getBytes());
        }
        report.write("-------------------- priority words ---------------------\n".getBytes());
        bvc =  new ValueMapComparator(priorityWords);
        sortedMap = new TreeMap<String,Integer>(bvc);
        sortedMap.putAll(priorityWords);
        for (String readWord : sortedMap.keySet()) {
            report.write(String.format("% 9d %s\n", priorityWords.get(readWord), readWord).getBytes());
        }
        LOGGER.info(String.format("Pocet slov '%s'.", count));
        report.close();
        raf.close();
        LOGGER.info("ended testDictionary");
    }

    @Test
    public void testIsSorted() throws Exception {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("/Users/pavelnovotny/temp/hash/hashRaw.hash.sorted")));
        int oldHashValue = Integer.MIN_VALUE;
        int newHashValue = Integer.MIN_VALUE;
        int oldPointerValue = Integer.MIN_VALUE;
        int newPointerValue = Integer.MIN_VALUE;
        long longValue = Long.MIN_VALUE;
        int count = 0;
        try {
            while (true) {
                count++;
                longValue = in.readLong();
                newHashValue = (int) (longValue >>32);
                newPointerValue = (int) (longValue);
                //if (!(newHashValue >= oldHashValue)) {
                if (!((newHashValue > oldHashValue) || (newHashValue == oldHashValue && newPointerValue >= oldPointerValue))) {
                    LOGGER.info(String.format("not sorted %010d %010d %d %d ---- %d %d", count, count*Long.SIZE/Byte.SIZE, newHashValue, newPointerValue, oldHashValue, oldPointerValue));
                }
                oldHashValue = newHashValue;
                oldPointerValue = newPointerValue;
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }


    }

    @Test
    public void testIndexSearch() throws Exception {
        int maxIndexsize = 50000000; //50 mil pro binarky
        int avgIndexsize = 5000000; //pokud pouzijeme 5mil, pro 50 mil by melo byt max. 10 zaznamu, ale vetsinou nemame 50 mil unikatnich klicu, takze to bude typicky mene. Budeme skoro cilit na zaznam.
        //velikost tohoto indegu bude 5 mil * 8 (long) = cca 40MG coz bude std. overhead na indexu.
        int factor = normalizeHashToMaxValue(Integer.MAX_VALUE, maxIndexsize) / avgIndexsize +1;
        LOGGER.info(String.format("computed index max: %d", normalizeHashToMaxValue(Integer.MAX_VALUE, maxIndexsize)/factor));
        LOGGER.info(String.format("computed index min: %d", normalizeHashToMaxValue(0, maxIndexsize)/factor));
        for (int i=0; i< 1000; i++) {
            LOGGER.info(String.format("computed index for  % 15d: % 15d", normalizeHashToMaxValue(i, maxIndexsize), normalizeHashToMaxValue(i, maxIndexsize)/factor));
        }
        for (int i=Integer.MAX_VALUE; i> Integer.MAX_VALUE - 1000; i--) {
            LOGGER.info(String.format("computed index for  % 15d: % 15d", normalizeHashToMaxValue(i, maxIndexsize), normalizeHashToMaxValue(i, maxIndexsize)/factor));
        }
    }

    private int normalizeHashToMaxValue(int hash, int maxHashValue) {
        return hash % maxHashValue;
    }

    @Test
    public void testList() throws Exception {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("/Users/pavelnovotny/temp/hash/hashRaw.hash.sorted")));
        int hashValue = Integer.MIN_VALUE;
        int pointerValue = Integer.MIN_VALUE;
        long longValue = Long.MIN_VALUE;
        int count = 0;
        try {
            while (true) {
                count++;
                longValue = in.readLong();
                hashValue = (int) (longValue >>32);
                pointerValue = (int) (longValue);
                LOGGER.info(String.format("hash, pointer: % 15d % 15d", hashValue, pointerValue));
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }
    }

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
    public void testBlockRecordContainer1() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/logData/_predpr_/other_s1_alsb_aspect.audit.20140314.12.bgz");
        List<File> filesToSeek = new LinkedList<File>();
        filesToSeek.add(file);
        AbstractSeekResults auditSeekResults = new AuditSeekResults(filesToSeek);
        List<List<String>> stringsToSeek = new LinkedList<List<String>>();
        List<String> strings = new LinkedList<String>();
        stringsToSeek.add(strings);
        strings.add("VOLAM");
        List<File> filesToSeekTime = new LinkedList<File>();
        file = new File("/Users/pavelnovotny/temp/logData/_predpr_/other_s1_alsb_aspect.time.20140314.bgz");
        filesToSeekTime.add(file);
        strings.add("2083480415628488911--59e05c04.144bc7915db.-1b");
        AbstractSeekResults timeSeekResults = new TimeSeekResults(filesToSeekTime);
        auditSeekResults.addDependentSeekResults(timeSeekResults);
        //tohle má vyhodit výjimku, že lze volat jenom z parenta - test výjimky
        //timeSeekResults.runSeek(stringsToSeek);
        //auditSeekResults.runSeek(stringsToSeek, System.out);
        auditSeekResults.reportSortedResults(new File("/Users/pavelnovotny/temp/vysledek.txt"), System.out,null,null, false);
    }

    @Test
    public void testBlockRecordContainer() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/logData/_predpr_/other_s1_alsb_aspect.audit.20140314.12.bgz");
        List<File> filesToSeek = new LinkedList<File>();
        filesToSeek.add(file);
        AbstractSeekResults auditSeekResults = new AuditSeekResults(filesToSeek);
        List<List<String>> stringsToSeek = new LinkedList<List<String>>();
        List<String> strings = new LinkedList<String>();
        stringsToSeek.add(strings);
        strings.add("VOvvvLAM");
        List<File> filesToSeekTime = new LinkedList<File>();
        file = new File("/Users/pavelnovotny/temp/logData/_predpr_/other_s1_alsb_aspect.time.20140314.bgz");
        filesToSeekTime.add(file);
        strings.add("2083480415628488911--59e05c04.144bc7915db.-1b");
        AbstractSeekResults timeSeekResults = new TimeSeekResults(filesToSeekTime);
        auditSeekResults.addDependentSeekResults(timeSeekResults);
        //tohle má vyhodit výjimku, že lze volat jenom z parenta - test výjimky
        //timeSeekResults.runSeek(stringsToSeek);
        //auditSeekResults.runSeek(stringsToSeek, System.out);
        auditSeekResults.reportData();
        auditSeekResults.reportTimeStamps();
        auditSeekResults.sort();
        auditSeekResults.reportHeaderData();
        auditSeekResults.reportFormattedData();
    }

    @Test
    public void testBpm() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/billingadapter_s2.log.part");
        String content = new Scanner(file).useDelimiter("\\Z").next();
        BpmBlockLogRecord bpmBlockLogRecord = new BpmBlockLogRecord(content, file, 111111);
        System.out.println(bpmBlockLogRecord.getHeaderData());
        System.out.println(bpmBlockLogRecord.getFormattedData());
    }

    @Test
    public void testTime() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/jms_s2_alsb_aspect.time.part");
        String content = new Scanner(file).useDelimiter("\\Z").next();
        TimeBlockLogRecord timeBlockLogRecord = new TimeBlockLogRecord(content, file, 111111);
        System.out.println(timeBlockLogRecord.getHeaderData());
        System.out.println(timeBlockLogRecord.getFormattedData());
    }


    @Test
    public void testCreateHash() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20140304.14");
        //vytvoření hashe
        BlockHashFileCreator hashFileCreator = new BlockHashFileCreator();
        hashFileCreator.createHashFile(file);
    }

    @Test
    public void testHashCorrectness() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20140304.14");
        //vytvoření hashe
        BlockHashFileCreator hashFileCreator = new BlockHashFileCreator();
        hashFileCreator.createHashFile(file);
        //načtení slov a jejich pozic pro pozdější kontrolu
        Map<String, Long> wordPositions = new HashMap<String, Long>();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        String word;
        while ((word = raf.readWord()) != null) {
            if (word.length() > HashSeekConstants.MIN_WORD_SIZE && word.length() < BlockSeekUtil.MAX_WORD_SIZE)
            wordPositions.put(word, raf.getFilePointer());
        }
        raf.close();
        BlockSeek blockSeek = new BlockSeek();
        List<List<String>> strings = new LinkedList<List<String>>();
        List<String> toSeek = new LinkedList<String>();
        strings.add(toSeek);
        for (String checkedWord : wordPositions.keySet()) {
            toSeek.clear();
            toSeek.add(checkedWord);
            Map<Long, Integer> positions = blockSeek.seekForPositions(strings, file, 100, System.out); //pozice a délka
            byte[] seekedBytes = checkedWord.getBytes();
            raf = new RandomAccessFile(file, "r");
            boolean found = false;
            for (Long position : positions.keySet()) { //některé jsou falešné, ale alespoň jedna tam musí být.
                raf.seek(position);
                byte[] rawBytes = raf.readRawBytes(positions.get(position));
                int index = BlockSeekUtil.indexOf(rawBytes, seekedBytes);
                if (index > -1) {
                    found = true;
                }
                //if ((position+index) == wordPositions.get(checkedWord)) {
                //    found = true;
                //}
            }
            if (!found) {
                System.out.println(String.format("oops.. '%s'", checkedWord));
            } else {
                //System.out.println(String.format("OK     '%s'", checkedWord));
            }
            raf.close();
        }

        //hashFileCreator.seek("Přidat", file);
        //hashFileCreator.seek("transactionType", file);
        //hashFileCreator.seek("9407022119");
        //hashFileCreator.seek("Změna", file);
        //System.out.println(hashFileCreator.normalizeToHashSpace(hashFileCreator.javaHash("Přidat")));
    }

    @Test
    public void testBlockHashFileCreator() throws IOException {
        //hashFileCreator.createHashFile(new File(args[0]));
        //hashFileCreator.createHashFile(new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20130401"));
        //hashFileCreator.writeFinalHashFile(null);
        //hashFileCreator.seek("MSP001874360");

        //hashFileCreator.createHashFile(new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20130401"));
        //hashFileCreator.createHashFile(new File("/Users/pavelnovotny/temp/logData/other_s3_alsb_aspect.audit.20130730.sorted"));
        //hashFileCreator.writeFinalHashFile(null);
        //hashFileCreator.seek1("MSP001874360");
        //hashFileCreator.seek1("xmlns");
        //hashFileCreator.seek1("SR0004");
        //hashFileCreator.seek("141913517");

        //hashFileCreator.createHashFile(new File("/Users/pavelnovotny/temp/logData/other_s3_alsb_aspect.audit.20140302.sorted"));
        //hashFileCreator.writeFinalHashFile(null);
        //hashFileCreator.seek("Přidat");
        //hashFileCreator.seek("Změna

        File file = new File("/Users/pavelnovotny/temp/logData/other_s1_alsb_aspect.audit.20140304.14"); //sorted file se bude jmenovat jako originální soubor
        BlockHashFileCreator hashFileCreator = new BlockHashFileCreator();
        BlockSeek blockSeek = new BlockSeek();
        //hashFileCreator.createHashFile(file);
        //hashFileCreator.writeFinalHashFile(file);
        testSeek("Přidat", file);
        //hashFileCreator.seek("transactionType", file);
        //hashFileCreator.seek("9407022119");
        //hashFileCreator.seek("Změna", file);
        //hashFileCreator.checkSeek("32.747", file);
        //System.out.println(hashFileCreator.normalizeToHashSpace(hashFileCreator.javaHash("Přidat")));

        //File file = new File("/Users/pavelnovotny/temp/logData/other_s3_alsb_aspect.audit.20140302"); //sorted file se bude jmenovat jako originální soubor
        //hashFileCreator.createHashFile(file);
        //hashFileCreator.writeFinalHashFile(file);
        //hashFileCreator.seek("transactionType", file);

        //File file = new File("/Users/pavelnovotny/temp/logData/pokus"); //sorted file se bude jmenovat jako originální soubor
        //hashFileCreator.createHashFile(file);
        //hashFileCreator.checkSeek("32.747", file);
        //hashFileCreator.checkSeek("timestamp", file);
        //System.out.println(hashFileCreator.maskSign(hashFileCreator.javaHash("32.747"))); // plusové číslo
        //int intOne = hashFileCreator.maskSign(hashFileCreator.javaHash("32.747"));
        //int intTwo = 0;
        //long longValue = hashFileCreator.makeLongFromTwoInts(intOne, intTwo);
        //System.out.println(longValue);
        //int hash = (int) (longValue >>32);
        //System.out.println(hash);

        //hashFileCreator.createHumanReadableFile();
        //hashFileCreator.makeLongFromTwoInts(1, 2);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(1, 2), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(-345, 99999), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(Integer.MAX_VALUE, Integer.MIN_VALUE), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(Integer.MIN_VALUE, Integer.MAX_VALUE), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(Integer.MIN_VALUE, Integer.MIN_VALUE), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(Integer.MAX_VALUE, Integer.MAX_VALUE), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(0, Integer.MAX_VALUE), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(Integer.MAX_VALUE, 0), 0, 0);
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(Integer.MIN_VALUE, 0), 0, 0);
        //int i1 =0, i2 =0;
        //hashFileCreator.makeTwoIntsFromLong(hashFileCreator.makeLongFromTwoInts(0, Integer.MIN_VALUE), i1, i2);
        //logger.debug(String.format("int one '%d' two '%d'", i1, i2));
    }



    @Test
    public void testSize() throws Exception {
        File file = new File("/Users/pavelnovotny/temp/logData/count.txt");
        RandomAccessFile raf = new RandomAccessFile(file,"r");
        String number;
        long bytes = 0;
        while ((number = raf.readWord()) != null) {
            if (number.length() ==0) {
               continue;
            }
            int num = Integer.parseInt(number);
            if (num > 10000) {
                num = 10000;
            }
            bytes += num*6;
        }
        LOGGER.info(String.format("Celkovy pocet bytu: '%s'.", bytes));
    }


    class ValueMapComparator implements Comparator<String> {

        Map<String, Integer> base;
        public ValueMapComparator(Map<String, Integer> base) {
            this.base = base;
        }

        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            }
        }

    }


}
