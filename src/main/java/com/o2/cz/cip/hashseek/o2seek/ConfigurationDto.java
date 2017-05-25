package com.o2.cz.cip.hashseek.o2seek;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by pavelnovotny on 23.05.17.
 */
public class ConfigurationDto {
    private JSONObject indexPattern;
    private Map<String, Folder> folders;

    public ConfigurationDto(JSONObject conf) {
        this.indexPattern = (JSONObject) conf.get("indexPatterns");
        readFolders(conf);
    }

    public String getIndexPattern(String key) {
        return (String) this.indexPattern.get(key);
    }

    public Folder getFolder(String key) {
        return folders.get(key);
    }

    private void readFolders(JSONObject conf) {
        JSONObject jsonFolders = (JSONObject) conf.get("folders");
        this.folders = new HashMap<String, Folder>();
        for(Iterator iterator = jsonFolders.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            JSONObject jsonFolder = (JSONObject) jsonFolders.get(key);
            JSONArray data = (JSONArray) jsonFolder.get("data");
            String[] folderData = new String[data.size()];
            JSONArray index = (JSONArray) jsonFolder.get("index");
            String[] folderIndex = new String[index.size()];
            Folder folder = new Folder();
            for (int i=0; i<folderData.length; i++) {
                folderData[i] = (String) data.get(i);
            }
            for (int i=0; i<folderIndex.length; i++) {
                folderIndex[i] = (String) index.get(i);
            }
            folder.data = folderData;
            folder.index = folderIndex;
            this.folders.put(key, folder);
        }
    }

    public class Folder {
        public String[] data;
        public String[] index;
    }

}

