package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.BlockSeekUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class BlockSeek {
    private static Logger LOGGER = Logger.getLogger(BlockSeek.class);

    public Map<Long, Integer> seekForPositions(List<List<String>> seekedStrings,  File seekedFile, int seekLimit, PrintStream output) throws IOException {
        Map<Long, Integer> allPositions = new HashMap<Long, Integer>();
        File hashFile = new File(seekedFile.getAbsolutePath()+".hash");
        RandomAccessFile hashRaf = new RandomAccessFile(hashFile,"r");
        RandomAccessFile seekedRaf = new RandomAccessFile(seekedFile, "r");
        hashRaf.readInt(); //version
        long hashSpacePosition = hashRaf.readLong();
        int hashSpace = hashRaf.readInt();
        int blockKind = hashRaf.readInt(); //custom block or fixedBlocks
        int fixedBlockSize = hashRaf.readInt(); //in case fixedSize number of bytes per block, in case of customBlock is not needed
        long customBlockTablePosition = hashRaf.getFilePointer();
        for (List<String> andStrings : seekedStrings) {
            if (andStrings != null && andStrings.size() > 0) {
                Set<Integer> candidateBlocks = candidateBlocks(hashRaf, andStrings, hashSpace, hashSpacePosition, blockKind, fixedBlockSize, output, seekedFile);
                Set<Integer> finalLimitedBlockCandidates = limitBlockCandidates(candidateBlocks, seekLimit, output);
                Map<Long, Integer> finalPositions = finalPositions(finalLimitedBlockCandidates, hashRaf, seekedRaf, andStrings, blockKind, fixedBlockSize, customBlockTablePosition);
                verifyPositions(seekedRaf, andStrings, allPositions, finalPositions);
            }
        }
        hashRaf.close();
        seekedRaf.close();
        return allPositions;
    }

    public static void main(String[] args) throws IOException {
        List<List<String>> seekedStrings = new ArrayList<List<String>>();
        List<String> andStrings = new ArrayList<String>();
        andStrings.add("00000000000000000120700464");
        seekedStrings.add(andStrings);
        File seekFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19");
        File indexFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        BlockSeek blockSeek = new BlockSeek();
        Map<Long, Integer> positions = blockSeek.seekForPositions(seekedStrings, seekFile,  100, System.out);
    }

    private Set<Integer> candidateBlocks(RandomAccessFile hashRaf, List<String> andSeekStrings, int hashSpace, long hashSpacePosition, int blockKind, int fixedBlockSize, PrintStream output, File seekedFile) throws IOException {
        List<Set<Integer>> candidateBlocksList = new LinkedList<Set<Integer>>();
        for (String seekedString : andSeekStrings) {
            int hash = BlockSeekUtil.normalizeToHashSpace(BlockSeekUtil.maskSign(BlockSeekUtil.javaHash(seekedString)), hashSpace); // plusové číslo
            hashRaf.seek(hashSpacePosition + (hash * BlockSeekUtil.HASH_SPACE_RECORD_SIZE));
            long pointersPosition = hashRaf.readLong(); //pozice pointerů
            int pointersCount = hashRaf.readInt(); // počet pointerů
            Set<Integer> candidateBlocks = new HashSet<Integer>();
            candidateBlocksList.add(candidateBlocks);
            if (blockKind ==2) { //fixed blockseek
                int blockSize = fixedBlockSize + BlockSeekUtil.MAX_WORD_SIZE;//přesah kvůli tomu, že u fixed bloků může být hledané slovo na hranici.
                hashRaf.seek(pointersPosition);
                //později potřebujeme průnik bloků
                for (int pointerPosition = 0; pointerPosition < pointersCount; pointerPosition++) {
                    int blockNumber = hashRaf.readInt(); //
                    long position = (long) (blockNumber * fixedBlockSize);
                    candidateBlocks.add(blockNumber);
                }
            } else if (blockKind ==1) { //custom blockseek
                hashRaf.seek(pointersPosition); //pointry do zdrojoveho souboru na pozice custom bloků
                //později potřebujeme průnik bloků
                for (int pointerPosition = 0; pointerPosition < pointersCount; pointerPosition++) {
                    int blockNumber = hashRaf.readInt(); //
                    candidateBlocks.add(blockNumber);
                }
            }
            outPrintLineSimple(output, String.format("Estimated real count for '%s' is '%s' in '%s'.", seekedString, candidateBlocks.size(), seekedFile.getAbsolutePath()));
        }
        Set<Integer> finalCandidate = intersectCandidates(candidateBlocksList);
        if (candidateBlocksList.size() > 0) {
            outPrintLineSimple(output, String.format("Estimated real count for AND condition is '%s' in '%s'.",  finalCandidate.size(),  seekedFile.getAbsolutePath()));
        }
        return finalCandidate;
    }


    private Set<Integer> limitBlockCandidates(Set<Integer> candidates, int seekLimit, PrintStream output) {
        Set<Integer> limitedCandidates = new HashSet<Integer>();
        Iterator it = candidates.iterator();
        int i=0;
        while (it.hasNext()) {
            if (seekLimit > i++) {
                limitedCandidates.add((Integer) it.next());
            } else {
                break;
            }
        }
        if (candidates.size() > seekLimit) {
            outPrintLineSimple(output, String.format("Result of AND condition was REDUCED to '%s'.",  seekLimit));
        } else {
            outPrintLineSimple(output, String.format("Result of AND condition was NOT reduced, but further restrictions are possible."));
        }
        return limitedCandidates;
    }

    private Set<Integer> intersectCandidates(List<Set<Integer>> candidates) {
        Set<Integer> finalCandidate;
        if (candidates.size() > 0) {
            finalCandidate = new HashSet<Integer>(candidates.get(0));
            for (Set<Integer> candidate : candidates) {
                finalCandidate.retainAll(candidate);
            }
        } else {
            finalCandidate = new HashSet<Integer>();
        }
        return finalCandidate;
    }

    private Map<Long, Integer> finalPositions(Set<Integer> finalLimitedCandidates, RandomAccessFile hashRaf, RandomAccessFile seekedRaf, List<String> andStrings, int blockKind, int fixedBlockSize, long customBlockTablePosition) throws IOException {
        Map<Long, Integer> finalPositions = new HashMap<Long, Integer>();
        for (Integer blockNumber : finalLimitedCandidates) {
            hashRaf.seek(customBlockTablePosition + (blockNumber * BlockSeekUtil.LONG_SIZE)); //zjištění pozice bloku v hledaném souboru
            long blockPosition = hashRaf.readLong();
            long nextBlockPosition = hashRaf.readLong();
            int customBlockSize = (int)(nextBlockPosition - blockPosition); //bez přesahu, předpokládáme, že custom blok je inteligentně udělaný
            finalPositions.put(blockPosition, customBlockSize);
        }
        return finalPositions;
    }

    private void verifyPositions(RandomAccessFile seekedRaf, List<String> andSeekStrings, Map<Long, Integer> allPositions, Map<Long, Integer> finalCandidates) throws IOException {
        for (Long position : finalCandidates.keySet()) {
            seekedRaf.seek(position);
            Integer blockSize = finalCandidates.get(position);
            byte[] raw = seekedRaf.readRawBytes(blockSize);
            boolean found = true;
            for (String seekedString : andSeekStrings) {
                byte[] seekedBytes = seekedString.getBytes("UTF-8");
                if (BlockSeekUtil.indexOf(raw, seekedBytes) == -1) { //eliminujeme falešné vyhledání (kolize hashů) kontrolou zda je hledaný string v bloku.
                    found = false;
                    break;
                }
            }
            if (found) {
                allPositions.put(position, blockSize);
            }
        }
    }

    public static void outPrintLineSimple(PrintStream output, String line) {
        LOGGER.info(line);
        if(System.out==output){
            LOGGER.info(line);
        }else{
            output.println(line);
            output.flush();
        }
    }

}
