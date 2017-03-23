package com.o2.cz.cip.hashseek.logs.evaluate;

import com.o2.cz.cip.hashseek.app.AppArguments;

import java.util.Set;

/**
 * Created by User on 27.2.14.
 */
public interface FileEvaluator {

    boolean canBeProcessed(String name, AppArguments appArguments);

    void addToMissedFiles(Set<String> missedFiles, AppArguments appArguments, String dateHour);

    String [] getServers(AppArguments appArguments);

    String [] getDomains(AppArguments appArguments);
}
