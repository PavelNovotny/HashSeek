package com.o2.cz.cip.hashseek.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pavelnovotny on 25.05.15.
 */
public class CsvParams {
    private String service;
    private List<String> xPath = new ArrayList<String>();
    private boolean isRequest; //response otherwise;

    public CsvParams() {
        for (int i=0;i<2;i++) {
            xPath.add("");
        }
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getxPath() {
        return xPath;
    }

    public void setXPath(int index, String xPath) {
        this.xPath.set(index, xPath);
    }

    public boolean isRequest() {
        return isRequest;
    }

    public void setRequest(boolean isRequest) {
        this.isRequest = isRequest;
    }


}
