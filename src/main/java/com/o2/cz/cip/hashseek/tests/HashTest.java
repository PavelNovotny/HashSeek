package com.o2.cz.cip.hashseek.tests;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.logs.auditlog.HashSeekAuditLog;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.core.SingleFileHashSeek;
import com.o2.cz.cip.hashseek.io.*;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.logs.timelog.HashSeekTimeLog;
import net.sf.samtools.util.BlockCompressedInputStream;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashTest {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        long startTime=System.currentTimeMillis();
        HashTest hashTest = new HashTest();
//        hashTest.testHashCreate();
//        hashTest.testHashSeekManyFiles();
//        hashTest.testReport();
//        hashTest.testAccuracy();
//        hashTest.testReadWords();
//        hashTest.testCountWords();
//        hashTest.testCountWordsFileChannel1();
//        hashTest.testValues();
        hashTest.testBgz();
        //hashTest.testHashCreate();
        //hashTest.testSortedAudit();
        //hashTest.testCreateIndex();
//        hashTest.testHashSeek();
//        hashTest.testHashSeekTimeLog();
//        hashTest.readTest();
//          hashTest.testMod();
//        hashTest.testWriteData();
        long runTime=(System.currentTimeMillis()-startTime)/1000;
        System.out.println("runTime:"+runTime);

    }


    private void testCountWordsFileChannel1() throws IOException {
//                File file = new File("c:\\download\\analyzeLog\\logs\\foo.log");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s2_alsb_aspect.audit.20121106");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20120810");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20130320");
//        HashSeekConstants.outPrintLine(String.format("started testCountWords in '%s'", file.getPath()));
//        File file = new File("c:\\download\\analyzeLog\\logs\\jms_s1_alsb_aspect.audit.20120101");
        File file = new File("c:\\download\\analyzeLog\\logs\\prod\\other_s4_alsb_aspect.audit.20130409");
        File reportFile = new File("c:/download/analyzeLog/logs/TestHash.txt");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream(reportFile));
        HashSeekConstants.outPrintLine(String.format("started testCountWords in '%s' to '%s'", file.getPath(), reportFile.getPath()));
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        String word;
        long count = 0;
        Set<String> words = new HashSet<String>(HashSeekConstants.HASH_TABLE_SIZE);
        String end;
        while ((end = raf.readWord()) != null) {
            words.add(end);
            count++;
            String[] subWords = end.split("[-@_.]");
            for (String subWord : subWords) {
                words.add(subWord);
                count++;
            }
        }
        for (String readWord : words) {
            report.write(String.format("%s\n", readWord).getBytes());
        }
        HashSeekConstants.outPrintLine(String.format("Pocet slov '%s'.", count));
        report.close();
        raf.close();
        HashSeekConstants.outPrintLine("ended testCountWords");
    }

    private void testCountWords() throws IOException {
        File file = new File("c:\\download\\analyzeLog\\logs\\prod\\other_s4_alsb_aspect.audit.20130409");
        HashSeekConstants.outPrintLine(String.format("started testCountWords in '%s'", file.getPath()));
        File reportFile = new File("c:/download/analyzeLog/logs/words.txt");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream(reportFile));
        BlockHashReader raf = new BlockHashReader(file, 0, null);
        String word;
        int count = 0;
        while ((raf.readWords()) != 0) {
            report.write(String.format("'%s'", raf.javaHashSmall).getBytes());
            report.write(String.format("'%s'", raf.javaHashSmall).getBytes());
            count++;
        }
        HashSeekConstants.outPrintLine(String.format("Pocet slov '%s', pocet unikatnich slov '%s'.", count));
        raf.close();
        report.close();
        HashSeekConstants.outPrintLine("ended testCountWords");
    }

    private void testReadWords() throws IOException {
//                File file = new File("c:\\download\\analyzeLog\\logs\\foo.log");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s2_alsb_aspect.audit.20121106");
        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20120810");
        HashSeekConstants.outPrintLine(String.format("started testCountWords in '%s'", file.getPath()));
        File reportFile = new File("c:/download/analyzeLog/logs/TestHash.txt");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream(reportFile));
        HashSeekConstants.outPrintLine(String.format("started testCountWords in '%s' to '%s'", file.getPath(), reportFile.getPath()));
        RandomAccessFile raf = new RandomAccessFile(file, "r", 600);
        String word;
        int count = 0;
        Set<String> words = new HashSet<String>(600000);
        while ((word = raf.readWord()) != null) {
            words.add(word);
            count++;
            String[] subWords = word.split("[-:$@%^&*#~|+!?\\]\\[`]");
            for (String subWord : subWords) {
                words.add(subWord);
                count++;
                String[] subSubwords = subWord.split("[.]");
                for (String subSubWord : subSubwords) {
                    words.add(subSubWord);
                    count++;
                }
            }
        }
        for (String readWord : words) {
            report.write(String.format("%s\n", readWord).getBytes());
        }
        HashSeekConstants.outPrintLine(String.format("Pocet slov '%s'.", count));
        report.close();
        raf.close();
        HashSeekConstants.outPrintLine("ended testCountWords");
    }

    private void testHashSeek() throws IOException, ClassNotFoundException {
//        File file = new File("c:\\download\\analyzeLog\\logs\\foo.log"));
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s2_alsb_aspect.audit.20121106");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20120810");
//        File file1 = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20130320");
        File file = new File("c:\\download\\analyzeLog\\logs\\jms_s1_alsb_aspect.audit.20120101");
//        File file = new File("c:\\download\\analyzeLog\\logs\\pokus.txt");
//        File file = new File("c:\\download\\analyzeLog\\logs\\prod\\other_s4_alsb_aspect.audit.20130409");
        HashSeekAuditLog hashSeek = new HashSeekAuditLog();
//        hashSeek.seek(new String[]{"MSISDN","ahojahoj","caucau","nazdar","ffff.11111.rrrr","11111","22222.33333.22222","22222","33333"}, file);
//        hashSeek.seek(new String[]{"bd73b3210000013496281726ffff8115","20120101"}, file);
//        hashSeek.seek(new String[]{"13938113"}, file1);
       // hashSeek.reportSortedResults(new File("c:/download/analyzeLog/logs/result.txt"));
    }

    private void testHashSeekTimeLog() throws IOException, ClassNotFoundException {
        File file = new File("c:\\download\\analyzeLog\\hashSeek\\other_s1_alsb_aspect.time.20130908");
        HashSeekTimeLog hashSeek = new HashSeekTimeLog();
        hashSeek.seek(new String[]{"MobileMessagingM2MSK", "generated-r2-hlbdlu0k-6ka3;generated-r2-hlbdlu0k-6ka3"}, file, new HashSet<String>());
      //  hashSeek.reportSortedResults(new File("c:/download/analyzeLog/logs/result.txt"));
    }

    private void testHashSeekManyFiles() throws IOException, ClassNotFoundException {
        AppArguments appArguments = new AppArguments();
        appArguments.setDateFrom("20120101");
        appArguments.setDaysToSeek("500");
        HashSeekAuditLog hashSeek = new HashSeekAuditLog();
        hashSeek.seek(new String[]{"13938113", "13938113"}, appArguments);
   //     hashSeek.reportSortedResults(new File("c:/download/analyzeLog/logs/result.txt"));
    }

    private void testReport() throws IOException {
//        File file = new File("c:\\download\\analyzeLog\\logs\\foo.log"));
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s2_alsb_aspect.audit.20121106");
        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20120810");
        SingleFileHashSeek singleFileHashSeek = new SingleFileHashSeek(file);
        report(singleFileHashSeek);
    }

    private void testHashCreate() throws IOException {
        //test blockhashfilecreator
    }

    private void report(SingleFileHashSeek singleFileHashSeek) throws IOException {
        int maxWordSize = 600; //jenom pro velikost bufferu, kvuli rychlosti. Defaultni je prilis velky.
        long[][] hashTable = singleFileHashSeek.getHashTable();
        File file = singleFileHashSeek.getFile();
        RandomAccessFile raf = new RandomAccessFile(file, "r", maxWordSize);
        StringBuffer sb = new StringBuffer(maxWordSize);
        for (int hash = 0; hash < hashTable.length; hash++) {
            long[] pointers = hashTable[hash];
            if (pointers != null) {
//                System.out.println(String.format("%s , size %s", hash, pointers.size()));
//                if (hashTable[hash].size() > 50) {
//                    raf.seek(pointers.get(0));
//                    System.out.println(String.format("%s", raf.readWord()));
//                }
//                if (hashTable[hash].size() < 5 ) {
//                    for (long pointer : pointers) {
//                        raf.seek(pointer);
//                        System.out.println(String.format("%s %s", pointer, raf.readWord()));
//                    }
//                }
            }
        }
    }

    private void testAccuracy() throws IOException {
        testCreateIndex();
        testValues();
    }

    private void testCreateIndex() throws IOException {
//                File file = new File("c:\\download\\analyzeLog\\logs\\foo.log");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s2_alsb_aspect.audit.20121106");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20120810");

//        File file = new File("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test\\jms_s1_alsb_aspect.audit.20140115");
        //File file = new File("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test\\jms_s1_alsb_aspect.audit.20140115.gz");
        File file = new File("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test\\other_s2_alsb_aspect.audit.20140117");

//        File file = new File("c:\\download\\analyzeLog\\logs\\prod\\other_s4_alsb_aspect.audit.20130409");
//        File file = new File("c:\\download\\analyzeLog\\logs\\jms_s1_alsb_aspect.audit.20120101");
//        File file = new File("c:\\download\\analyzeLog\\logs\\pokus.txt");
//        File file = new File("c:\\download\\analyzeLog\\logs\\TestHash.txt");
        HashSeekConstants.outPrintLine("started testCreateIndex");
//        HashFileCreator hashCreator = new HashFileCreator();
//        hashCreator.createHashFileInner(file);
        HashSeekConstants.outPrintLine("ended testCreateIndex");
    }

    private void testSortedAudit() throws IOException {
        String beaStr = "1674051389536206822--60ce10db.143a12d1a60.3bdb";
        System.out.println("Length:" + beaStr.length() + "-" + beaStr.getBytes().length);
        File file = new File("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test\\other_s1_alsb_aspect.audit.20140118");
        FileReader fileReader=new FileReader(new File(file.getParentFile(),"beaid_sorted.txt"));
        BufferedReader bufferedReader=new BufferedReader(fileReader);
        java.io.RandomAccessFile randomAccessFile=new java.io.RandomAccessFile(file,"r");
        String line=bufferedReader.readLine();
        byte data[]=new byte[1024];
        while(line!=null){
            StringTokenizer stringTokenizer=new StringTokenizer(line,";");
            stringTokenizer.nextToken();
            long fromIndex=Long.parseLong(stringTokenizer.nextToken());
            long toIndex=Long.parseLong(stringTokenizer.nextToken())+1;
            int missingBytes=(int)(toIndex-fromIndex);
            randomAccessFile.seek(fromIndex);
            int size=0;
            while(missingBytes-size>0 && size>=0){
                size=randomAccessFile.read(data);
                String part=new String(data,0,missingBytes<size?missingBytes:size);
                missingBytes=missingBytes-size;
                System.out.println(part);
            }
            line=bufferedReader.readLine();
        }
        randomAccessFile.close();
        bufferedReader.close();

    }
    private void testBgz() throws IOException {
        File file = new File("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test\\other_s1_alsb_aspect.audit.20140118");
        File bgzfile = new File("d:\\_svnko\\cip_modules\\branches\\HashSeek27504\\logs\\test\\other_s1_alsb_aspect.audit.20140118.bgz");

        //File bgzfile = BgzUtil.createBlockCompressedGzFile(file, 2000000, 300);
        System.out.println(BgzUtil.fileLength(bgzfile));
        System.out.println(BgzUtil.fileLength(file));
        File fileOut = new File(file.getParentFile(), file.getName() + ".decomp");
        FileOutputStream fout = new FileOutputStream(fileOut);
        FileInputStream fileInputStream = new FileInputStream(bgzfile);
        BlockCompressedInputStream inputStream = new BlockCompressedInputStream(fileInputStream);


        byte[] buffer = new byte[100000];
        int len = 0;
        while ((len = inputStream.read(buffer)) > 0) {
            fout.write(buffer, 0, len);
        }
        inputStream.close();
        fout.close();

    }

    private void testValues() throws IOException {
        HashSeekConstants.outPrintLine("started seekValues test");
        //overeni zda vyhledane stringy sedi.
//        File file = new File("c:\\download\\analyzeLog\\logs\\foo.log");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s2_alsb_aspect.audit.20121106");
//        File file = new File("c:\\download\\analyzeLog\\logs\\other_s1_alsb_aspect.audit.20120810");
//        File file = new File("c:\\download\\analyzeLog\\logs\\prod\\other_s1_alsb_aspect.audit.20130320");
        File file = new File("c:\\download\\analyzeLog\\logs\\jms_s1_alsb_aspect.audit.20120101");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream("c:/download/analyzeLog/logs/TestHash_results.txt"));
        BufferedReader source = new BufferedReader(new FileReader(new File("c:/download/analyzeLog/logs/TestHash.txt")));
        String line;
        Set<String> words = new HashSet<String>(200000);
        while ((line = source.readLine()) != null) {
            words.add(line);
        }
        source.close();
        report.write(String.format("%s\n", "================== NOT OK ====================").getBytes());
        int maxWordSize = 600; //jenom pro velikost bufferu, kvuli rychlosti. Defaultni je prilis velky.
        SingleFileHashSeek singleFileHashSeek = new SingleFileHashSeek(file);
        long[][] hashTable = singleFileHashSeek.getHashTable();
        SeekableInputStream raf = new RandomSeekableInputStream(file, "r", maxWordSize);
        for (String word : words) {
            if (word.length() < HashSeekConstants.MIN_WORD_SIZE) {
                continue;
            }
            boolean ok = false;
            for (Long pointer : singleFileHashSeek.getNoCollisionPointers(word, raf)) {
                raf.seek(pointer);
                String fileWord = raf.readWord();
                if (fileWord.contains(word)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                report.write(String.format("%s\n", word).getBytes());
                report.flush();
            }
        }
        report.close();
        raf.close();
        HashSeekConstants.outPrintLine("ended seekValues test");
    }


    public void readTest() throws IOException {
        File file = new File("c:\\download\\analyzeLog\\logs\\foo.log");
        HashSeekConstants.outPrintLine(String.format("started testRead."));
        File reportFile = new File("c:/download/analyzeLog/logs/copy.txt");
        BufferedOutputStream report = new BufferedOutputStream(new FileOutputStream(reportFile));
        BlockHashReader raf = new BlockHashReader(file, 0, null);
        FileChannel fc = new FileInputStream(file).getChannel();
        int c = -1;
        long bufferPos = 0L;
        final long bufferSize = 1600;
        long newBufferPos = bufferPos + bufferSize;
        HashSeekConstants.outPrintLine(String.format("fc size: %s", fc.size()));
        MappedByteBuffer mb = fc.map(FileChannel.MapMode.READ_ONLY, bufferPos, newBufferPos > fc.size() ? fc.size() - bufferPos : bufferSize);
        bufferPos = newBufferPos;

        while (true) {
            try {
                c = mb.get();
            } catch (BufferUnderflowException e) {
                if (bufferPos > fc.size()) {
                    break;
                }
                newBufferPos = bufferPos + bufferSize;
                mb = fc.map(FileChannel.MapMode.READ_ONLY, bufferPos, newBufferPos > fc.size() ? fc.size() - bufferPos : bufferSize);
                bufferPos = newBufferPos;
                continue;
            }
            report.write(c);
        }
        fc.close();
        report.close();
        HashSeekConstants.outPrintLine(String.format("ended testRead."));
    }

    public void testMod() {
        int[][] hashes = new int[HashSeekConstants.BUFFER_COUNT][HashSeekConstants.BUFFER_SIZE];
        for (int hashIndex = 0; hashIndex < HashSeekConstants.HASH_TABLE_SIZE; hashIndex++) {
            int bufferHashIndex = hashIndex % HashSeekConstants.BUFFER_SIZE;
            int bufferNo = hashIndex / (HashSeekConstants.HASH_TABLE_SIZE / HashSeekConstants.BUFFER_COUNT);

            hashes[bufferNo][bufferHashIndex]++;
        }
        int count = 0, sum = 0;
        for (int i = 0; i < HashSeekConstants.BUFFER_COUNT; i++) {
            for (int j = 0; j < HashSeekConstants.BUFFER_SIZE; j++) {
                if (hashes[i][j] != 0) {
                    count++;
                    sum += hashes[i][j];
//                    HashSeekConstants.outPrintLine(String.format("%s %s %s", i, j, hashes[i][j]));
                }
            }
        }
        HashSeekConstants.outPrintLine(String.format("%s       %s %s ", count, sum, HashSeekConstants.BUFFER_SIZE * HashSeekConstants.BUFFER_COUNT));
    }

    public void testWriteData() throws IOException {
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("./test.bin")));
        long position = 3 * Integer.SIZE; //HASH_TABLE_SIZE, BUFFER_SIZE, BUFFER_COUNT
        position += HashSeekConstants.BUFFER_COUNT * (Integer.SIZE + Long.SIZE + Long.SIZE); //indexFrom, startPosition, endPosition
        position = position / Byte.SIZE; //to bytes;
        os.writeInt(HashSeekConstants.HASH_TABLE_SIZE);
        os.writeInt(HashSeekConstants.BUFFER_SIZE);
        os.writeInt(HashSeekConstants.BUFFER_COUNT);
        os.close();
//        RandomAccessFile raf = new RandomAccessFile(new File("./test.bin"), "r");
        RandomAccessFile raf = new RandomAccessFile(new File("./jms_s1_alsb_aspect.audit.20120101.hash"), "r");
        HashSeekConstants.outPrintLine(String.format("%s", raf.readInt()));
        HashSeekConstants.outPrintLine(String.format("%s", raf.readInt()));
        HashSeekConstants.outPrintLine(String.format("%s", raf.readInt()));
        raf.close();
    }

}
