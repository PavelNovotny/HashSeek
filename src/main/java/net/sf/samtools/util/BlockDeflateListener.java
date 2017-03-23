package net.sf.samtools.util;

import java.io.IOException;

/**
 * Created by User on 13.1.14.
 */
public interface BlockDeflateListener {

    public void onDeflateBlock(long mBlockAddress, int totalBlockSize, int bytesToCompress, long blockCounter, long filePointer);
    public void onClose();
}
