package com.o2.cz.cip.hashseek.util;

import java.io.*;
import java.nio.channels.FileChannel;

import com.o2.cz.cip.hashseek.io.RandomAccessFile;
import net.sf.samtools.util.BlockDeflateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by User on 28.2.14.
 */
public class CloseUtil {

    static final Logger LOGGER=LoggerFactory.getLogger(CloseUtil.class);

    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(InputStream stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }

    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(Reader stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }
    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(OutputStream stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }

    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(RandomAccessFile stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }

    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(java.io.RandomAccessFile stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }

    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(FileChannel stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }

    /**
     * Closes the resource. In case exception occurs, writes a warning to the log and returns false.
     *
     * @param stream The resource to be closed
     *
     * @return boolean true if successfully closed, false otherwise
     */
    public static boolean close(FileWriter stream) {
        if (null == stream) {
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            logFailedClose(stream, e);
            return false;

        }
    }

    private static void logFailedClose(Object toClose, Exception e) {
        LOGGER.warn("Failed to call close method on object of class \"" + toClose.getClass().getName() + "\"." + e.getMessage());
    }

}