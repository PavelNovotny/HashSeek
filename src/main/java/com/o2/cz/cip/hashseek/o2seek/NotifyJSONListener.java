package com.o2.cz.cip.hashseek.o2seek;

import com.o2.cz.cip.hashseek.common.seek.SeekIndex;
import org.json.simple.JSONObject;

import java.io.IOException;

/**
 * Created by pavelnovotny on 25.05.17.
 */
public interface NotifyJSONListener {
    public void notifyResult(JSONObject jsonObject);
    public void resultsFinished();
}
