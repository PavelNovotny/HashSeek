package com.o2.cz.cip.hashseek.logs.timelog;



import com.o2.cz.cip.hashseek.sort.ExternalSort;
import com.o2.cz.cip.hashseek.util.CloseUtil;
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
public class SortTimeLogUtil {
    public static final char END_LINE_CHARACTER = '\n';
    static final Logger LOGGER= LoggerFactory.getLogger(SortTimeLogUtil.class);

    public static void findBeaIdsPosition(String fileName,int nioBufferSize, int maxTmpFiles) throws Exception{
        File file = new File(fileName);
        RandomAccessFile aFile = new RandomAccessFile(file, "r");
        FileOutputStream blockFileOutputStream=new FileOutputStream(new File(file.getParentFile(),file.getName()+".blocks"));
        DataOutputStream blockOutputStream=new DataOutputStream(blockFileOutputStream);

        FileChannel inChannel = aFile.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(nioBufferSize);


        //counter pozice v souboru,
        long position=-1;
        //true - nejedna se o radek kde zacina beaId, takze nemusim pak zbytek cist a muzu preskocit az na konec radku
        boolean waitForNextLine = false;

        //uz jsem identifikoval beaId
        boolean readBeaId = false;

        while (inChannel.read(byteBuffer) > 0) {
            byteBuffer.flip();

            for (int i = 0; i < byteBuffer.limit(); i++) {
                position++;
                char readedChar = (char) byteBuffer.get();
                if (readedChar == END_LINE_CHARACTER) {
                    waitForNextLine = false;

                } else {
                    if (!waitForNextLine) {
                        //novy radek zacina hned '[' jedna se o zacatek time logu
                        if(readedChar=='['){
                            blockOutputStream.writeLong(position-1);
                        }else{
                            waitForNextLine = true;
                        }
                    }
                }

            }
            byteBuffer.clear(); // do something with the data and clear/compact it.
        }

        inChannel.close();
        aFile.close();


        blockOutputStream.writeLong(position);
        position++;
        LOGGER.info("spocitany konec souboru:" + position);
        LOGGER.info("skutecny konec souboru:"+file.length());
        blockOutputStream.close();
    }


    public static void testBlocks(String fileName) throws Exception{
        File file = new File(fileName);
        File fileBlock =new File(file.getParentFile(),file.getName()+".blocks");
        RandomAccessFile raf = new RandomAccessFile(file,"r");
        FileInputStream blockFileInputStream=new FileInputStream(new File(file.getParentFile(),file.getName()+".blocks"));
        DataInputStream blockInputStream=new DataInputStream(blockFileInputStream);
        long previousPos=-1;
        int size;
        int remainSize=0;
        byte [] bytes=new byte [1024];
        try {
            while (true) {
                long customBlockAddress = blockInputStream.readLong();
                remainSize=(int)(customBlockAddress-previousPos);
                if(previousPos>=0){
                    System.out.println("----------------------");
                    raf.seek(previousPos);
                    while(remainSize>0){
                        size=raf.read(bytes);
                        if(size>remainSize){
                            System.out.println(new String(bytes,0,remainSize,"UTF-8"));
                            remainSize=0;
                        }else{
                            System.out.println(new String(bytes,0,size,"UTF-8"));
                            remainSize-=size;
                        }
                    }
                }
                previousPos=customBlockAddress;
                //System.out.println(customBlockAddress);
            }
        } catch (EOFException e) {
        } finally {
            CloseUtil.close(blockInputStream);
            CloseUtil.close(raf);
        }


    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3){
            LOGGER.info("Ocekavany 3 srgumenty: String fileName,int nioBufferSize, int maxTmpFiles  | priklad 10240000 10");
            return;
        }
        long startTime=System.currentTimeMillis();
        int nioBufferSize=Integer.parseInt(args[1]);
        int maxTmpFiles=Integer.parseInt(args[2]);
        testBlocks(args[0]);
        findBeaIdsPosition(args[0],nioBufferSize,maxTmpFiles);
        long runTime=(System.currentTimeMillis()-startTime)/1000;
        LOGGER.info("Doba behu:"+runTime);
    }
}
