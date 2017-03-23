package com.o2.cz.cip.hashseek.logs.evaluate;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pavelnovotny on 13.03.14.
 */
public class NoeFileEvaluator implements FileEvaluator {

    public List<File> filesToSeek(AppArguments appArguments) throws IOException{
        Set<File> files = FileEvaluatorUtil.filesToSeek(appArguments, this);
        List<File> filesToSeek = new LinkedList<File>();
        for (File file : files) {
            filesToSeek.add(file);
        }
        return filesToSeek;
    }

    @Override
    public boolean canBeProcessed(String name, AppArguments appArguments) {
        Pattern pattern = Pattern.compile("^noe_s..log\\.(\\d{8}|part)(\\.\\d{2})?(.bgz)?$");
        Matcher matcher = pattern.matcher(name);
        if (appArguments.getDatesToScan().isEmpty()) {
            return false;
        } else if (matcher.matches()) {
            return (appArguments.getDatesToScan().contains(matcher.group(1)));
        } else {
            return false;
        }
    }

    @Override
    public void addToMissedFiles(Set<String> missedFiles, AppArguments appArguments, String dateHour) {
    }

    @Override
    public String[] getServers(AppArguments appArguments) {
        if (appArguments.isSeekProd()) {
            return AbstractLogSeek.PROD_NOE_Servers;
        } else {
            if(appArguments.isSeekPredprod()){
                return AbstractLogSeek.PREDPROD_NOE_Servers;
            } else {
                return AbstractLogSeek.TEST_NOE_Servers;
            }
        }
    }

    @Override
    public String[] getDomains(AppArguments appArguments) {
        return AbstractLogSeek.ESB_Domains;
    }
}
