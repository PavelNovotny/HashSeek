package com.o2.cz.cip.hashseek.io;

import com.o2.cz.cip.hashseek.util.CloseUtil;
import net.sf.samtools.util.BlockCompressedFilePointerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mfrydl on 18.1.14.
 */
public class BgzIndexFile {
    static Logger LOGGER = LoggerFactory.getLogger(BgzIndexFile.class);

    File bgzIndexFile;

    List<BgzIndexHeaderPointer> headerBlockPointers=new ArrayList<BgzIndexHeaderPointer>();
    List<BgzPointer> bufferPointers=new ArrayList<BgzPointer>();

    class BgzIndexHeaderPointer{
        /**
         * adresa v bgz souboru (tj. adresa v komprimovanych datech)
         */
        long bgzAddr;
        /**
         * adresa v rozbalenem souboru
         */
        long uncompressAddr;
        /**
         * kde zacina buffer bloku v bgzIndexFile souboru
         */
        long startPosition;
        /**
         * pocet bloku v bufferu
         */
        int rowCount;
    }

    class BgzPointer{
        /**
         * adresa v bgz souboru (tj. adresa v komprimovanych datech)
         */
        long bgzAddr;
        /**
         * adresa v rozbalenem souboru
         */
        long uncompressAddr;
    }

    public BgzIndexFile(File bgzIndexFile)throws IOException  {
        this.bgzIndexFile = bgzIndexFile;
        loadHeader();
    }

    public void loadHeader() throws IOException {
        headerBlockPointers.clear();
        FileInputStream fileInputStream=new FileInputStream(bgzIndexFile);
        DataInputStream dataInputStream=new DataInputStream(fileInputStream);
        int blockBufferCount=dataInputStream.readInt();
        for (int i = 0; i < blockBufferCount; i++) {
            BgzIndexHeaderPointer bgzIndexHeaderPointer=new BgzIndexHeaderPointer();
            bgzIndexHeaderPointer.bgzAddr=dataInputStream.readLong();
            bgzIndexHeaderPointer.uncompressAddr =dataInputStream.readLong();
            bgzIndexHeaderPointer.startPosition=dataInputStream.readLong();
            bgzIndexHeaderPointer.rowCount=dataInputStream.readInt();
            headerBlockPointers.add(bgzIndexHeaderPointer);
        }
        CloseUtil.close(dataInputStream);
    }

    /**
     * Nahravam konkretni buffer bloku do bufferPointers
     * @param lastLowerIndex
     * @throws java.io.IOException
     */
    private void loadBufferOfBlocks(int lastLowerIndex) throws IOException {
        bufferPointers.clear();
        BgzIndexHeaderPointer bgzIndexHeaderPointer=headerBlockPointers.get(lastLowerIndex);
        RandomAccessFile randomAccessFile=new RandomAccessFile(bgzIndexFile,"r");
        randomAccessFile.seek(bgzIndexHeaderPointer.startPosition);
        for (int i = 0; i < bgzIndexHeaderPointer.rowCount; i++) {
            BgzPointer bgzPointer=new BgzPointer();
            bgzPointer.bgzAddr=randomAccessFile.readLong();
            bgzPointer.uncompressAddr =randomAccessFile.readLong();
            bufferPointers.add(bgzPointer);
        }
        CloseUtil.close(randomAccessFile);
    }

    public long fileLength() {
        RandomAccessFile randomAccessFile=null;
        long size=-1;
        try {
            randomAccessFile = new RandomAccessFile(bgzIndexFile, "r");
            randomAccessFile.seek(bgzIndexFile.length() - Long.SIZE/Byte.SIZE);
            size = randomAccessFile.readLong();
        } catch (Throwable t) {
            LOGGER.error("fileLength",t);
        } finally {
            CloseUtil.close(randomAccessFile);
        }
        return size;
    }

    /**
     * znam skutecnou pozici a chci najit adresu bgz bloku
     * @param position - pozice v uncompressed souboru
     * @return - filePointer na adresu gz bloku + offset na pozici v uncompressed souboru
     */
    public long findBlockGzPointer(long position) throws IOException{
        LOGGER.debug("findBlockGzPointer-" + position);
        long pos=0;

        //zjistuji zda neni v nactenem bufferu bloku
        if(bufferPointers!=null && bufferPointers.size()>0 && position>=bufferPointers.get(0).uncompressAddr && position<=bufferPointers.get(bufferPointers.size()-1).uncompressAddr){
            pos=findBlockGzPointerInBuffer(position);
            return pos;
        }else{
            //hledam v jakem bufferu se nachazi
            int lastLowerIndex=-1;

            for (int i = 0; i < headerBlockPointers.size(); i++) {
                BgzIndexHeaderPointer bgzIndexHeaderPointer=headerBlockPointers.get(i);
                if(position>=bgzIndexHeaderPointer.uncompressAddr){
                    lastLowerIndex=i;
                }else{
                    break;
                }
            }
            loadBufferOfBlocks(lastLowerIndex);
            pos=findBlockGzPointerInBuffer(position);
        }

        return pos;
    }

    /**
     * Hledam Pointer v ramci buffer-u bloku
     * @param position - hledana pozice
     * @return vraci nalezeny GzPointer
     */
    private long findBlockGzPointerInBuffer(long position) {
        int lastLowerIndex=0;

        for (int i = 0; i < bufferPointers.size(); i++) {
            BgzPointer bgzPointer =  bufferPointers.get(i);
            if(position>=bgzPointer.uncompressAddr){
                lastLowerIndex=i;
            }else{
                break;
            }
        }
        int uncompressedOffset=(int)(position-bufferPointers.get(lastLowerIndex).uncompressAddr);
        long pos= BlockCompressedFilePointerUtil.makeFilePointer((bufferPointers.get(lastLowerIndex).bgzAddr), uncompressedOffset);
        return pos;
    }


    public long findUncompressedPointer(long position) throws IOException{
        long blockAddress=BlockCompressedFilePointerUtil.getBlockAddress(position);
        long pos=0;
        //zjistuji zda neni v nactenem bufferu bloku
        if(bufferPointers!=null && bufferPointers.size()>0 && blockAddress>=bufferPointers.get(0).bgzAddr && blockAddress<=bufferPointers.get(bufferPointers.size()-1).bgzAddr){
            pos=findUncompressedPointerInBuffer(position, blockAddress);
        }else{
            //hledam v jakem bufferu se nachazi
            int lastLowerIndex=-1;

            for (int i = 0; i < headerBlockPointers.size(); i++) {
                BgzIndexHeaderPointer bgzIndexHeaderPointer=headerBlockPointers.get(i);
                if(blockAddress>=bgzIndexHeaderPointer.bgzAddr){
                    lastLowerIndex=i;
                }else{
                    break;
                }
            }
            loadBufferOfBlocks(lastLowerIndex);
            pos=findUncompressedPointerInBuffer(position, blockAddress);
        }
        LOGGER.debug("findUncompressedPointer-" + pos);
        return pos;
    }

    /**
     * Hledam Pointer v ramci buffer-u bloku
     * @param position - hledana pozice
     * @param blockAddress - adresa bloku
     * @return vraci nalezeny GzPointer
     */
    private long findUncompressedPointerInBuffer(long position,long blockAddress) {
        int lastLowerIndex=0;

        for (int i = 0; i < bufferPointers.size(); i++) {
            BgzPointer bgzPointer =  bufferPointers.get(i);
            if(blockAddress>=bgzPointer.bgzAddr){
                lastLowerIndex=i;
            }else{
                break;
            }
        }
        long pos= bufferPointers.get(lastLowerIndex).uncompressAddr + BlockCompressedFilePointerUtil.getBlockOffset(position);
        return pos;
    }


    /**
     * Vytvari BgzIndexFile - obsahuje hlavicku ktera obsahuje odkazy na buffer bloku (skupina bloku o poctu blockBufferSizeForBgzIndexFile)
     * @param bgzFile - bgz file
     * @param bgzIndexTmpFile - temporary file obsahujici seznam bloku ve formatu bgzAddresd,uncompressAddress
     * @param blockCount - pocet bloku v bgzIndexTmpFile
     * @param blockBufferSizeForBgzIndexFile - kolik bloku mam sloucit do jednoho bufferu
     * @throws java.io.IOException
     */
    public static void writeDataToBgzIndexFile(File bgzFile, File bgzIndexTmpFile,int blockCount,int blockBufferSizeForBgzIndexFile,long originalFileLength) throws IOException {
/*        List<Long> blocksGzipAddrs=null;
        List<Long> blocksRealAddrs=null;*/
        RandomAccessFile randomAccessFile=null;
        DataOutputStream dataOutputStream=null;
        File bgzIndexFile=null;
        try {
            randomAccessFile = new RandomAccessFile(bgzIndexTmpFile, "r");
            //vytvarim indexovy soubor
            bgzIndexFile = BgzUtil.getBgzIndexFile(bgzFile);
            if (bgzIndexFile == null) {
                throw new IOException("Could not create bgzIndexFile. Original file must ends with gz or bgz");
            }

            FileOutputStream fout = new FileOutputStream(bgzIndexFile);
            dataOutputStream = new DataOutputStream(fout);

            int blockBufferCount = blockCount / blockBufferSizeForBgzIndexFile;
            if (blockCount % blockBufferSizeForBgzIndexFile > 0) {
                //na konci bude jeden mensi buffer
                blockBufferCount++;
            }

            //inexy zapisuju tak,seznam indexu rozdelim do bufferu po blockBufferSizeForBgzIndexFile zaznamech. Na zacatek indexu dam hlavicku ktera mi rika kde zacina konkretni buffer.
            long positionHeader = Integer.SIZE; //HASH_TABLE_SIZE, BUFFER_SIZE, BUFFER_COUNT
            dataOutputStream.writeInt(blockBufferCount); //pocet bufferu bloku
            //spocitam kolik byte bude zabirat hlavicka
            positionHeader += blockBufferCount * (Long.SIZE + Long.SIZE + Long.SIZE + Integer.SIZE); //bgzAddress,decompressAddress, startPosition, rowCount
            positionHeader = positionHeader / Byte.SIZE; //to bytes, urcuje konec header, tj. misto kde zacinaji jednotliva data pro konkretni buffer;
            LOGGER.info("BgzIndexHeader:[positionHeader," + positionHeader + "],[blockCount," + blockCount + "],[blockBufferCount," + blockBufferCount + "]");
            long positionBody = 0;
            //zapisuju hlavicku
            for (int i = 0; i < blockBufferCount; i++) {
                randomAccessFile.seek(positionBody);
                long gzipAddr = randomAccessFile.readLong();
                long realAddr = randomAccessFile.readLong();
                dataOutputStream.writeLong(gzipAddr); //bgzAddress
                dataOutputStream.writeLong(realAddr); //decompressAddress
                long positionStart=positionHeader + positionBody;
                dataOutputStream.writeLong(positionStart); //startPosition
                int rowCount=blockBufferSizeForBgzIndexFile;
                if (i == (blockBufferCount - 1)) {
                    //posledni blok bude mit nejspise mensi velikost
                    rowCount=blockCount - i * blockBufferSizeForBgzIndexFile;
                    positionBody += (Long.SIZE + Long.SIZE) * (rowCount) / Byte.SIZE;
                } else {
                    positionBody += (Long.SIZE + Long.SIZE) * blockBufferSizeForBgzIndexFile / Byte.SIZE;
                }
                long positionEnd=positionHeader + positionBody;
                dataOutputStream.writeInt(rowCount); //rowCount
                if(LOGGER.isDebugEnabled()){
                    LOGGER.debug("BgzIndexHeader:[gzipAddr,"+gzipAddr+"],[realAddr,"+realAddr+"],[positionStart,"+positionStart+"],[positionEnd,"+positionEnd+"],[rowCount,"+rowCount+"]");
                }
            }

            //zapisuju body - jednotlive odkazy
            randomAccessFile.seek(0);
            for (int i = 0; i < blockCount; i++) {
                long gzipAddr = randomAccessFile.readLong();
                long realAddr = randomAccessFile.readLong();
                dataOutputStream.writeLong(gzipAddr);
                dataOutputStream.writeLong(realAddr);
            }
            //zapisuju delku originalniho souboru
            dataOutputStream.writeLong(originalFileLength);
            CloseUtil.close(randomAccessFile);
            CloseUtil.close(dataOutputStream);
            LOGGER.info("Vytvoren bgz.ind soubor " + bgzIndexFile.getAbsolutePath());

            boolean isDeleted=bgzIndexTmpFile.delete();
            LOGGER.info("Smazan bgz.indtmp soubor " + bgzIndexTmpFile.getAbsolutePath() + ". Vysledek mazani=" + isDeleted);
        } catch (Throwable t) {
            CloseUtil.close(randomAccessFile);
            CloseUtil.close(dataOutputStream);

            throw new IOException(t);
        }
    }
}
