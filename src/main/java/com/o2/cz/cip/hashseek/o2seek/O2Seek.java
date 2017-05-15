package com.o2.cz.cip.hashseek.o2seek;

import com.o2.cz.cip.hashseek.common.analyze.Analyzer;
import com.o2.cz.cip.hashseek.common.analyze.AnalyzerFactory;
import com.o2.cz.cip.hashseek.common.datastore.ExtractData;
import com.o2.cz.cip.hashseek.common.datastore.ExtractDataFactory;
import com.o2.cz.cip.hashseek.common.seek.DataDocument;
import com.o2.cz.cip.hashseek.common.seek.SeekIndexFile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by pavelnovotny on 15.05.17.
 */
public class O2Seek {

    public JSONObject seek(SeekParamsDto seekParams) throws IOException {
        File indexFile = new File("/Users/pavelnovotny/Downloads/transfer/e2e/jms_s1_alsb_aspect.audit.20170209.19.hash");
        //todo zjistit hash soubory (indexy) kterých se hledání týká
        SeekIndexFile seekIndexFile = new SeekIndexFile(indexFile, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer", 100);
        //SeekIndexFile seekIndexFile = new SeekIndexFile(indexFile, extractData, analyzer, 100);
        List<DataDocument> dataDocuments = seekIndexFile.seek(seekParams.getSeekString());
        JSONObject obj=new JSONObject();
        JSONArray documentsJSON = new JSONArray();
        JSONArray analyzedJSON = new JSONArray();
        for (DataDocument document : dataDocuments) {
            documentsJSON.add(document.getJSON());
        }
        for (byte[] word : seekIndexFile.getAnalyzed()) {
            analyzedJSON.add(new String(word));
        }
        obj.put("documents",documentsJSON);
        obj.put("analyzed", analyzedJSON);
        System.out.println(obj.toString());
        return obj;
    }
}
