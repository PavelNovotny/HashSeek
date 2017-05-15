package com.o2.cz.cip.hashseek.common.seek;

import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by pavelnovotny on 11.05.17.
 */
public class DataDocument implements Comparable {

    private byte[] document;
    private int score;

    public DataDocument(byte[] document, int score) {
        this.document = document;
        this.score = score;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof DataDocument) {
            return this.score > ((DataDocument) o).getScore()?1:0;
        }
        return 0;
    }

    public int getScore() {
        return score;
    }

    public byte[] getDocument() {
        return document;
    }

    public JSONObject getJSON () throws UnsupportedEncodingException {
        JSONObject obj = new JSONObject();
        obj.put("score", this.score);
        obj.put("document", new String(document, "UTF-8"));
        return obj;
    }

}
