package org.example;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ThreadPoolWorker implements Runnable {
    private static final long POLL_TIMEOUT_MS = 100;
    private final BlockingQueue<File> queue;
    private final DirectoryScannerUsingThreadPool scanner;
    private final CountDownLatch latch;

    public ThreadPoolWorker(BlockingQueue<File> queue,
                            DirectoryScannerUsingThreadPool scanner,
                            CountDownLatch latch) {
        this.queue = queue;
        this.scanner = scanner;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            while (true) {
                File file = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (file == null) break;
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    if (children != null) {
                        for (File c : children) queue.offer(c);
                    }
                } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                    scanner.incrementAndNotify(Thread.currentThread().getName());
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("ThreadPoolWorker interrupted: " + Thread.currentThread().getName());
        } catch (Exception e) {
            System.err.println("Error in ThreadPoolWorker " +
                    Thread.currentThread().getName() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }
}

/*
* We asked ChatGPT to suggest a distribution algorithm that distributes the work among the threads of the thread-pool
* While this algorithm is not strictly fair, it is still an attempt to utilize the CPU as much as possible
* */