package com.o2.cz.cip.hashseek.common.seek;

import java.io.IOException;
import java.util.List;

/**
 * Created by pavelnovotny on 25.05.17.
 */
public interface NotifyDocumentListener {
    public void notifyResult(SeekIndex seekIndex) throws IOException;
}
