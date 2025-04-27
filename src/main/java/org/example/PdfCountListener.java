package org.example;

public interface PdfCountListener {
    void onPdfCountChanged(String countingStrategy, String threadName, int newCount);
}
