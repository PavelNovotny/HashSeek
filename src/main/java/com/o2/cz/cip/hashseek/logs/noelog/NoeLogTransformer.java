package com.o2.cz.cip.hashseek.logs.noelog;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.logs.AbstractLogTransformer;
import com.o2.cz.cip.hashseek.util.CloseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by mfrydl on 11.3.14.
 */
public class NoeLogTransformer extends AbstractLogTransformer{
    static final Logger LOGGER= LoggerFactory.getLogger(NoeLogTransformer.class);

    @Override
    public boolean canBeProcessed(File file) {
        boolean result=file.getName().contains("noe");
        return result;
    }

    @Override
    public void processFileBeforeComputeHash(File file) throws Exception {
        if(HashSeekConstants.BLOCK_INDEXER_TYPE.equals(AppProperties.getIndexerType())){
            constructSortedBlockFile(file, AppProperties.getTransformerNioBufferSize(), AppProperties.getTransformerMaxTmpFiles());
        }
    }

    @Override
    public void processFileAfterComputeHash(File file) throws Exception  {

    }

    public void constructSortedBlockFile(File file,int nioBufferSize, int maxTmpFiles) throws Exception{
        //zjistuji jak je soubor velky, a z kolika cifer je velikost. abych vedel na kolik cifer mam zleva doplnovat pozici nulama, abych mohl jednoduse Stringove sortovat
        int countOfDigits=countOfSigninNumber(file.length());
        //soubor kam ukladam beaId a pozice
        File fileBeaId=new File(file.getParentFile(),file.getName()+".beaid");

        //soubor kam ukladam beaId a pozice
        File fileBeaIdSorted=new File(file.getParentFile(),file.getName()+".beaid.sorted");

        FileWriter fileWriter=null;
        RandomAccessFile aFile = null;
        FileChannel inChannel = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(nioBufferSize);

        try {
            fileWriter=new FileWriter(fileBeaId);
            aFile = new RandomAccessFile(file, "r");
            inChannel = aFile.getChannel();

            StringBuilder lastLine = new StringBuilder();
            StringBuilder line = new StringBuilder();
            Map<String, String> logThreads = new HashMap<String, String>();
            Long logThreadId = 0L;
            //sem si ukladam datum
            StringBuilder lineDate = new StringBuilder();
            //sem ukladam cas
            StringBuilder lineTime = new StringBuilder();

            //sem ukladam beaId
            StringBuilder lineBeaId = new StringBuilder();

            //pozice kde zacina posledni radek
            long beaIdStart = 0;
            //counter pozice v souboru,
            long position = -1;
            // pozice posledniho konce radku
            long lastEndLine = -1;
            //pocitam pocet stredniku za casem
            int semicolCount=0;

            //true - nejedna se o radek s beaId, takze nemusim pak zbytek cist a muzu preskocit az na konec radku
            boolean waitForNextLine = false;

            //uz jsem identifikoval beaId
            boolean readBeaId = false;

            boolean writedFirstBeaId=false;

            while (inChannel.read(byteBuffer) > 0) {
                byteBuffer.flip();

                for (int i = 0; i < byteBuffer.limit(); i++) {
                    position++;
                    char readedChar = (char) byteBuffer.get();
                    //System.out.print(readedChar);
                    if (readedChar == HashSeekConstants.END_LINE_CHARACTER) {
                        lastEndLine = position;
                        beaIdStart = position + 1;
                        //nuluju vsechny countery
                        lastLine.setLength(0);
                        lineDate.setLength(0);
                        lineTime.setLength(0);
                        semicolCount=0;
                        lineBeaId.setLength(0);
                        line.setLength(0);
                        waitForNextLine = false;
                        readBeaId = false;
                    } else {
                        if (!waitForNextLine) {
                            line.append(readedChar);
                            if (readBeaId) {
                                if (lineBeaId.length() > 0 && lineBeaId.charAt(lineBeaId.length() - 1) == ';') {
                                    //LOGGER.info(lineDate.toString() + lineTime.toString() + lineThread.toString() + lineBeaId.toString());
                                    waitForNextLine = true;
                                    if (lastEndLine > 0 && writedFirstBeaId) {
                                        fileWriter.write(";" + convertLongToStringWithFixLenght(lastEndLine, countOfDigits) + "\n");
                                    }
                                    fileWriter.write(lineBeaId.toString());
                                    logThreadId = writeThread(fileWriter, line, logThreads, logThreadId, lineBeaId.toString()); //může se inkrementovat
                                    fileWriter.write(convertLongToStringWithFixLenght(beaIdStart, countOfDigits));
                                    writedFirstBeaId=true;

                                } else {
                                    lineBeaId.append(readedChar);
                                }
                            } else {
                                if (lineDate.length() < 9) {
                                    lineDate.append(readedChar);
                                } else {

                                    //kontroluju jestli na zacatku radky je datum
                                    for (int j = 0; j < 8; j++) {
                                        if ('0' > lineDate.charAt(j) || '9' < lineDate.charAt(j)) {
                                            waitForNextLine = true;
                                            break;
                                        }
                                    }
                                    if (waitForNextLine) {
                                        continue;
                                    }
                                    //kotroluju jestli datum konci strednikem
                                    if (';' != lineDate.charAt(8)) {
                                        waitForNextLine = true;
                                        continue;
                                    }
                                    if (lineTime.length() < 9) {
                                        lineTime.append(readedChar);
                                    } else {
                                        if (semicolCount>=3){
                                            readBeaId = true;
                                            lineBeaId.append(readedChar);
                                        }else{
                                            if(readedChar==';'){
                                                semicolCount++;
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
            fileWriter.write(";" + position + "\n");
        }  finally {
            CloseUtil.close(fileWriter);
            CloseUtil.close(inChannel);
            CloseUtil.close(aFile);
        }



        //tridim beaId
        boolean distinct=false;
        int headersize=0;

        Charset charset=Charset.defaultCharset();
        //tridim soubor obsahujici beaId a jejich pozice
        performExtraSort(fileBeaId, fileBeaIdSorted,maxTmpFiles,  distinct, headersize, charset);
        //pridavam pred kazde beaId pozici
        addPositionBeforeBeaId(fileBeaIdSorted,fileBeaId);
        //tridim beaId tak aby byli za sebou pokud mozno tak jak prichazeli v case
        performExtraSort(fileBeaId, fileBeaIdSorted,maxTmpFiles,  distinct, headersize, charset);
        //sortuji puvodni audit.log soubor podle drive setridenych beaId
        constructSortedFile(file,fileBeaIdSorted);
        fileBeaId.delete();
        fileBeaIdSorted.delete();
        LOGGER.info("Setridene beaId:"+fileBeaIdSorted.getAbsolutePath());
    }

    private Long writeThread(FileWriter fileWriter, StringBuilder line, Map<String, String> logThreads, Long logThreadId, String lineBeaId) throws IOException {
        String logThread = line.toString().split(";")[4];
        String key = lineBeaId+logThread;
        if (!logThreads.containsKey(key)) {
            logThreads.put(key,String.format("%012d;",++logThreadId));
        }
        fileWriter.write(logThreads.get(key));
        return logThreadId;
    }

    //v abstraktní třídě se nám to liší (nepočítá se s doplňkovým sortováním v rámci beaId), proto dávám sem
    protected void constructSortedFile(File logFile,File logBeaIdFile) throws Exception{
        FileReader beaSortedFileReader=null;
        BufferedReader beaSortedBufferedReader=null;
        FileOutputStream sortedOriginalLogFile=null;
        FileOutputStream blockFileOutputStream=null;
        DataOutputStream blockOutputStream=null;
        RandomAccessFile originalRandomAccessFile=null;
        File sortedFile=new File(logFile.getParentFile(),logFile.getName()+".sorted");
        File dir = AppProperties.getBgzDir(logFile.getParentFile());
        try {
            beaSortedFileReader=new FileReader(logBeaIdFile);
            beaSortedBufferedReader=new BufferedReader(beaSortedFileReader);
            sortedOriginalLogFile=new FileOutputStream(sortedFile);
            blockFileOutputStream=new FileOutputStream(new File(dir,logFile.getName()+ HashSeekConstants.BLOCKS_FILE_SUFFIX));
            blockOutputStream=new DataOutputStream(blockFileOutputStream);
            originalRandomAccessFile=new RandomAccessFile(logFile,"r");

            byte data[] = new byte[1024];
            long position = 0;
            String lastBeaid = "";
            String line = beaSortedBufferedReader.readLine();
            while (line != null && line.length()>0) {
                //zjistuju pozici pro dalsi fvolani prislusejici beaid
                StringTokenizer stringTokenizer = new StringTokenizer(line, ";");
                stringTokenizer.nextToken();
                String beaId = stringTokenizer.nextToken();
                stringTokenizer.nextToken();
                long fromIndex = Long.parseLong(stringTokenizer.nextToken());
                long toIndex = Long.parseLong(stringTokenizer.nextToken());
                int missingBytes = (int) (toIndex - fromIndex);

                //pokud zacina dalsi beaid, tak skoncilo flow a ukladam informaci o pozici flow
                if (!lastBeaid.equals(beaId)) {
                    blockOutputStream.writeLong(position);
                    //LOGGER.info(""+position);
                    lastBeaid = beaId;
                }
                position += missingBytes;

                //hledam zacatek volani beaid
                originalRandomAccessFile.seek(fromIndex);

                //kopiruju obsah volani ze zdrojoveho souboru do setrideneho
                int size = 0;
                while (missingBytes > 0 && size >= 0) {
                    size = originalRandomAccessFile.read(data);
                    //String part=new String(data,0,missingBytes<size?missingBytes:size);

                    try {
                        sortedOriginalLogFile.write(data, 0, missingBytes < size ? missingBytes : size);
                    } catch (Throwable t) {
                        LOGGER.error("sortFile", t);
                    }
                    missingBytes = missingBytes - size;
                }
                //zapisuju enter, zvysuju counter pozice
                position++;
                sortedOriginalLogFile.write(10);
                line = beaSortedBufferedReader.readLine();
            }
            //zapisuju konec souboru
            blockOutputStream.writeLong(position);
            LOGGER.info("spocitany konec souboru:" + position);
            LOGGER.info("skutecny konec souboru:" + sortedFile.length());
        }  finally {
            CloseUtil.close(beaSortedBufferedReader);
            CloseUtil.close(sortedOriginalLogFile);
            CloseUtil.close(blockOutputStream);
            CloseUtil.close(originalRandomAccessFile);
        }
        logFile.delete();
        boolean isRenamed=sortedFile.renameTo(logFile);
        LOGGER.info(sortedFile.getAbsolutePath()+" is renamed to "+logFile.getAbsolutePath()+" with result:"+isRenamed);
        //sortedFile=new File(logFile.getParentFile(),logFile.getName()+".sorted");
        //sortedFile.delete();
    }

    protected void addPositionBeforeBeaId(File srcFile,File destFile) throws IOException {
        FileReader fileReader=null;
        BufferedReader bufferedReader=null;
        FileWriter fileWriter1=null;

        try {
            fileReader=new FileReader(srcFile);
            bufferedReader=new BufferedReader(fileReader);
            fileWriter1=new FileWriter(destFile);

            String lastBeaid = "";
            String lastPosition = "";
            String line = bufferedReader.readLine();
            while (line != null && line.length()>0) {
                StringTokenizer stringTokenizer = new StringTokenizer(line, ";");
                String newBeaId = stringTokenizer.nextToken();
                if (!lastBeaid.equals(newBeaId)) {
                    lastBeaid = newBeaId;
                    stringTokenizer.nextToken();
                    lastPosition = stringTokenizer.nextToken();
                }
                fileWriter1.write(lastPosition);
                fileWriter1.write(";");
                fileWriter1.write(line);
                fileWriter1.write("\n");
                line = bufferedReader.readLine();
            }
        }  finally {
            CloseUtil.close(fileWriter1);
            CloseUtil.close(bufferedReader);
        }

    }

    public static void main(String[] args) throws Exception {
        NoeLogTransformer noeLogTransformer = new NoeLogTransformer();
        File file = new File("/Users/pavelnovotny/projects/HashSeek-current1/build/libs/logs/noe-strednik.log");
        noeLogTransformer.processFileBeforeComputeHash(file);
    }


}
