package org.example;

import java.io.File;
import java.util.concurrent.*;

public class FourThreadWorker implements Runnable {
    private static final long POLL_TIMEOUT_MS = 100;
    private final BlockingQueue<File> queue;
    private final DirectoryScannerUsingFourThreads scanner;
    private final CountDownLatch latch;

    public FourThreadWorker(BlockingQueue<File> queue,
                            DirectoryScannerUsingFourThreads scanner,
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
                        for (File c : children) {
                            queue.offer(c);
                        }
                    }
                } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                    scanner.incrementAndNotify(Thread.currentThread().getName());
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("FourThreadWorker interrupted: " + Thread.currentThread().getName());
        } catch (Exception e) {
            System.err.println("Unexpected error in FourThreadWorker " +
                    Thread.currentThread().getName() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }
}


/*
 * We asked ChatGPT to suggest a distribution algorithm that distributes the work among the four threads
 * so no one thread does all the work and leaves the others starving for resources
 * While this algorithm is not strictly fair, it is still an attempt to utilize the CPU as much as possible
 * */