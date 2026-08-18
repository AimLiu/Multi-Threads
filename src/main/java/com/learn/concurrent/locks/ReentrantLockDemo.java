package com.learn.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/18 10:24
 * @Description:
 */

public class ReentrantLockDemo {
    // 默认非公平
    private final ReentrantLock lock = new ReentrantLock();
    private int counter = 0;

    public void safeIncrement(){
        lock.lock();
        try {
            counter++;
        }finally {
            lock.unlock();      // ★ 铁律：unlock 放 finally
        }
    }
    /** 模拟：同一网关指令需要串行，但 3 秒抢不到锁就快速失败，避免线程池被拖死 */

    public boolean sendCommandWithTimeout(String cmd) throws InterruptedException {
        if (!lock.tryLock(10, TimeUnit.SECONDS)){
            System.out.println("[" + Thread.currentThread().getName() + "] 抢锁超时，放弃指令 " + cmd);
            return false;
        }
        try {
            System.out.println("[" + Thread.currentThread().getName() + "] 获得锁，下发 " + cmd);
            TimeUnit.MILLISECONDS.sleep(200);    // 模拟网络 IO
            return true;
        }finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();

        Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.safeIncrement(); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.safeIncrement(); });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("ReentrantLock 计数：" + demo.counter);    // 恒为 200000

        demo.lock.lock();
        Thread worker = new Thread(() -> {
            try {
                demo.sendCommandWithTimeout("restart");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "cmd-sender");
        worker.start();
        worker.join();
        demo.lock.unlock();
        System.out.println("main 释放锁，程序结束");
    }
}
