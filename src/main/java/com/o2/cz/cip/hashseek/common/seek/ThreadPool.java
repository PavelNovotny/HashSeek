package com.o2.cz.cip.hashseek.common.seek;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by pavelnovotny on 25.05.17.
 */
public class ThreadPool {

    private static Set<Thread> freeThreads;
    private static Set<Thread> busyThreads;
    private static Map<Thread, SeekIndex> allThreads;
    private static Map<SeekIndex, Thread> allSeekIndex;
    private static int threadCount = 100;

    static {
        ThreadPool.freeThreads = new HashSet<Thread>();
        ThreadPool.allThreads = new HashMap<Thread, SeekIndex>();
        ThreadPool.allSeekIndex = new HashMap<SeekIndex, Thread>();
        ThreadPool.busyThreads = new HashSet<Thread>();
        for (int i=0;i<threadCount;i++) {
            SeekIndex seekIndex = new SeekIndex();
            Thread thread = new Thread(seekIndex);
            freeThreads.add(thread);
            allThreads.put(thread, seekIndex);
            allSeekIndex.put(seekIndex, thread);
            thread.start();
        }
    }

    public static synchronized Thread getThread() {
        if (freeThreads.size() > 0) {
            Thread thread = freeThreads.iterator().next();
            freeThreads.remove(thread);
            busyThreads.add(thread);
            return thread;
        }
        //todo waiting list, queue
        return null;
    }

    public static synchronized SeekIndex getSeekIndex(Thread thread) {
        return allThreads.get(thread);
    }

    public static synchronized void finishThread(SeekIndex seekIndex) {
        Thread thread = allSeekIndex.get(seekIndex);
        busyThreads.remove(thread);
        freeThreads.add(thread);
    }


}
