package com.learn.concurrent.race;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 17:48
 * @Description:
 */

public class VolatileNotAtomicDemo {
    private static volatile int volatileCounter = 0;   // volatile 只保证可见性
    private static final AtomicInteger atomicCounter = new AtomicInteger(); // CAS 保证原子
    private static final int THREADS = 10;
    private static final int LOOPS = 100_000;

    interface Op{void op();}

    public static void main(String[] args) throws InterruptedException {
        runConcurrently(()->volatileCounter++);
        runConcurrently(atomicCounter::incrementAndGet);
        System.out.println("volatile 计数：" + volatileCounter + "（多半 < " + THREADS * LOOPS + "）");
        System.out.println("Atomic  计数：" + atomicCounter.get() + "（恒等于 " + THREADS * LOOPS + "）");
    }

    static void runConcurrently(Op op) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(()->{
                for (int j = 0; j < LOOPS; j++) {
                    op.op();
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        for (Thread t : ts) {
            t.join();
        }
    }
}
