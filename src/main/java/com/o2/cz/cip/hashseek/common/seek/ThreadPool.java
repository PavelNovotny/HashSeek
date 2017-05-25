package com.o2.cz.cip.hashseek.common.seek;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pavelnovotny on 25.05.17.
 */
public class ThreadPool {

    private Set<Thread> freeThreads;
    private Set<Thread> busyThreads;

    public ThreadPool() {
        this.freeThreads = new HashSet<Thread>();
        this.busyThreads = new HashSet<Thread>();
    }

    public synchronized Thread getThread() {
        if (freeThreads.size() > 0) {
            return freeThreads.iterator().next();
        }
        //todo waiting list
        return null;
    }

    public synchronized void finishThread(Thread thread) {
        busyThreads.remove(thread);
        freeThreads.add(thread);
    }


}
