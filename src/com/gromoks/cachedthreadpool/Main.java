package com.gromoks.cachedthreadpool;

import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        CachedThreadPoolExecutor cachedThreadPoolExecutor = new CachedThreadPoolExecutor();

        cachedThreadPoolExecutor.execute(() -> {
            System.out.println("Thread1" + Thread.currentThread().getName());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Count1 = " + cachedThreadPoolExecutor.getThreadCount());

        cachedThreadPoolExecutor.execute(() -> {
            System.out.println("Thread1" + Thread.currentThread().getName());
        });

        System.out.println("Count1 = " + cachedThreadPoolExecutor.getThreadCount());
    }
}
