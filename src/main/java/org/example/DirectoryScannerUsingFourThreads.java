package org.example;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class DirectoryScannerUsingFourThreads implements DirectoryScanner {
    private static final int THREAD_COUNT = 4;
    private final String directoryPath;
    private final List<PdfCountListener> listeners = new ArrayList<>();
    private final ReentrantLock countLock = new ReentrantLock();
    private int pdfCount = 0;

    public DirectoryScannerUsingFourThreads(String directoryPath) {
        this.directoryPath = directoryPath;
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
                System.err.println("Invalid directory for FourThreads: " + directoryPath);
                return;
            }
            File[] initial = root.listFiles();
            if (initial != null) {
                for (File f : initial) queue.offer(f);
            }

            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            for (int i = 0; i < THREAD_COUNT; i++) {
                new Thread(new FourThreadWorker(queue, this, latch),
                        "WorkerThread-" + i)
                        .start();
            }
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("FourThreads scan interrupted.");
        } catch (Exception e) {
            System.err.println("Error in FourThreads countPdfs: " + e.getMessage());
            e.printStackTrace();
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
                        listener.onPdfCountChanged("FourThreads", threadName, currentCount);
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

