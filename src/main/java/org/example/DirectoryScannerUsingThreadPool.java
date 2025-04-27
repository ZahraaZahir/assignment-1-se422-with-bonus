package org.example;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class DirectoryScannerUsingThreadPool implements DirectoryScanner {
    private final String directoryPath;
    private final List<PdfCountListener> listeners = new ArrayList<>();
    private final ReentrantLock countLock = new ReentrantLock();
    private int pdfCount = 0;

    public DirectoryScannerUsingThreadPool(String path) {
        this.directoryPath = path;
    }

    @Override
    public void addListener(PdfCountListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void countPdfs() {
        BlockingQueue<File> queue = new LinkedBlockingQueue<>();
        try {
            File root = new File(directoryPath);
            if (!root.exists() || !root.isDirectory()) {
                System.err.println("Invalid directory for ThreadPool: " + directoryPath);
                return;
            }

            File[] initial = root.listFiles();
            if (initial != null) {
                for (File f : initial) queue.offer(f);
            }

            int poolSize = ThreadPoolProvider.getInstance().getThreadCount();
            CountDownLatch latch = new CountDownLatch(poolSize);
            for (int i = 0; i < poolSize; i++) {
                ThreadPoolWorker w = new ThreadPoolWorker(queue, this, latch);
                ThreadPoolProvider.getInstance().submitTask(w);
            }
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("Thread-pool scan interrupted.");
        } catch (Exception e) {
            System.err.println("Error in ThreadPool countPdfs: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ThreadPoolProvider.getInstance().shutdown();
        }
    }

    @Override
    public void incrementAndNotify(String threadName) {
        int currentCount;
        countLock.lock();
        try {
            pdfCount++;
            currentCount = pdfCount;
            synchronized (listeners) {
                for (PdfCountListener listener : listeners) {
                    try {
                        listener.onPdfCountChanged("ThreadPool", threadName, currentCount);
                    } catch (Exception e) {
                        System.err.println("Listener error: " + e.getMessage());
                    }
                }
            }
        } finally {
            countLock.unlock();
        }
    }

    @Override
    public int getPdfCount() {
        countLock.lock();
        try {
            return pdfCount;
        } finally {
            countLock.unlock();
        }
    }
}
