package com.o2.cz.cip.hashseek.blockseek.blockbpmlog;


import com.o2.cz.cip.hashseek.blockseek.AbstractSeekResults;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;
import com.o2.cz.cip.hashseek.blockseek.blocktimelog.TimeBlockLogRecordFactory;

import java.io.File;
import java.util.List;

/**
 * Created by pavelnovotny on 12.03.14.
 */
public class BpmSeekResults extends AbstractSeekResults {

    public BpmSeekResults(List<File> seekedFiles) {
        super(seekedFiles);
    }

    @Override
    public BlockRecordFactory createBlockRecordFactory() {
        return new BpmBlockLogRecordFactory();
    }

    @Override
    public List<List<String>> decorateSeekedStrings(List<List<String>> seekedStrings) {
        //todo pn - projet výsledky parenta a zjistit kde se volá bpm a přidat klíče do listu.
        return seekedStrings;
    }

}
