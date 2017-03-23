package com.o2.cz.cip.hashseek.blockseek.blockauditlog;


import com.o2.cz.cip.hashseek.blockseek.AbstractSeekResults;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;

import java.io.File;
import java.util.List;

/**
 * Created by pavelnovotny on 12.03.14.
 */
public class AuditSeekResults extends AbstractSeekResults {

    public AuditSeekResults(List<File> seekedFiles) {
        super(seekedFiles);
    }

    @Override
    public BlockRecordFactory createBlockRecordFactory() {
        return new AuditBlockLogRecordFactory();
    }

    @Override
    public List<List<String>> decorateSeekedStrings(List<List<String>> seekedStrings) {
        return seekedStrings;
    }


}
