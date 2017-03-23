package com.o2.cz.cip.hashseek.blockseek;

import java.io.File;

/**
 * Created by pavelnovotny on 07.03.14.
 */
public abstract class AbstractBlockRecord implements Comparable<AbstractBlockRecord> {

    //custom block by měl být natolik inteligentně udělaný, že nebude potřeba jej sortit uvnitř. Bude ale potřeba ho zformátovat.
    //fixed block bude potřebovat speciální handling
    //bude potřeba ho zmergovat s ostatními bloky.

    protected SortInfo sortInfo; //stejná instance se nasetuje v rámci celé kolekce BlockRecord v SeekResultContaineru které se pak tím pádem budou sortovat podle stejného klíče. Pro změnu sortování, pak stačí jenom přepnout sortBy u této třídy a ovlivní to všechny členy kolekce. Vyhneme se tím nebezpečí statické proměnné, což by byl problém pokud by se hledalo ve více vláknech najednou.
    protected String rawData;
    protected String formattedData;
    protected String timeStamp;
    protected String headerData;
    protected File logFile;
    protected long filePosition;
    protected String normalizedFileName; //pro účely sortování, znormalizované jméno souboru na nějaký počet znaků.

    public abstract String format(); //poděděné třídy implementují metodu na zformátování rawData (včetně markerů)
    public abstract String timeStamp(); //poděděné třídy implementují metodu na získání timestampu (asi z rawData)
    public abstract String headerData(); //poděděné třídy implementují metodu na vytvoření headeru (včetně markerů)
    public abstract String sortKey(); //poděděné třídy implementují metodu na vrácení klíče pro sortování (předpokládá se, že vezmou v potaz sortInfo)

    public AbstractBlockRecord(String rawData, File logFile, long filePosition) {
        this.rawData = rawData;
        this.logFile = logFile;
        this.filePosition = filePosition;
    }

    public String getFormattedData() {
        if (formattedData == null) {
            this.formattedData = format();
        }
        return formattedData;
    }

    public String getTimeStamp() {
        if (timeStamp == null) {
            this.timeStamp = timeStamp();
        }
        return timeStamp;
    }

    public String getHeaderData() {
        if (headerData == null) {
            this.headerData = headerData();
        }
        return headerData;
    }

    public String getRawData() {
        return rawData;
    }

    protected String normalizedFileName() {
        if (this.normalizedFileName == null) {
           this.normalizedFileName = String.format("%100s", logFile.getName());
        }
        return normalizedFileName;
    }

    @Override
    public int compareTo(AbstractBlockRecord blockRecord) {
        if (SortInfo.SortDirection.ASC.equals(sortInfo.getSortDirection())) {
            return sortKey().compareTo(blockRecord.sortKey());
        } else {
            return blockRecord.sortKey().compareTo(sortKey());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AbstractBlockRecord) {
            AbstractBlockRecord blockRecord = (AbstractBlockRecord) o;
            return logFile.equals(blockRecord.logFile) && filePosition == blockRecord.filePosition;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return logFile.hashCode() ^ (int)filePosition;
    }

    public void setSortInfo(SortInfo sortInfo) {
        this.sortInfo = sortInfo;
    }
}
