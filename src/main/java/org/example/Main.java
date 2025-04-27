/*
* Group members:
* Dania Hazm Sadeq | dh20066@auis.edu.krd
* Zahraa Zahir Alhamdani | zz21260@auis.edu.krd
* */

package org.example;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String... args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Please provide a PATH to a directory: ");
        String path = input.nextLine();

        PdfCountLogger logger = new PdfCountLogger();
        Thread loggerThread = new Thread(logger, "Logger");
        loggerThread.start();

        CountDownLatch mainLatch = new CountDownLatch(3);

        // 1) Single Thread Counting Strategy
        DirectoryScannerUsingSingleThread single = new DirectoryScannerUsingSingleThread(path);
        single.addListener(logger);
        new Thread(() -> {
            try {
                single.countPdfs();
            } catch (Exception e) {
                System.err.println("Error in single-thread scanner: " + e.getMessage());
                e.printStackTrace();
            } finally {
                mainLatch.countDown();
            }
        }, "Single-Scanner").start();

        // 2) Four Threads Counting Strategy
        DirectoryScannerUsingFourThreads four = new DirectoryScannerUsingFourThreads(path);
        four.addListener(logger);
        new Thread(() -> {
            try {
                four.countPdfs();
            } catch (Exception e) {
                System.err.println("Error in four-thread scanner: " + e.getMessage());
                e.printStackTrace();
            } finally {
                mainLatch.countDown();
            }
        }, "Four-Scanner").start();

        // 3) Thread-pool Counting Strategy
        DirectoryScannerUsingThreadPool pool = new DirectoryScannerUsingThreadPool(path);
        pool.addListener(logger);
        new Thread(() -> {
            try {
                pool.countPdfs();
            } catch (Exception e) {
                System.err.println("Error in thread-pool scanner: " + e.getMessage());
                e.printStackTrace();
            } finally {
                mainLatch.countDown();
            }
        }, "Pool-Scanner").start();

        try {
            mainLatch.await();
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Final PDF count (Single Thread): " + single.getPdfCount());
        System.out.println("Final PDF count (Four Threads): " + four.getPdfCount());
        System.out.println("Final PDF count (Thread Pool): " + pool.getPdfCount());

        loggerThread.interrupt();
    }
}
