package com.o2.cz.cip.hashseek.logs.evaluate;

import com.o2.cz.cip.hashseek.app.AppArguments;
import com.o2.cz.cip.hashseek.logs.AbstractLogSeek;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pavelnovotny on 13.03.14.
 */
public class HashVersionEvaluator implements FileEvaluator { //File evaluator pro určení verze. Verze se určí na základě hash audit logů. Ostatní logy pro stejné datumy by měly být hashovány ve stejné verzi, jinak je něco hodně špatně.


    public int hashVersion(AppArguments appArguments) throws IOException{
        Set<File> files = FileEvaluatorUtil.filesToSeek(appArguments, this);
        int version = 0, prevVersion =-1;
        if (files.isEmpty()) {
            return 0;
        } else {
            for (File hashFile : files) {
                Pattern pattern = Pattern.compile("^.*(hash_v(\\d)).*$");
                Matcher matcher = pattern.matcher(hashFile.getName());
                if (matcher.matches()) {
                    version = Integer.parseInt(matcher.group(2));
                    if (version != prevVersion && prevVersion !=-1) {
                        return -1; //version mismatch během periody
                    } else {
                        prevVersion = version;
                    }
                }
            }
        }
        return version;
    }

    @Override
    public boolean canBeProcessed(String name, AppArguments appArguments) {
        Pattern pattern = Pattern.compile("^(other|jms)_s._alsb_aspect.audit\\.(\\d{8})(\\.\\d{2})?(.bgz)?(.hash)(_v(\\d))(.bgz)?$");
        Matcher matcher = pattern.matcher(name);
        if (appArguments.getDatesToScan().isEmpty()) {
            return false;
        } else if (matcher.matches()) {
            return (appArguments.getDatesToScan().contains(matcher.group(2)));
        } else {
            return false;
        }
    }

    @Override
    public void addToMissedFiles(Set<String> missedFiles, AppArguments appArguments, String dateHour) {
        missedFiles.add(String.format("other_s1_alsb_aspect.audit.%s", dateHour));
        missedFiles.add(String.format("other_s2_alsb_aspect.audit.%s", dateHour));
        missedFiles.add(String.format("jms_s1_alsb_aspect.audit.%s", dateHour));
        missedFiles.add(String.format("jms_s2_alsb_aspect.audit.%s", dateHour));
        if (appArguments.isSeekProd()) {
            missedFiles.add(String.format("other_s3_alsb_aspect.audit.%s", dateHour));
            missedFiles.add(String.format("other_s4_alsb_aspect.audit.%s", dateHour));
            missedFiles.add(String.format("jms_s3_alsb_aspect.audit.%s", dateHour));
            missedFiles.add(String.format("jms_s4_alsb_aspect.audit.%s", dateHour));
        }
    }

    @Override
    public String[] getServers(AppArguments appArguments) {
        if (appArguments.isSeekProd()) {
            return AbstractLogSeek.PROD_ESB_Servers;
        } else {
            if(appArguments.isSeekPredprod()){
                return AbstractLogSeek.PREDPROD_ESB_Servers;
            } else {
                return AbstractLogSeek.TEST_ESB_Servers;
            }
        }
    }

    @Override
    public String[] getDomains(AppArguments appArguments) {
        return AbstractLogSeek.ESB_Domains;
    }
}
