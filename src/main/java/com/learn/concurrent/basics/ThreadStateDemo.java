package com.learn.concurrent.basics;

import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 15:50
 * @Description:
 */

public class ThreadStateDemo {
    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();
        Thread waiting = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();  // 释放锁 → WAITING
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "waiting-thread");

        Thread timedWaiting = new Thread(() -> {
            try {                    // → TIMED_WAITING
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "timed-waiting-thread");

        Thread blockd = new Thread(() -> {
            synchronized (lock) {   // 抢不到锁 → BLOCKED
                System.out.println("blocked-thread 拿到了锁（main释放后)");
            }
        }, "blocked-thread");

        Thread finished = new Thread(() -> { }, "finished-thread");

        System.out.println("启动前：" + waiting.getState());          // NEW
        waiting.start();
        timedWaiting.start();
        TimeUnit.MILLISECONDS.sleep(5000);       // 等两线程就位

        System.out.println("wait() 中：" + waiting.getState());
        System.out.println("sleep() 中：" + timedWaiting.getState());

        synchronized (lock) {
            blockd.start();
            TimeUnit.MILLISECONDS.sleep(2000);
            System.out.println("抢锁中: " + blockd.getState());    // BLOCKED
            System.out.println("main: " + Thread.currentThread().getState());   // RUNNABLE
            lock.notify();
        }
        finished.start();
        finished.join();
        System.out.println("已结束：" + finished.getState());

        //收尾
        waiting.join();
        timedWaiting.join();
        blockd.join();
        timedWaiting.interrupt();
    }
}

