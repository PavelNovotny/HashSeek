package com.o2.cz.cip.hashseek.logs;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import com.o2.cz.cip.hashseek.sort.ExternalSort;
import com.o2.cz.cip.hashseek.util.CloseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Abstraktni trida pro listenery, ktere nejakym zpusobem transformuji logove soubory pred a po pocitani hashu
 * Created by mfrydl on 11.3.14.
 */
public abstract class AbstractLogTransformer {
    static final Logger LOGGER= LoggerFactory.getLogger(AbstractLogTransformer.class);

    public abstract boolean canBeProcessed(File file);

    public abstract void processFileBeforeComputeHash(File file) throws Exception ;

    public abstract void processFileAfterComputeHash(File file) throws Exception ;

    protected void performExtraSort(File fromFile, File toFile,int maxTmpFiles,  boolean distinct, int headersize, Charset charset) throws IOException {
        List<File> l = ExternalSort.sortInBatch(fromFile, ExternalSort.defaultcomparator, maxTmpFiles, charset, fromFile.getParentFile(), distinct, headersize, false);
        ExternalSort.mergeSortedFiles(l, toFile, ExternalSort.defaultcomparator, charset, distinct, false, false);
    }


    public String convertLongToStringWithFixLenght(long p_number,int countOfDigits){
        String s_number=String.valueOf(p_number);
        StringBuffer buf = new StringBuffer();
        int count = countOfDigits - s_number.length();
        for (int i = 0; i < count; i++) {
            buf.append('0');
        }
        buf.append(s_number);
        return buf.toString();
    }

    public int countOfSigninNumber(long cislo)
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

    /**
     * tato metoda pridava pred beaid pozici, aby az to nasledne srovnam, tak aby byli jednotliva flow za sebou cca tak jak prisli requestove zpravy
     * @param srcFile
     * @param destFile
     * @throws IOException
     */
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

    /**
     *
     * @param logFile -zdrojovy soubor
     * @param logBeaIdFile - soubor se sesortovanyma pozicema pro beaid
     * @throws Exception
     */
    protected void constructSortedFile(File logFile,File logBeaIdFile) throws Exception{
        //
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
}
