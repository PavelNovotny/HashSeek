package com.o2.cz.cip.hashseek.logs.auditlog;



import com.o2.cz.cip.hashseek.sort.ExternalSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by mfrydl on 18.2.14.
 */
public class SortAuditLogUtil {
    public static final char END_LINE_CHARACTER = '\n';
    static final Logger LOGGER= LoggerFactory.getLogger(SortAuditLogUtil.class);

    public static void findBeaIdsPosition(String fileName,int nioBufferSize, int maxTmpFiles) throws Exception{
        File file = new File(fileName);
        int countOfDigits=countOfSigninNumber(file.length());
        //soubor kam ukladam beaId a pozice
        File fileBeaId=new File(file.getParentFile(),file.getName()+".beaid");

        //soubor kam ukladam beaId a pozice
        File fileBeaIdSorted=new File(file.getParentFile(),file.getName()+".beaid.sorted");

        FileWriter fileWriter=new FileWriter(fileBeaId);
        RandomAccessFile aFile = new RandomAccessFile(file, "r");
        FileChannel inChannel = aFile.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(nioBufferSize);

        StringBuilder lastLine = new StringBuilder();
        //sem si ukladam datum
        StringBuilder lineDate = new StringBuilder();
        //sem ukladam cas
        StringBuilder lineTime = new StringBuilder();
        //sem ukladam thread
        StringBuilder lineThread = new StringBuilder();
        //sem ukladam beaId
        StringBuilder lineBeaId = new StringBuilder();

        //pozice kde zacina posledni radek
        long beaIdStart=0;
        //counter pozice v souboru,
        long position=-1;
        // pozice posledniho konce radku
        long lastEndLine=-1;
        //true - nejedna se o radek s beaId, takze nemusim pak zbytek cist a muzu preskocit az na konec radku
        boolean waitForNextLine = false;

        //uz jsem identifikoval beaId
        boolean readBeaId = false;

        while (inChannel.read(byteBuffer) > 0) {
            byteBuffer.flip();

            for (int i = 0; i < byteBuffer.limit(); i++) {
                position++;
                char readedChar = (char) byteBuffer.get();
                if (readedChar == END_LINE_CHARACTER) {
                    lastEndLine=position;
                    beaIdStart=position+1;
                    //nuluju vsechny countery
                    lastLine.setLength(0);
                    lineDate.setLength(0);
                    lineTime.setLength(0);
                    lineThread.setLength(0);
                    lineBeaId.setLength(0);
                    waitForNextLine = false;
                    readBeaId = false;
                } else {
                    if (!waitForNextLine) {
                        if (readBeaId) {
                            if (lineBeaId.length() > 0 && lineBeaId.charAt(lineBeaId.length() - 1) == ';') {
                                //LOGGER.info(lineDate.toString() + lineTime.toString() + lineThread.toString() + lineBeaId.toString());
                                waitForNextLine = true;
                                if(lastEndLine>0){
                                    fileWriter.write(";"+convertLongToStringWithFixLenght(lastEndLine,countOfDigits)+"\n");
                                }
                                fileWriter.write(lineBeaId.toString());
                                fileWriter.write(convertLongToStringWithFixLenght(beaIdStart,countOfDigits));

                            } else {
                                lineBeaId.append(readedChar);
                            }
                        } else {
                            if (lineDate.length() < 9) {
                                lineDate.append(readedChar);
                            } else {

                                //kontroluju jestli na zacatku radky je datum
                                for (int j = 0; j < 8; j++) {
                                    if('0'>lineDate.charAt(j) || '9'<lineDate.charAt(j)){
                                        waitForNextLine = true;
                                        break;
                                    }
                                }
                                if(waitForNextLine){
                                    continue;
                                }
                                //kotroluju jestli datum konci strednikem
                                if(';'!=lineDate.charAt(8)){
                                    waitForNextLine = true;
                                    continue;
                                }
                                if (lineTime.length() < 9) {
                                    lineTime.append(readedChar);
                                } else {
                                    if (lineThread.length() > 100) {
                                        waitForNextLine = true;
                                        LOGGER.info("\t\t"+lineDate.toString() + lineTime.toString() + lineThread.toString() + lineBeaId.toString());
                                    } else {
                                        if (lineThread.length() > 0 && lineThread.charAt(lineThread.length() - 1) == ';') {
                                            readBeaId = true;
                                        } else {
                                            lineThread.append(readedChar);
                                        }

                                    }
                                }
                            }
                        }
                    }
                }

            }
            byteBuffer.clear(); // do something with the data and clear/compact it.
        }
        fileWriter.write(";"+position+"\n");
        fileWriter.close();
        inChannel.close();
        aFile.close();

        //tridim beaId
        boolean distinct=false;
        int headersize=0;

        Charset charset=Charset.defaultCharset();
        performExtraSort(fileBeaId, fileBeaIdSorted,maxTmpFiles,  distinct, headersize, charset);
        addPositionBeforeBeaId(fileBeaIdSorted,fileBeaId);
        performExtraSort(fileBeaId, fileBeaIdSorted,maxTmpFiles,  distinct, headersize, charset);
        constructSortedFile(file,fileBeaIdSorted);
        fileBeaId.delete();
        fileBeaIdSorted.delete();
        LOGGER.info("Setridene beaId:"+fileBeaIdSorted.getAbsolutePath());
    }

    /**
     *
     * @param logFile -zdrojovy soubor
     * @param logBeaIdFile - soubor se sesortovanyma pozicema pro beaid
     * @throws Exception
     */
    private static void constructSortedFile(File logFile,File logBeaIdFile) throws Exception{
        //
        FileReader beaSortedFileReader=new FileReader(logBeaIdFile);
        BufferedReader beaSortedBufferedReader=new BufferedReader(beaSortedFileReader);
        FileOutputStream sortedOriginalLogFile=new FileOutputStream(new File(logFile.getParentFile(),logFile.getName()+".sorted"));
        FileOutputStream blockFileOutputStream=new FileOutputStream(new File(logFile.getParentFile(),logFile.getName()+".blocks"));
        DataOutputStream blockOutputStream=new DataOutputStream(blockFileOutputStream);
        java.io.RandomAccessFile originalRandomAccessFile=new java.io.RandomAccessFile(logFile,"r");

        byte data[]=new byte[1024];
        long position=0;
        String lastBeaid="";
        String line=beaSortedBufferedReader.readLine();
        while(line!=null){
            //zjistuju pozici pro dalsi fvolani prislusejici beaid
            StringTokenizer stringTokenizer=new StringTokenizer(line,";");
            stringTokenizer.nextToken();
            String beaId=stringTokenizer.nextToken();
            long fromIndex=Long.parseLong(stringTokenizer.nextToken());
            long toIndex=Long.parseLong(stringTokenizer.nextToken());
            int missingBytes=(int)(toIndex-fromIndex);

            //pokud zacina dalsi beaid, tak skoncilo flow a ukladam informaci o pozici flow
            if(!lastBeaid.equals(beaId)){
                blockOutputStream.writeLong(position);
                //LOGGER.info(position);
                lastBeaid=beaId;
            }
            position+=missingBytes;

            //hledam zacatek volani beaid
            originalRandomAccessFile.seek(fromIndex);

            //kopiruju obsah volani ze zdrojoveho souboru do setrideneho
            int size=0;
            while(missingBytes>0 && size>=0){
                size=originalRandomAccessFile.read(data);
                //String part=new String(data,0,missingBytes<size?missingBytes:size);

                try {
                    sortedOriginalLogFile.write(data, 0, missingBytes < size ? missingBytes : size);
                } catch (Throwable t) {
                    LOGGER.error("sortFile",t);
                }
                missingBytes=missingBytes-size;
            }
            //zapisuju enter, zvysuju counter pozice
            position++;
            sortedOriginalLogFile.write(10);
            line=beaSortedBufferedReader.readLine();
        }
        //zapisuju konec souboru
        blockOutputStream.writeLong(position);
        LOGGER.info("spocitany konec souboru:"+position);
        LOGGER.info("skutecny konec souboru:"+new File(logFile.getParentFile(),logFile.getName()+".sorted").length());
        blockOutputStream.close();
        sortedOriginalLogFile.close();
        beaSortedBufferedReader.close();
        File fileToRename = new File(logFile.getAbsolutePath()+".sorted");
        fileToRename.renameTo(logFile);
    }

    private static void performExtraSort(File fromFile, File toFile,int maxTmpFiles,  boolean distinct, int headersize, Charset charset) throws IOException {
        List<File> l = ExternalSort.sortInBatch(fromFile, ExternalSort.defaultcomparator, maxTmpFiles, charset, fromFile.getParentFile(), distinct, headersize, false);
        ExternalSort.mergeSortedFiles(l, toFile, ExternalSort.defaultcomparator, charset, distinct, false, false);
    }

    /**
     * tato metoda pridava pred beaid pozici, aby az to nasledne srovnam, tak aby byli jednotliva flow za sebou cca tak jak prisli requestove zpravy
     * @param srcFile
     * @param destFile
     * @throws IOException
     */
    private static void addPositionBeforeBeaId(File srcFile,File destFile) throws IOException {
        FileReader fileReader=new FileReader(srcFile);
        BufferedReader bufferedReader=new BufferedReader(fileReader);
        FileWriter fileWriter1=new FileWriter(destFile);

        String lastBeaid="";
        String lastPosition="";
        String line=bufferedReader.readLine();
        while(line!=null){
            StringTokenizer stringTokenizer=new StringTokenizer(line,";");
            String newBeaId=stringTokenizer.nextToken();
            if(!lastBeaid.equals(newBeaId)){
                lastBeaid=newBeaId;
                lastPosition =stringTokenizer.nextToken();
            }
            fileWriter1.write(lastPosition);
            fileWriter1.write(";");
            fileWriter1.write(line);
            fileWriter1.write("\n");
            line=bufferedReader.readLine();
        }
        fileWriter1.close();
        bufferedReader.close();
    }

    public static String convertLongToStringWithFixLenght(long p_number,int countOfDigits){
        String s_number=String.valueOf(p_number);
        StringBuffer buf = new StringBuffer();
        int count = countOfDigits - s_number.length();
        for (int i = 0; i < count; i++) {
            buf.append('0');
        }
        buf.append(s_number);
        return buf.toString();
    }

    public static int countOfSigninNumber(long cislo)
    {
        int cifry = 0;
        //Dělí číslo 10 dokud není 0 a přičítá počet cifer za každé dělení
        while (cislo > 0)
        {
            cislo /= 10;
            cifry++;
        }
        return cifry;
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=3){
            LOGGER.info("Ocekavany 3 srgumenty: String fileName,int nioBufferSize, int maxTmpFiles  | priklad 10240000 10");
            return;
        }
        long startTime=System.currentTimeMillis();
        int nioBufferSize=Integer.parseInt(args[1]);
        int maxTmpFiles=Integer.parseInt(args[2]);
        findBeaIdsPosition(args[0],nioBufferSize,maxTmpFiles);
        long runTime=(System.currentTimeMillis()-startTime)/1000;
        LOGGER.info("Doba behu:"+runTime);
    }
}
