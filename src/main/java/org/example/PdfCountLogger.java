package org.example;

import java.util.concurrent.LinkedBlockingQueue;

public class PdfCountLogger implements Runnable, PdfCountListener {
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    @Override
    public void onPdfCountChanged(String strategy, String threadName, int newCount) {
        queue.offer(String.format("[Printer][%s][%s] PDF count: %d",
                strategy, threadName, newCount));
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String msg = queue.take();
                System.out.println(msg);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Logger thread exiting.");
    }
}
