package com.o2.cz.cip.hashseek.seek;

import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.Utils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class FileSeek {
    private static Logger LOGGER = Logger.getLogger(FileSeek.class);

    public Map<Long, Integer> rawDocLocations(List<List<String>> seekedStrings, File seekedFile, int seekLimit, PrintStream output) throws IOException {
        Map<Long, Integer> allPositions = new HashMap<Long, Integer>();
        File hashFile = new File(seekedFile.getAbsolutePath()+".hash");
        RandomAccessFile hashRaf = new RandomAccessFile(hashFile,"r");
        RandomAccessFile seekedRaf = new RandomAccessFile(seekedFile, "r");
        hashRaf.readInt(); //version
        long hashSpacePosition = hashRaf.readLong();
        int hashSpace = hashRaf.readInt();
        //todo remove in future index versions
        int blockKind = hashRaf.readInt(); //custom block or fixedBlocks
        int fixedBlockSize = hashRaf.readInt(); //in case fixedSize number of bytes per block, in case of customBlock is not needed
        long customBlockTablePosition = hashRaf.getFilePointer();
        for (List<String> andStrings : seekedStrings) {
            if (andStrings != null && andStrings.size() > 0) {
                Set<Integer> candidateBlocks = candidateBlocks(hashRaf, andStrings, hashSpace, hashSpacePosition, output, seekedFile);
                Set<Integer> finalLimitedBlockCandidates = limitBlockCandidates(candidateBlocks, seekLimit, output);
                Map<Long, Integer> finalPositions = finalPositions(finalLimitedBlockCandidates, hashRaf, customBlockTablePosition);
                verifyPositions(seekedRaf, andStrings, allPositions, finalPositions);
            }
        }
        hashRaf.close();
        seekedRaf.close();
        return allPositions;
    }

    private Set<Integer> candidateBlocks(RandomAccessFile hashRaf, List<String> andSeekStrings, int hashSpace, long hashSpacePosition, PrintStream output, File seekedFile) throws IOException {
        List<Set<Integer>> candidateBlocksList = new LinkedList<Set<Integer>>();
        for (String seekedString : andSeekStrings) {
            int hash = Utils.normalizeToHashSpace(Utils.maskSign(Utils.javaHash(seekedString)), hashSpace); // plusové číslo
            hashRaf.seek(hashSpacePosition + (hash * Utils.HASH_SPACE_RECORD_SIZE));
            long pointersPosition = hashRaf.readLong(); //pozice pointerů
            int pointersCount = hashRaf.readInt(); // počet pointerů
            Set<Integer> candidateBlocks = new HashSet<Integer>();
            candidateBlocksList.add(candidateBlocks);
            hashRaf.seek(pointersPosition); //pointry do zdrojoveho souboru na pozice custom bloků
            //později potřebujeme průnik bloků
            for (int pointerPosition = 0; pointerPosition < pointersCount; pointerPosition++) {
                int blockNumber = hashRaf.readInt(); //
                candidateBlocks.add(blockNumber);
            }
            outPrintLineSimple(output, String.format("Estimated real count for '%s' is '%s' in '%s'.", seekedString, candidateBlocks.size(), seekedFile.getAbsolutePath()));
        }
        Set<Integer> finalCandidate = intersectCandidates(candidateBlocksList);
        if (candidateBlocksList.size() > 0) {
            outPrintLineSimple(output, String.format("Estimated real count for AND condition is '%s' in '%s'.", finalCandidate.size(), seekedFile.getAbsolutePath()));
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
            outPrintLineSimple(output, String.format("Result of AND condition was REDUCED to '%s'.", seekLimit));
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

    private Map<Long, Integer> finalPositions(Set<Integer> finalLimitedCandidates, RandomAccessFile hashRaf, long customBlockTablePosition) throws IOException {
        Map<Long, Integer> finalPositions = new HashMap<Long, Integer>();
        for (Integer blockNumber : finalLimitedCandidates) {
            hashRaf.seek(customBlockTablePosition + (blockNumber * Utils.LONG_SIZE)); //zjištění pozice bloku v hledaném souboru
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
                if (Utils.indexOf(raw, seekedBytes) == -1) { //eliminujeme falešné vyhledání (kolize hashů) kontrolou zda je hledaný string v bloku.
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
