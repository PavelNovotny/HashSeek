package com.o2.cz.cip.hashseek.logs.timelog;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.logs.AbstractLogTransformer;
import com.o2.cz.cip.hashseek.util.CloseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by mfrydl on 11.3.14.
 */
public class TimeLogTransformer extends AbstractLogTransformer{
    static final Logger LOGGER= LoggerFactory.getLogger(TimeLogTransformer.class);

    @Override
    public boolean canBeProcessed(File file) {
        boolean result=file.getName().contains("time");
        return result;
    }


    @Override
    public void processFileBeforeComputeHash(File file) throws Exception {
        if(HashSeekConstants.BLOCK_INDEXER_TYPE.equals(AppProperties.getIndexerType())){
            constructBlockFile(file,AppProperties.getTransformerNioBufferSize());
        }
    }

    @Override
    public void processFileAfterComputeHash(File file) throws Exception  {

    }

    private void constructBlockFile(File file,int nioBufferSize) throws Exception{
        RandomAccessFile aFile = new RandomAccessFile(file, "r");
        FileOutputStream blockFileOutputStream=null;
        DataOutputStream blockOutputStream=null;
        FileChannel inChannel = null;
        File dir = AppProperties.getBgzDir(file.getParentFile());
        File blockFile = new File(dir, file.getName() + ".blocks");
        LOGGER.info("Zaciname vytvaret block file:"+blockFile.getAbsolutePath());
        try {

            blockFileOutputStream=new FileOutputStream(blockFile);
            blockOutputStream=new DataOutputStream(blockFileOutputStream);
            inChannel=aFile.getChannel();

            ByteBuffer byteBuffer = ByteBuffer.allocate(nioBufferSize);


            //counter pozice v souboru,
            long position = -1;
            //true - nejedna se o radek kde zacina beaId, takze nemusim pak zbytek cist a muzu preskocit az na konec radku
            boolean waitForNextLine = false;

            //uz jsem identifikoval beaId
            boolean readBeaId = false;

            while (inChannel.read(byteBuffer) > 0) {
                byteBuffer.flip();

                for (int i = 0; i < byteBuffer.limit(); i++) {
                    position++;
                    char readedChar = (char) byteBuffer.get();
                    if (readedChar == HashSeekConstants.END_LINE_CHARACTER) {
                        waitForNextLine = false;

                    } else {
                        if (!waitForNextLine) {
                            //novy radek zacina hned '[' jedna se o zacatek time logu
                            if (readedChar == '[') {
                                blockOutputStream.writeLong(position - 1);
                            } else {
                                waitForNextLine = true;
                            }
                        }
                    }

                }
                byteBuffer.clear();
            }
            position++;
            blockOutputStream.writeLong(position);

            LOGGER.info("vytvoren block file:"+blockFile.getAbsolutePath()+"| Velikost souboru originalni:"+file.length()+" a spocitana:" + position);
        } catch (Exception t) {
            throw  t;
        } finally {
            CloseUtil.close(inChannel);
            CloseUtil.close(aFile);
            CloseUtil.close(blockOutputStream);
        }
    }

}
