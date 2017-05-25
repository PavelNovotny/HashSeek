package com.o2.cz.cip.hashseek.common.seek;

import com.o2.cz.cip.hashseek.common.analyze.Analyzer;
import com.o2.cz.cip.hashseek.common.analyze.AnalyzerFactory;
import com.o2.cz.cip.hashseek.common.datastore.ExtractData;
import com.o2.cz.cip.hashseek.common.datastore.ExtractDataFactory;
import com.o2.cz.cip.hashseek.common.io.RandomAccessFile;
import com.o2.cz.cip.hashseek.common.util.Utils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class SeekIndex {
    private static Logger LOGGER = Logger.getLogger(SeekIndex.class);
    private File dataFile;
    private File indexFile;
    private ExtractData extractData;
    private Analyzer analyzer;
    private static final int REZERVA_SCORE = 3;

    private byte[][] analyzed;

    public SeekIndex(File indexFile, File dataFile, String dataStoreKind, String analyzerKind) throws FileNotFoundException {
        this.indexFile = indexFile;
        //todo lepší zjištění datového souboru (z indexu?)
        this.dataFile = dataFile;
        //todo možná vysunout a vytvářet instance mimo tuto třídu
        this.extractData = ExtractDataFactory.createInstance(dataStoreKind);
        this.extractData.setDataFile(this.dataFile);
        this.analyzer = AnalyzerFactory.createInstance(analyzerKind);
    }

    private  byte[][] removeDuplicities(byte[][] analyzed) throws UnsupportedEncodingException {
        Set<String> analyzedSet = new HashSet<String>();
        for (byte[] word : analyzed) {
            analyzedSet.add(new String(word));
        }
        byte[][] noDuplicity = new byte[analyzedSet.size()][];
        int i=0;
        for (String analyzedString : analyzedSet ) {
            byte[] word = analyzedString.getBytes("UTF-8");
            noDuplicity[i++] = word;
        }
        return noDuplicity;
    }

    public List<Document> seek(String seekString) throws IOException {
        RandomAccessFile hashRaf = new RandomAccessFile(indexFile,"r");
        hashRaf.readInt(); //version
        long hashSpacePosition = hashRaf.readLong();
        int hashSpace = hashRaf.readInt();
        //todo remove in future index versions
        int blockKind = hashRaf.readInt(); //custom block or fixedBlocks
        int fixedBlockSize = hashRaf.readInt(); //in case fixedSize number of bytes per block, in case of customBlock is not needed
        long indexDocOffset = hashRaf.getFilePointer();
        this.analyzed = removeDuplicities(analyzer.analyze(seekString.getBytes("UTF-8")));
        Word[] words = findWords(hashRaf, hashSpace, hashSpacePosition);
        Document[] indexDocuments = findDocuments(words, indexDocOffset, hashRaf);
        RandomAccessFile dataRaf = new RandomAccessFile(dataFile,"r");
        List<Document> dataDocuments = verifyDocuments(indexDocuments, dataRaf);
        hashRaf.close();
        dataRaf.close();
        return dataDocuments;
    }


    private Word[] findWords(RandomAccessFile hashRaf, int hashSpace, long hashSpacePosition) throws IOException {
        Word[] words = new Word[analyzed.length];
        for (int i=0; i<this.analyzed.length;i++) {
            byte[] word = this.analyzed[i];
            int hash = Utils.normalizeToHashSpace(Utils.maskSign(Utils.javaHash(word)), hashSpace); // plusové číslo
            hashRaf.seek(hashSpacePosition + (hash * Utils.HASH_SPACE_RECORD_SIZE));
            long docsOffset = hashRaf.readLong(); //pozice dokumentů ke slovu
            int docCount = hashRaf.readInt(); // počet dokumentů
            Word seekWord = new Word();
            seekWord.docsOffset = docsOffset;
            seekWord.docCount = docCount;
            words[i] = seekWord;
        }
        return words;
    }

    private List<Document> verifyDocuments(Document[] documents, RandomAccessFile dataRaf) throws IOException {
        for (int i = 0;i<documents.length && i< 100; i++) {
            dataRaf.seek(documents[i].dataOffset);
            documents[i].data = dataRaf.readRawBytes(documents[i].docLen);
            for (byte[] word : analyzed) {
                if (Utils.indexOf(documents[i].data, word) > -1) { //počítáme reálné score
                    documents[i].dataScore++;
                }
            }
        }
        Arrays.sort(documents, Document.dataScoreComparator()); //dokumenty s největším reálným skore na začátku.
        int maxScore;
        List<Document> returnDoc = new ArrayList<Document>();
        if (documents.length > 0) {
            maxScore = documents[0].dataScore;
            for (Document document : documents) {
                if (document.dataScore == maxScore) { // filtrujeme pouze dokumenty s maximálním score
                    returnDoc.add(document);
                }
            }
        }
        return returnDoc;
    }

    private Document[] findDocuments(Word[] words, long docOffset, RandomAccessFile hashRaf) throws IOException {
        Arrays.sort(words, Word.sizeComparator()); //začínáme od nejmenšího počtu výskytů v indexu, což jsou pravděpodobně ty které nás zajímají, ostatní je balast s velkým množstvím výskytů
        Map<Integer, Integer> docToScore = new HashMap<Integer, Integer>();
        Map<Integer, Integer> scoreCounts = new HashMap<Integer, Integer>();
        int bestScore = 0;
        outer:
        for (int i=0;i< words.length;i++) {
            Word word = words[i];
            hashRaf.seek(word.docsOffset); //pozice na čísla dokumentů
            for (int j=0;j< word.docCount;j++) {
                int docNumber = hashRaf.readInt(); //konkretni cislo dokumentu
                Integer scoreValue = docToScore.get(docNumber);
                if (scoreValue == null) {
                    scoreValue = 0;
                }
                scoreValue++;
                if (scoreValue > bestScore - REZERVA_SCORE) { //dokument s best score nemusí mít na datech ověřené nejlepší score, proto si necháváme i dokumenty s horším score v indexu
                    if (scoreValue > bestScore) {
                        bestScore = scoreValue;
                    }
                    docToScore.put(docNumber, scoreValue);
                    Integer scoreCount = scoreCounts.get(scoreValue);
                    if (scoreCount == null) {
                        scoreCount = 0;
                    }
                    if (scoreCount>1000) { //ty pravděpodobnější dokumenty tam už budou, a pokud by se mělo jednat o to nejlepší skore, tak 1000 je až až.
                        break;
                    }
                    scoreCounts.put(scoreValue, scoreCount+1);
                }
                if (bestScore > words.length - i - REZERVA_SCORE) { //žádné nové slovo nemůže dosáhnout na best score, tj. např. 60 hledaných slov, nejlepší skore je 20 a jsme na 43 slově, tj. všechny další slova už budou mít to skŕore menší s rezervou 3
                    break outer;
                }
                if (word.docCount > 10000 && docToScore.size() > 0 ) { //nějaké (pravděpodobnější) výsledky už máme a hledat průnik s takto častým slovem prolézáním všech takto častých dokumentů je časově a paměťově náročné, pokud tam to slovo s velkým výskytem bude, odhalí se ve finálním scoringu.
                    break outer;
                }
                if (word.docCount > 50000 && j > 200 ) { //příliš mnoho výskytů, skončíme rychle, nějaký vzorek máme
                    break outer;
                }
            }
        }
        int i = 0;
        Document[] documents = new Document[docToScore.size()];
        for (int docNum : docToScore.keySet()) {
            int docScore = docToScore.get(docNum);
            Document document = new Document();
            document.indexScore = docScore;
            document.docNum = docNum;
            hashRaf.seek(docOffset + (docNum * Utils.LONG_SIZE)); //zjištění pozice dokumentu v hledaném souboru
            long dataDocOffset = hashRaf.readLong();
            long nextDocOffset = hashRaf.readLong();
            int docLen = (int)(nextDocOffset - dataDocOffset); //bez přesahu, předpokládáme, že custom blok je inteligentně udělaný
            document.dataOffset = dataDocOffset;
            document.docLen = docLen;
            documents[i++] = document;
        }
        Arrays.sort(documents, Document.indexScoreComparator()); //dokumenty s největším index score na začátku
        return documents;
    }

    public byte[][] getAnalyzed() {
        return analyzed;
    }


}
