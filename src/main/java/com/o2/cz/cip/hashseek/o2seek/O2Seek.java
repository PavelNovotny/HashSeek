package com.o2.cz.cip.hashseek.o2seek;

import com.o2.cz.cip.hashseek.common.seek.Document;
import com.o2.cz.cip.hashseek.common.seek.NotifyDocumentListener;
import com.o2.cz.cip.hashseek.common.seek.SeekIndex;
import com.o2.cz.cip.hashseek.common.seek.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by pavelnovotny on 15.05.17.
 */
public class O2Seek implements NotifyDocumentListener {

    public static ConfigurationDto conf;
    private NotifyJSONListener jsonListener;
    private int seekFilesCount;

    public O2Seek(NotifyJSONListener jsonListener) {
        this.jsonListener = jsonListener;
    }

    public void seek(SeekParamsDto seekParams) throws IOException, java.text.ParseException {
        Set<SeekFile> seekFiles = seekFiles(seekParams);
        seekFilesCount = seekFiles.size();
        for (SeekFile seekFile : seekFiles) {
            Thread thread = ThreadPool.getThread();
            SeekIndex seekIndex = ThreadPool.getSeekIndex(thread);
            seekIndex.setNotifyListener(this);
            seekIndex.setSeekParams(seekParams.getSeekString(), seekFile.indexFile, seekFile.dataFile, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer");
            synchronized (seekIndex) {
                seekIndex.notify();
            }
        }
    }

    private Set<SeekFile> seekFiles(final SeekParamsDto seekParams) throws java.text.ParseException {
        final Set<SeekFile> files = new HashSet<SeekFile>();
        final List<String> dates = seekDates(seekParams.getDateFrom(), seekParams.getDateTo());
        for (String environment : seekParams.getEnvironment()) {
            ConfigurationDto.Folder folder = conf.getFolder(environment);
            File[] acceptedHashFiles = new File[0];
            for (String indexFolder : folder.index) {
                File dir = new File(indexFolder);
                acceptedHashFiles = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        for (String date : dates) {
                            for (String fileKind : seekParams.getFileKind()) {
                                String pattern = conf.getIndexPattern(fileKind);
                                pattern = pattern.replaceAll("\\{date\\}", date);
                                return name.matches(pattern);
                            }
                        }
                        return false;
                    }
                });
            }
            for (String dataFolder : folder.data) {
                File dir = new File(dataFolder);
                final File[] finalAcceptedHashFiles = acceptedHashFiles;
                File[] acceptedFiles = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String hashName = name + ".hash";
                        for (File hashFile : finalAcceptedHashFiles) {
                            if (hashFile.getName().equals(hashName)) {
                                SeekFile seekFile = new SeekFile();
                                seekFile.dataFile = new File(dir, name);
                                seekFile.indexFile = hashFile;
                                files.add(seekFile);
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }
        }
        return files;
    }

    private List<String> seekDates(String from, String to) throws java.text.ParseException {
        long hours12overlap =12 * 60 * 60 * 1000; //vyrovná chybu letního času až do intervalu 12 let
        long hours24 =24 * 60 * 60 * 1000;
        List<String> seekDates = new ArrayList<String>();
        SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd");
        long dateFrom = fileDateFormat.parse(from).getTime();
        long dateTo = fileDateFormat.parse(to).getTime();
        long diff = dateTo - dateFrom;
        for (long day = dateFrom; day <= dateTo; day+=hours24) {
            String date = fileDateFormat.format(new Date(day));
            seekDates.add(date);
        }
        return seekDates;
    }

    @Override
    public synchronized void notifyResult(SeekIndex seekIndex) throws IOException {
        List<Document> documents = seekIndex.getResult();
        JSONObject result = new JSONObject();
        JSONArray documentsJSON = new JSONArray();
        JSONArray analyzedJSON = new JSONArray();
        for (byte[] word : seekIndex.getAnalyzed()) {
            analyzedJSON.add(new String(word));
        }
        result.put("analyzed", analyzedJSON);
        for (Document document : documents) {
            documentsJSON.add(document.getJSON());
        }
        result.put("documents",documentsJSON);
        result.put("dataFile",new String(seekIndex.getDataFile().getAbsolutePath()));
        this.jsonListener.notifyResult(result);
        ThreadPool.finishThread(seekIndex);
        if (--this.seekFilesCount <= 0) {
            this.jsonListener.resultsFinished();
        }
    }

    public class SeekFile {
        public File indexFile;
        public File dataFile;

        @Override
        public int hashCode() {
            return indexFile.getAbsolutePath().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o.hashCode() == hashCode();
        }
    }


}
