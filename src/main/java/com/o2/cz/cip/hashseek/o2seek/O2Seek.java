package com.o2.cz.cip.hashseek.o2seek;

import com.o2.cz.cip.hashseek.common.seek.Document;
import com.o2.cz.cip.hashseek.common.seek.SeekIndex;
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
        File indexFile = new File("/Users/pavelnovotny/Downloads/hashSeekZkusData/e2e/other_s4_alsb_aspect.audit.20170504.hash");
        //todo zjistit hash soubory (indexy) kterých se hledání týká
        SeekIndex seekIndex = new SeekIndex(indexFile, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer", 100);
        List<Document> documents = seekIndex.seek(seekParams.getSeekString());
        JSONObject obj=new JSONObject();
        JSONArray documentsJSON = new JSONArray();
        JSONArray analyzedJSON = new JSONArray();
        for (Document document : documents) {
            documentsJSON.add(document.getJSON());
        }
        for (byte[] word : seekIndex.getAnalyzed()) {
            analyzedJSON.add(new String(word));
        }
        obj.put("documents",documentsJSON);
        obj.put("analyzed", analyzedJSON);
        System.out.println(obj.toString());
        return obj;
    }
}
