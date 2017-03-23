package com.o2.cz.cip.hashseek.logs.bpmlog;

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
import java.util.StringTokenizer;

/**
 * Created by mfrydl on 11.3.14.
 */
public class BpmLogTransformer extends AbstractLogTransformer{
    static final Logger LOGGER= LoggerFactory.getLogger(BpmLogTransformer.class);

    @Override
    public boolean canBeProcessed(File file) {
        boolean result=file.getName().contains("bpm");
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
                        waitForNextLine = false;
                        readBeaId = false;
                    } else {
                        if (!waitForNextLine) {
                            if (readBeaId) {
                                if (lineBeaId.length() > 0 && lineBeaId.charAt(lineBeaId.length() - 1) == ';') {
                                    //LOGGER.info(lineDate.toString() + lineTime.toString() + lineThread.toString() + lineBeaId.toString());
                                    waitForNextLine = true;
                                    if (lastEndLine > 0 && writedFirstBeaId) {
                                        fileWriter.write(";" + convertLongToStringWithFixLenght(lastEndLine, countOfDigits) + "\n");
                                    }
                                    fileWriter.write(lineBeaId.toString());
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




}
