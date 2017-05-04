package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.io.BgzUtil;
import com.o2.cz.cip.hashseek.io.ReadOnlyFileChannel;

import java.io.*;

/**
 * User: Pavel
 * Date: 20.3.13 10:52
 */
public class HashFileCreator extends AbstractHashFileCreator{

    private HashBuffer hashBuffers[];
    private Collisions[] workingCollisions;

    public HashFileCreator() {
    }

    private void init() {
        if(hashBuffers==null){
            hashBuffers = new HashBuffer[HashSeekConstants.BUFFER_COUNT];
            for (int i=0; i< HashSeekConstants.BUFFER_COUNT;i++) {
               hashBuffers[i]  = new HashBuffer(i);
            }
            workingCollisions = new Collisions[HashSeekConstants.BUFFER_SIZE];
            for (int i=0; i< HashSeekConstants.BUFFER_SIZE; i++) {
                workingCollisions[i] = new Collisions();
            }
        }
    }

    @Override
    void registerTransformers() {

    }

    @Override
    public void createHashFileInner(File file) throws IOException {
        init();
        String maxIndexDepth = file.getPath().contains("_predpr_")?AppProperties.getPredprodMaxIndexDepth():AppProperties.getDefaultMaxIndexDepth();
        HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS = Integer.parseInt(maxIndexDepth);
        HashSeekConstants.HASH_FILE_LOCATION = AppProperties.getHashLocation();
        HashSeekConstants.outPrintLine(String.format("maximum word count to index (index depth) is '%s'", HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS));

        HashSeekConstants.outPrintLine(String.format("started indexing '%s'.", file.getPath()));
        if (!deleteCache()) {
            return;
        }
        File bgzFile= BgzUtil.createBlockCompressedGzFile(file, AppProperties.getBgzBlockSize(),AppProperties.getBgzIndexBufferSize());
        ReadOnlyFileChannel rof = new ReadOnlyFileChannel(bgzFile);
        int end;
        while ((end = rof.readWords()) > 0) {
            switch (end) {
                case ReadOnlyFileChannel.END_ALL:
                    putInHashTable(rof.smallWordLength, rof.javaHashSmall, rof.djb2HashSmall, rof.wordPosition);
                    if (rof.smallWordLength != rof.bigWordLength) {
                        putInHashTable(rof.bigWordLength, rof.javaHashBig, rof.djb2HashBig, rof.wordPosition);
                    }
                    rof.javaHashBig=0;
                    rof.djb2HashBig=ReadOnlyFileChannel.INITIAL_DJB2;
                    rof.bigWordLength=0;
                    break;
                case ReadOnlyFileChannel.END_SMALL:
                    putInHashTable(rof.smallWordLength, rof.javaHashSmall, rof.djb2HashSmall, rof.wordPosition);
                    break;
                default:
                    break;
            }
        }
        rof.close();
        finishHashFile(bgzFile);
        HashSeekConstants.outPrintLine(String.format("ended indexing '%s'.", file.getPath()));
    }

    private boolean deleteCache() throws IOException {
        HashSeekConstants.outPrintLine(String.format("started delete cache in '%s'.", HashSeekConstants.BUFFER_LOCATION));
        File dir = new File(HashSeekConstants.BUFFER_LOCATION);
        if(dir.exists()){
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (!file.delete()) {
                        HashSeekConstants.outPrintLine(String.format("problems deleting cache file '%s'.", file.getPath()));
                        return false;
                    }
                }
            } else {
                HashSeekConstants.outPrintLine(String.format("problems deleting cache. '%s' is not a directory.", HashSeekConstants.BUFFER_LOCATION));
                return false;
            }
        }else{
            dir.mkdirs();
        }
        HashSeekConstants.outPrintLine(String.format("ended delete cache in '%s'.", HashSeekConstants.BUFFER_LOCATION));
        return true;
    }

    private void finishHashFile(File file) throws IOException {
        for (HashBuffer buffer: hashBuffers) {
            buffer.processBuffer(workingCollisions);
        }
        writeHash(file.getPath());
    }

    protected void putInHashTable(int wordLength, int javaHash, long djb2Hash, long pointer) throws IOException {
        if (wordLength < HashSeekConstants.MIN_WORD_SIZE) {
            return;
        }
        int hashIndex = (javaHash & 0x7fffffff) % HashSeekConstants.HASH_TABLE_SIZE;
        int bufferNo = hashIndex  / (HashSeekConstants.HASH_TABLE_SIZE/ HashSeekConstants.BUFFER_COUNT);
        int bufferHashIndex = hashIndex % HashSeekConstants.BUFFER_SIZE;
        hashBuffers[bufferNo].add(workingCollisions, bufferHashIndex, djb2Hash, pointer);
    }

    private void writeHash(String originalFilePath) throws IOException {
        File originalFile=new File(originalFilePath);
        File tmpFile = new File(String.format("%s", HashSeekConstants.HASH_TMP_FILE));
        File hashFile =new File( AppProperties.getHashDir(originalFile.getParentFile()),originalFile.getName()+HashSeekConstants.HASH_FILE_SUFFIX);

        if(!hashFile.getParentFile().exists()){
            hashFile.getParentFile().mkdirs();
        }
        HashSeekConstants.outPrintLine(String.format("started writing hash to file '%s'", hashFile.getPath()));
        DataOutputStream osTmp = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(tmpFile)));
        writeBody(osTmp);
        osTmp.close();
        DataOutputStream os = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(hashFile)));
        DataInputStream is = new DataInputStream( new BufferedInputStream(new FileInputStream(tmpFile)));
        writeHeader(os);
        copyBody(is, os);
        os.close();
        is.close();
        HashSeekConstants.outPrintLine(String.format("ended writing hash to file '%s'", hashFile.getPath()));

        BgzUtil.createBlockCompressedGzFile(hashFile,AppProperties.getBgzBlockSize(),AppProperties.getBgzIndexBufferSize());
    }

    private void copyBody(DataInputStream is, DataOutputStream os) throws IOException {
        HashSeekConstants.outPrintLine(String.format("started copying hash body"));
        byte data;
        try {
            while (true) {
                data = is.readByte();
                os.writeByte(data);
            }
        } catch (EOFException e) {}
        HashSeekConstants.outPrintLine(String.format("ended copying hash body"));
    }

    private void writeHeader(DataOutputStream os) throws IOException {
        long position = 3*Integer.SIZE; //HASH_TABLE_SIZE, BUFFER_SIZE, BUFFER_COUNT
        position += HashSeekConstants.BUFFER_COUNT * (Integer.SIZE+Long.SIZE+Long.SIZE); //indexFrom, startPosition, endPosition
        position = position/Byte.SIZE; //to bytes;
        os.writeInt(HashSeekConstants.HASH_TABLE_SIZE);
        os.writeInt(HashSeekConstants.BUFFER_SIZE);
        os.writeInt(HashSeekConstants.BUFFER_COUNT);
        for (int i=0; i<hashBuffers.length; i++) {
            os.writeInt(hashBuffers[i].getBufferNo()* HashSeekConstants.BUFFER_SIZE); //indexFrom
            os.writeLong(position); //startPosition
            position += hashBuffers[i].getBytesWritten();
            os.writeLong(position); //endPosition
        }
    }


    private void writeBody(DataOutputStream os) throws IOException {
        for (int i=0; i< hashBuffers.length; i++) {
            hashBuffers[i].readCacheFileTo(workingCollisions);
            for (int hash=0; hash < workingCollisions.length; hash++) {
                int bytesWritten = workingCollisions[hash].writeHash(hash, os);
                hashBuffers[i].setBytesWritten(bytesWritten+hashBuffers[i].getBytesWritten());
            }
        }
    }

    public static void main (String args[]) throws IOException {
        HashFileCreator hashFileCreator = new HashFileCreator();
        if (args.length>1) {
            HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS = Integer.parseInt(args[1]);
        }
        if (args.length>2) {
            HashSeekConstants.HASH_FILE_LOCATION = args[2];
        }
        HashSeekConstants.outPrintLine(String.format("maximum word count to index (index depth) is '%s'", HashSeekConstants.MAX_WORD_OCCURENCES_TO_PROCESS));
        hashFileCreator.createHashFileInner(new File(args[0]));
    }

}
