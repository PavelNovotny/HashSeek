package com.o2.cz.cip.hashseek.common.seek;

import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;

/**
 * Created by pavelnovotny on 11.05.17.
 */
public class Document {

    public byte[] data;
    public int indexScore;
    public int dataScore = 0;
    public int docNum;
    public long dataOffset;
    public int docLen;

    public static Comparator<Document> indexScoreComparator() {
        return new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                return o2.indexScore - o1.indexScore; //reverse order
            }
        };
    }

    public static Comparator<Document> dataScoreComparator() {
        return new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                return o2.dataScore - o1.dataScore; //reverse order
            }
        };
    }

    public JSONObject getJSON () throws UnsupportedEncodingException {
        JSONObject obj = new JSONObject();
        obj.put("score", this.dataScore);
        obj.put("document", new String(data, "UTF-8"));
        return obj;
    }

}
