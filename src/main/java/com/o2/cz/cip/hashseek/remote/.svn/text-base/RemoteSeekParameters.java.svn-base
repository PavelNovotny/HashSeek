package com.o2.cz.cip.hashseek.remote;

import com.o2.cz.cip.hashseek.core.HashSeekConstants;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * User: Pavel
 * Date: 14.5.13 14:43
 */
public class RemoteSeekParameters implements Serializable {
    private Set<String> stringsToSeek;
    private long currentTime = System.currentTimeMillis();
    private String clientFileToSeek; //soubor na filesystemu klienta. Nutne prelozit na soubor na filesystemu listeneru.

    public Set<String> getStringsToSeek() {
        return stringsToSeek;
    }

    public void setStringsToSeek(Set<String> stringsToSeek) {
        this.stringsToSeek = stringsToSeek;
    }

    public void setStringsToSeek(String[] stringsToSeek) {
        this.stringsToSeek = new HashSet<String>(Arrays.asList(stringsToSeek));
    }

    public static void main(String[] args) {
        RemoteSeekParameters remoteSeekParameters = new RemoteSeekParameters();
    }

    public String getClientFileToSeek() {
        return clientFileToSeek;
    }

    public void setClientFileToSeek(String clientFileToSeek) {
        this.clientFileToSeek = clientFileToSeek;
    }

    public void reportParams() {
        HashSeekConstants.outPrintLine(String.format("remote message, seek params, list of stringsToSeek:" ));
        if (stringsToSeek != null) {
            for (String stringToSeek : stringsToSeek) {
                HashSeekConstants.outPrintLine(String.format("remote message, seek params, stringToSeek: '%s'", stringToSeek));
            }
        } else {
            HashSeekConstants.outPrintLine(String.format("remote message, seek params, stringsToSeek: null"));
        }
        HashSeekConstants.outPrintLine(String.format("remote message, seek params, clientFileToSeek: '%s'", clientFileToSeek));
    }

}
