package org.example;

public interface DirectoryScanner {
    void countPdfs();
    void addListener(PdfCountListener listener);
    int getPdfCount();
    void incrementAndNotify(String threadName);
}
