package com.o2.cz.cip.hashseek.blockseek.blocktimelog;

import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.SortInfo;
import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public class TimeBlockLogRecord extends AbstractBlockRecord {

    public TimeBlockLogRecord(String rawData, File logFile, long filePosition) {
        super(rawData, logFile, filePosition);
    }

    @Override
    public String format() {
        return "";
    }

    @Override
    public String timeStamp() {
        String rawData = getRawData();
        return rawData.substring(1,18);
    }

    @Override
    public String headerData() {
        StringBuilder headerData = new StringBuilder();
        headerData.append("TIME:  ").append(logFile);
        headerData.append("\n").append(getRawData());
        return headerData.toString();
    }

    @Override
    public String sortKey() { //sortuje se po blokách, uvnitř bloku je už sesortováno tudíž můžeme vynechat beaId apod..
        if (SortInfo.SortBy.ENTRYTIME.equals(sortInfo.getSortBy())) {
            return String.format("%s#%s#%020d", getTimeStamp(), normalizedFileName(), filePosition);
        } else { //default sort, no sort
            return "";
        }
    }

}
