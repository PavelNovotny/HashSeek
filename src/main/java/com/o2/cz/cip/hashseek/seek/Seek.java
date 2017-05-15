package com.o2.cz.cip.hashseek.seek;

import com.o2.cz.cip.hashseek.analyze.Analyzer;
import com.o2.cz.cip.hashseek.analyze.AnalyzerFactory;
import com.o2.cz.cip.hashseek.datastore.ExtractData;
import com.o2.cz.cip.hashseek.datastore.ExtractDataFactory;
import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.util.Utils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class Seek {
    //todo best score
    private static Logger LOGGER = Logger.getLogger(Seek.class);
    private File dataFile;
    private File indexFile;
    private int seekLimit;
    private ExtractData extractData;
    private Analyzer analyzer;
    private List<String> seekStrings;
    private byte[][] analyzed;

    //todo json výstup
    //todo REST dotaz
    //todo pro highlight předávat hodnoty analyzed.

    public Seek(File indexFile, String dataStoreKind, String analyzerKind, int seekLimit) throws FileNotFoundException {
        this.indexFile = indexFile;
        //todo lepší zjištění datového souboru (z indexu?)
        String indexFilePath = indexFile.getAbsolutePath();
        this.dataFile = new File(indexFilePath.substring(0, indexFilePath.length()-5));
        this.seekLimit = seekLimit;
        //todo možná vysunout a vytvářet instance mimo tuto třídu
        this.extractData = ExtractDataFactory.createInstance(dataStoreKind);
        this.extractData.setDataFile(this.dataFile);
        this.analyzer = AnalyzerFactory.createInstance(analyzerKind);
    }

    public List<DataDocument> seek (String seekString) throws IOException {
        RandomAccessFile hashRaf = new RandomAccessFile(indexFile,"r");
        hashRaf.readInt(); //version
        long hashSpacePosition = hashRaf.readLong();
        int hashSpace = hashRaf.readInt();
        //todo remove in future index versions
        int blockKind = hashRaf.readInt(); //custom block or fixedBlocks
        int fixedBlockSize = hashRaf.readInt(); //in case fixedSize number of bytes per block, in case of customBlock is not needed
        long customBlockTablePosition = hashRaf.getFilePointer();
        this.analyzed = analyzer.analyze(seekString.getBytes("UTF-8"));
        Map<Integer, IndexDocument> indexDocuments =  indexDocuments(hashRaf, hashSpace, hashSpacePosition);
        List<DataDocument> scoredDocuments = computeScore(indexDocuments, hashRaf, customBlockTablePosition);
        List<DataDocument> resultDocuments = filterFinal(scoredDocuments);
        hashRaf.close();
        return resultDocuments;
    }

    public JSONObject result(String seekString) throws IOException {
        List<DataDocument> documents = seek(seekString);
        JSONObject obj=new JSONObject();
        JSONArray documentsJSON = new JSONArray();
        JSONArray analyzedJSON = new JSONArray();
        for (DataDocument document : documents) {
            documentsJSON.add(document.getJSON());
        }
        for (byte[] word : this.analyzed) {
            analyzedJSON.add(new String(word));
        }
        obj.put("documents",documentsJSON);
        obj.put("analyzed", analyzedJSON);
        System.out.println(obj.toString());
        return obj;
    }

    private List<DataDocument> filterFinal(List<DataDocument> scoredDocuments) {
        List<DataDocument> documents = new ArrayList<DataDocument>();
        int score = 0;
        for (DataDocument dataDocument : scoredDocuments) {
            if (dataDocument.getScore() < score) {
                break;
            }
            score = dataDocument.getScore();
            documents.add(dataDocument);
            System.out.println(String.format("score:%02d%%", this.analyzed.length));
            System.out.println(new String(dataDocument.getDocument()));
        }
        return documents;
    }


    private List<DataDocument> computeScore(Map<Integer, IndexDocument> fileDocuments, RandomAccessFile hashRaf, long customBlockTablePosition) throws IOException {
        RandomAccessFile dataRaf = new RandomAccessFile(dataFile,"r");
        List<DataDocument> dataDocumentList = new ArrayList<DataDocument>();
        List<IndexDocument> indexDocumentList = new ArrayList<IndexDocument>();
        indexDocumentList.addAll(fileDocuments.values());
        Collections.sort(indexDocumentList, Collections.reverseOrder()); //předběžný scoring na základě počtu hashů
        //finální scoring - načteme documenty podle pořadí předběžného scoringu, spočítáme reálný scoring
        int maxScore = 1;
        for (IndexDocument indexDocument : indexDocumentList) {
            hashRaf.seek(customBlockTablePosition + (indexDocument.hashCode() * Utils.LONG_SIZE)); //zjištění pozice bloku v hledaném souboru
            long docOffset = hashRaf.readLong();
            long nextDocOffset = hashRaf.readLong();
            int docSize = (int)(nextDocOffset - docOffset);
            dataRaf.seek(docOffset);
            byte[] data = dataRaf.readRawBytes(docSize);
            int score = 0;
            for (int i=0; i<this.analyzed.length; i++) {
                byte[] word = this.analyzed[i];
                if (Utils.indexOf(data, word) > -1) { //hledané slovo je opravdu v dokumentu
                    score++;
                }
            }
            if (score >= maxScore) { //vyzobeme pouze dokumenty s maximalním score
                DataDocument dataDocument = new DataDocument(data, score);
                dataDocumentList.add(dataDocument);
                maxScore = score;
            }
        }
        Collections.sort(dataDocumentList, Collections.reverseOrder());
        dataRaf.close();
        return dataDocumentList;
    }

    private Map<Integer, IndexDocument> indexDocuments(RandomAccessFile hashRaf, int hashSpace, long hashSpacePosition) throws IOException {
        Map<Integer, IndexDocument> fileDocuments = new HashMap<Integer, IndexDocument>();
        for (int i=0; i<this.analyzed.length;i++) {
            byte[] word = this.analyzed[i];
            int hash = Utils.normalizeToHashSpace(Utils.maskSign(Utils.javaHash(word)), hashSpace); // plusové číslo
            hashRaf.seek(hashSpacePosition + (hash * Utils.HASH_SPACE_RECORD_SIZE));
            long docsOffset = hashRaf.readLong(); //pozice dokumentů ke slovu
            int docCount = hashRaf.readInt(); // počet dokumentů
            docCount = docCount>500?500:docCount; //todo prověřit: docCount > 500 utínám, výsledek hledání by to nemělo ovlivnit, zajímají nás víc specifická slova
            hashRaf.seek(docsOffset); //čísla dokumentů
            for (int docNum = 0; docNum < docCount; docNum++) {
                int indexDocNum = hashRaf.readInt(); //
                IndexDocument indexDocument = fileDocuments.get(indexDocNum);
                if (indexDocument == null) {
                    indexDocument = new IndexDocument(indexDocNum);
                    fileDocuments.put(indexDocNum, indexDocument);
                }
                indexDocument.addWordHash(hash);
            }
        }
        return fileDocuments;
    }

    public Map<Long, Integer> rawDocLocations(List<List<String>> seekedStrings, File seekedFile, int seekLimit, PrintStream output) throws IOException {
        Map<Long, Integer> allPositions = new HashMap<Long, Integer>();
        //todo introduce analyzer a datastore
        //todo store info about source file into index (hashFile) and take parameter indexFile instead seekedFile in the method
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
