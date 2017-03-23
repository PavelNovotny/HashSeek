package com.o2.cz.cip.hashseek.io;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.util.CloseUtil;
import net.sf.samtools.util.BlockCompressedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by Milos Frydl on 15.1.14.
 *
 * Tato trida vyrabi blokovy gz file, ktery umoznuje random pristup
 */
public class BgzUtil {
    public final static int BLOCK_INDEX_BUFFER_SIZE =300;


    static  Logger LOGGER =LoggerFactory.getLogger(BgzUtil.class);

    public final static String BGZ_SUFFIX=".bgz";
    public final static String BGZ_SUFFIX_LAST_PATTERN="\\.bgz(?!.*\\.bgz)";
    public final static String BGZ_INDEX_SUFFIX=BGZ_SUFFIX+".ind";
    public final static String BGZ_INDEX_TEMP_SUFFIX=BGZ_SUFFIX+".ind.tmp";

    /**
     *
     * @param originalFile - rozbaleny soubor, nebo klasicky gz soubor
     * @param blockSize - velikost bloku
     * @blockBufferSizeForBgzIndexFile - velikost bufferu v BgzIndex souboru. Buffer je cast indexu
     * @return zabaleny block gz soubor
     * @throws Exception
     */
    public static File createBlockCompressedGzFile(File originalFile,int blockSize,int blockBufferSizeForBgzIndexFile) throws IOException{
        LOGGER.info("Zacinam vytvaret bgz soubor pro " + originalFile.getAbsolutePath() + ", [blockSize," + blockSize + "], [blockBufferSizeForBgzIndexFile," + blockBufferSizeForBgzIndexFile + "]");
        InputStream inFileStream = new FileInputStream(originalFile);
        InputStream inStream=null;

        File bgzFile=getBgzFile(originalFile);
        if(originalFile.getName().toLowerCase().endsWith(".gz")){
            inStream=new GZIPInputStream(inFileStream, blockSize);
        }else{
            inStream=new BufferedInputStream(inFileStream);
        }

        BlockCompressedOutputStream compressedOutputStream = new BlockCompressedOutputStream(bgzFile);
        //nastavuji velikost bloku
        compressedOutputStream.setBlockSize(blockSize);

        //zde registruji listener, ktery si zapamatovava informace o blokach, ktere na konci proceduty ulozim do souboru
        File bgzIndexTmpFile=getBgzIndexTmpFile(bgzFile);
        BgzIndexFileBlockDeflateListener bgzIndexFileBlockDeflateListener=new BgzIndexFileBlockDeflateListener(bgzIndexTmpFile, blockBufferSizeForBgzIndexFile);
        //nastavuji listener na bloky
        compressedOutputStream.setBlockDeflateListener(bgzIndexFileBlockDeflateListener);

        //vytvarim bdgz soubor
        byte[] buffer = new byte[blockSize];
        int len = 0;
        int counter=0;
        while ((len = inStream.read(buffer)) > 0) {
            compressedOutputStream.write(buffer, 0, len);
            counter++;
        }
        CloseUtil.close(inStream);
        CloseUtil.close(compressedOutputStream);
        LOGGER.info("Vytvoren bgz soubor " + bgzFile.getAbsolutePath());

        BgzIndexFile.writeDataToBgzIndexFile(bgzFile, bgzIndexTmpFile, bgzIndexFileBlockDeflateListener.getBlockCount(),blockBufferSizeForBgzIndexFile,originalFile.length());

        boolean isDeleted=originalFile.delete();
        LOGGER.info("Smazani puvodniho souboru - " + originalFile.getAbsolutePath() + " isDeleted=" + isDeleted);


        return bgzFile;
    }



    /**
     * Konstrujuje bgz soubor
     * @param originalFile
     * @return
     */
    public static File getBgzFile(File originalFile){
        File bgzFile=null;
        if(originalFile.getName().toLowerCase().endsWith(".gz")){
            bgzFile=new File(originalFile.getParentFile(),originalFile.getName().replace(".gz",BGZ_SUFFIX));
        }else{
            bgzFile=new File(originalFile.getParentFile(),originalFile.getName()+BGZ_SUFFIX);
        }
        return bgzFile;
    }

    /**
     * Konstruuje bgz index soubor
     * @param originalFile
     * @return
     */
    public static File getBgzIndexFile(File originalFile){
        File dir = originalFile.getParentFile();
        try {
            dir=AppProperties.getBgzDir(originalFile.getParentFile());

        } catch (IOException t) {}

        File bgzIndexFile=null;
        if(originalFile.getName().toLowerCase().endsWith(".gz")){
            bgzIndexFile=new File(dir,originalFile.getName().replace(".gz",BGZ_INDEX_SUFFIX));
        }else{
            if(originalFile.getName().toLowerCase().endsWith(BGZ_SUFFIX)){
                bgzIndexFile=new File(dir,originalFile.getName().replaceAll(BGZ_SUFFIX_LAST_PATTERN,BGZ_INDEX_SUFFIX));
            }else{
                bgzIndexFile=new File(dir,originalFile.getName()+BGZ_INDEX_SUFFIX);
            }
        }
        return bgzIndexFile;
    }

    /**
     * Konstruuje bgz index temp soubor. do ktereho se zaisuji data z BlockDeflateListener-u a nasledne je z nej zkonstruovan v
     * @param originalFile
     * @return
     */
    public static File getBgzIndexTmpFile(File originalFile){
        File dir = originalFile.getParentFile();
        try {
            dir=AppProperties.getBgzDir(originalFile.getParentFile());

        } catch (IOException t) {}

        File bgzIndexFile=null;
        if(originalFile.getName().toLowerCase().endsWith(".gz")){
            bgzIndexFile=new File(dir,originalFile.getName().replace(".gz",BGZ_INDEX_TEMP_SUFFIX));
        }else{
            if(originalFile.getName().toLowerCase().endsWith(BGZ_SUFFIX)){
                bgzIndexFile=new File(dir,originalFile.getName().replaceAll(BGZ_SUFFIX_LAST_PATTERN,BGZ_INDEX_TEMP_SUFFIX));
            }else{
                bgzIndexFile=new File(dir,originalFile.getName()+BGZ_INDEX_TEMP_SUFFIX);
            }
        }
        return bgzIndexFile;
    }

    public static boolean isBgzFile(File file){
        return  file.getName().endsWith(BGZ_SUFFIX);
    }

    public static long fileLength(File file) {
        if (isBgzFile(file)) {
            long size=-1;

            try {
                BgzIndexFile bgzIndexFile = new BgzIndexFile(BgzUtil.getBgzIndexFile(file));
                size = bgzIndexFile.fileLength();
            } catch (Throwable t) {
                LOGGER.error("Nepodarilo se zjistit velikost BGZ souboru:" + file.getAbsolutePath(), t);
           }
            return size;
        } else {
            return file.length();
        }
    }

    public static void main(String[] args) throws Exception {
        AppProperties.getValue("fake");
        long startTime=System.currentTimeMillis();
        if(args.length!=1 && args.length!=3){
            System.out.println("Ocekavany 3 argumenty: originalFile bgzBlockSize bgzBufferSize. Tj. napr. soubor 200000 300");
            return;
        }
        File originalFile=new File(args[0]);
        int blockSize=200000;
        int bufferSize=300;
        if(args.length==3){
            blockSize=Integer.parseInt(args[1]);
            bufferSize=Integer.parseInt(args[2]);
            createBlockCompressedGzFile(originalFile,blockSize,bufferSize);
        }else{
            long originalLenght=fileLength(originalFile);
            long compressedLenght=originalFile.length();
            long compression=originalLenght/compressedLenght;
            System.out.println(compression+","+originalLenght+","+compressedLenght);
        }


        long runTime=(System.currentTimeMillis()-startTime)/1000;
        System.out.println("Doba behu:"+runTime);
    }
}
