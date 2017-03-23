package com.o2.cz.cip.hashseek.io;

import net.sf.samtools.util.BlockDeflateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Tato trida slouzi k zapsani adres na vytvorene bloky v bgz souboru. Listener adresy zapisuje do tmp souboru, ktery je nasledne ve tride BgzIndexFile
 * rozsiren o hlavicku, ktera umozni rychlejsi pristup.
 *
 * Created by mfrydl on 19.1.14.
 */

public class BgzIndexFileBlockDeflateListener implements BlockDeflateListener {
    static Logger logger = LoggerFactory.getLogger(BgzIndexFileBlockDeflateListener.class);

    File file;
    DataOutputStream dataOutputStream;
    int blockBufferSizeForBgzIndexFile;

    long totalSize = 0;
    long totalCompress = 0;
    String lastBlockText = "";
    int blockCount = 0;


    public BgzIndexFileBlockDeflateListener(File file,int blockBufferSizeForBgzIndexFile) throws IOException {
        this.file = file;
        this.blockBufferSizeForBgzIndexFile=blockBufferSizeForBgzIndexFile;
        FileOutputStream fout = new FileOutputStream(file);
        dataOutputStream = new DataOutputStream(fout);
    }

    public File getFile() {
        return file;
    }

    public int getBlockCount() {
        return blockCount;
    }

    @Override
    public void onDeflateBlock(long mBlockAddress, int totalBlockSize, int bytesToCompress, long blockCounter, long filePointer) {

        try {
            dataOutputStream.writeLong(mBlockAddress);
            dataOutputStream.writeLong(totalSize);
            blockCount++;
        } catch (Throwable t) {
            logger.error("Could not write BgzIndexTempFile", t);
            throw new RuntimeException(t);
        }

        totalSize += bytesToCompress;
        totalCompress += totalBlockSize;
        lastBlockText = "[cislo bloku:" + blockCounter + "]-[adresa bloku:" + mBlockAddress + "]-[velikost gzip bloku:" + totalBlockSize + "(" + totalCompress + ")]-[velikost nekompr. dat v bloku:" + bytesToCompress + "(" + totalSize + ")]-" + filePointer;
        if(logger.isDebugEnabled()){
            if ((blockCounter-1) % (blockBufferSizeForBgzIndexFile) == 0) {
                logger.info(lastBlockText);
            }
        }
    }

    @Override
    public void onClose() {
        if (dataOutputStream != null) {
            logger.info("Posledni blok: "+lastBlockText);
            try {
                dataOutputStream.close();
                dataOutputStream = null;
                logger.info("Closed BgzIndexTmpFile:" + file.getAbsolutePath());

            } catch (Exception e) {
                logger.error("Cound not close DataOutputStream to file:" + file.getAbsolutePath());
            }
        }
    }
}
