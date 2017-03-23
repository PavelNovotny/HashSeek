package com.o2.cz.cip.hashseek.blockseek.blocktimelog;


import com.o2.cz.cip.hashseek.blockseek.AbstractBlockRecord;
import com.o2.cz.cip.hashseek.blockseek.AbstractSeekResults;
import com.o2.cz.cip.hashseek.blockseek.BlockRecordFactory;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditBlockLogRecord;
import com.o2.cz.cip.hashseek.blockseek.blockauditlog.AuditBlockLogRecordFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pavelnovotny on 12.03.14.
 */
public class TimeSeekResults extends AbstractSeekResults {

    public TimeSeekResults(List<File> seekedFiles) {
        super(seekedFiles);
    }

    @Override
    public BlockRecordFactory createBlockRecordFactory() {
        return new TimeBlockLogRecordFactory();
    }

    @Override
    public List<List<String>> decorateSeekedStrings(List<List<String>> seekedStrings) {
        //todo pn - projet parenta a zjistit beaId a přidat do listu
        List<AbstractBlockRecord> parentBlockList = getParentSeekResults().getBlockList();
        for (AbstractBlockRecord abstractParentBlock : parentBlockList) {
            if (abstractParentBlock instanceof AuditBlockLogRecord) {
                AuditBlockLogRecord auditParentBlock = (AuditBlockLogRecord) abstractParentBlock;
                String beaId = auditParentBlock.getBeaId();
                if (beaId != null) {
                    List<String> andBeaIds = new LinkedList<String>();
                    andBeaIds.add(beaId);
                    seekedStrings.add(andBeaIds);
                }
            }
        }
        return seekedStrings;
    }


}
