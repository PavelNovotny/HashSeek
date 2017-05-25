package com.o2.cz.cip.hashseek.o2seek;

import com.o2.cz.cip.hashseek.common.seek.Document;
import com.o2.cz.cip.hashseek.common.seek.SeekIndex;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by pavelnovotny on 15.05.17.
 */
public class O2Seek {

    public static ConfigurationDto conf;

    public JSONObject seek(SeekParamsDto seekParams) throws IOException, java.text.ParseException {
        Set<SeekFile> seekFiles = seekFiles(seekParams);
        JSONObject obj=new JSONObject();
        JSONArray results = new JSONArray();
        obj.put("results",results);
        for (SeekFile seekFile : seekFiles) {
            //todo thread pool, každé hledání v souboru (SeekIndex) v samostatném vlákně. Jinak čekat, až se uvolní
            SeekIndex seekIndex = new SeekIndex(seekFile.indexFile, seekFile.dataFile, "PlainFileExtractData", "DefaultOldHashSeekAnalyzer");
            List<Document> documents = seekIndex.seek(seekParams.getSeekString());
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
            results.add(result);
        }
        System.out.println(obj.toString());
        return obj;
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
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        for (long day = dateFrom; day <= dateTo; day+=hours24) {
            String date = fileDateFormat.format(new Date(day));
            seekDates.add(date);
        }
        return seekDates;
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
