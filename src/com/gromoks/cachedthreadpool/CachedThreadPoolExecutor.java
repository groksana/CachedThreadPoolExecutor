package com.gromoks.cachedthreadpool;

import java.util.*;
import java.util.concurrent.*;

public class CachedThreadPoolExecutor implements ExecutorService {

    private static final Long INIT_TIME_MS = 0L;
    private final Queue<Runnable> runnableTaskQueue = new LinkedList<>();
    private final Map<Thread, Long> threadPool;
    private volatile boolean isActive;

    private final Thread dutyThread;

    public CachedThreadPoolExecutor() {
        isActive = true;
        threadPool = new ConcurrentHashMap<>();

        Runnable dutyThreadRunner = this::dutyThreadRunner;
        dutyThread = new Thread(dutyThreadRunner);
        dutyThread.setDaemon(true);
        dutyThread.start();
    }

    public int getThreadCount() {
        return threadPool.size();
    }

    @Override
    public void shutdown() {
        isActive = false;
        synchronized (runnableTaskQueue) {
            runnableTaskQueue.notifyAll();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> runnableList = new ArrayList<>();

        shutdown();

        threadPool.forEach((thread, value) -> thread.interrupt());
        dutyThread.interrupt();

        synchronized (runnableTaskQueue) {
            runnableList.addAll(runnableTaskQueue);
            runnableTaskQueue.clear();
        }
        return runnableList;
    }

    @Override
    public boolean isShutdown() {
        return !isActive;
    }

    @Override
    public boolean isTerminated() {
        return runnableTaskQueue.isEmpty() && !isActive;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> futureTask = new FutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> futureTask = new FutureTask<>(task, result);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<Void> futureTask = new FutureTask<Void>(task, null);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> futureList = new ArrayList<>();

        for (Callable<T> task : tasks) {
            RunnableFuture<T> future = new FutureTask<T>(task);
            futureList.add(future);
            execute(future);
        }

        for (int i = 0; i < futureList.size(); i++) {
            Future<T> future = futureList.get(i);
            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return futureList;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public void execute(Runnable command) {
        if (isActive) {
            if (!isAnyAvailableThread()) {
                threadInit();
            }
            synchronized (runnableTaskQueue) {
                runnableTaskQueue.offer(command);
                runnableTaskQueue.notify();
            }
        }
    }

    public void printThreadState() {
        for (Map.Entry<Thread, Long> element : threadPool.entrySet()) {
            System.out.println("Status of " + element.getKey().getName()
                    + " - " + element.getKey().getState() + " - " + element.getKey().isAlive());
        }
    }

    private void threadInit() {
        Runnable taskRunner = this::taskRunner;
        Thread thread = new Thread(taskRunner);
        threadPool.put(thread, 0L);
        thread.start();
    }

    private void taskRunner() {
        while (!isTerminated()) {
            Runnable task;

            synchronized (runnableTaskQueue) {
                while (runnableTaskQueue.isEmpty()) {
                    try {
                        Thread currentThread = Thread.currentThread();
                        threadPool.put(currentThread, System.currentTimeMillis());
                        runnableTaskQueue.wait();
                        if (!isActive) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Thread with name " + Thread.currentThread().getName() + " interrupted by timeout: " + e);
                        threadPool.remove(Thread.currentThread());
                        break;
                    }
                }

                if (!Thread.currentThread().isInterrupted()) {
                    task = runnableTaskQueue.poll();
                } else {
                    break;
                }
            }

            if (task != null) {
                String name = Thread.currentThread().getName();
                System.out.println("Task Started by Thread :" + name);
                task.run();
                System.out.println("Task Finished by Thread :" + name);
            }
        }
    }

    private void dutyThreadRunner() {
        while (isActive) {
            Long currentTimeMs = System.currentTimeMillis();

            for (Map.Entry<Thread, Long> element : threadPool.entrySet()) {
                Thread currentThread = element.getKey();

                if ((currentTimeMs - element.getValue() > 10000) && !INIT_TIME_MS.equals(element.getValue())) {
                    currentThread.interrupt();
                    System.out.println(currentThread);
                    System.out.println("1 size = " + threadPool.size());
                    threadPool.remove(currentThread);
                    System.out.println("2 size = " + threadPool.size());
                    System.out.println("Thread with name " + currentThread.getName() + " and time - " + element.getValue()
                            + " has been interrupt at " + System.currentTimeMillis());
                }
            }
        }
    }

    private boolean isAnyAvailableThread() {
        int count = 0;
        for (Map.Entry<Thread, Long> element : threadPool.entrySet()) {
            System.out.println("Check available thread = " + element.getKey().getName() + " - " + element.getKey().getState());
            if (element.getKey().isAlive() && (element.getKey().getState() != Thread.State.RUNNABLE)
                    && (element.getKey().getState() != Thread.State.BLOCKED)) {
                System.out.println("!!!Available Thread State = " + element.getKey().getState());
                count++;
                break;
            }
        }
        return count != 0;
    }
}
