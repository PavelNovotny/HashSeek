package com.o2.cz.cip.hashseek.o2seek;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by pavelnovotny on 15.05.17.
 */
public class SeekParamsDto {
    private String seekString;
    private String[] environment;
    private String[] fileKind;
    private String dateFrom;
    private String dateTo;

    public SeekParamsDto(JSONObject seekParam) {
        this.seekString = (String) seekParam.get("seekString");
        this.dateFrom = (String) seekParam.get("dateFrom");
        this.dateTo = (String) seekParam.get("dateTo");
        JSONArray environmentJSON = (JSONArray) seekParam.get("environment");
        this.environment = new String[environmentJSON.size()];
        int i=0;
        for (Object environment : environmentJSON) {
            this.environment[i++] = (String) environment;
        }
        JSONArray fileKindJSON = (JSONArray) seekParam.get("fileKind");
        this.fileKind = new String[fileKindJSON.size()];
        i=0;
        for (Object fileKind : fileKindJSON) {
            this.fileKind[i++] = (String) fileKind;
        }
    }

    public String getSeekString() {
        return seekString;
    }

    public void setSeekString(String seekString) {
        this.seekString = seekString;
    }

    public String[] getEnvironment() {
        return environment;
    }

    public void setEnvironment(String[] environment) {
        this.environment = environment;
    }

    public String[] getFileKind() {
        return fileKind;
    }

    public void setFileKind(String[] fileKind) {
        this.fileKind = fileKind;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }
}
