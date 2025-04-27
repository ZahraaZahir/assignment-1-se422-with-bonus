package org.example;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.*;

public class DirectoryScannerUsingSingleThread implements DirectoryScanner {
    private final String directoryPath;
    private final List<PdfCountListener> listeners = new ArrayList<>();
    private final ReentrantLock countLock = new ReentrantLock();
    private int pdfCount = 0;

    public DirectoryScannerUsingSingleThread(String directoryPath) {
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
        try {
            scanDirectory(new File(directoryPath));
        } catch (Exception e) {
            System.err.println("Exception in single-thread scan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void scanDirectory(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanDirectory(child);
                }
            }
        } else if (file.getName().toLowerCase().endsWith(".pdf")) {
            incrementAndNotify(Thread.currentThread().getName());
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
                        listener.onPdfCountChanged("SingleThread", threadName, currentCount);
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
