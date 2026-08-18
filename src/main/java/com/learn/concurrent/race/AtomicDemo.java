package com.learn.concurrent.race;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 18:30
 * @Description:
 */

public class AtomicDemo {
    public static void main(String[] args) throws InterruptedException {
        // ① CAS 基本用法
        AtomicInteger cas = new AtomicInteger(100);
        boolean ok = cas.compareAndSet(100, 200);
        System.out.println("CAS(100→200) 成功？" + ok + "，当前值 " + cas.get());

        // ② ABA：普通 CAS 感知不到中间变化，AtomicStampedReference 用版本号识破
        AtomicInteger naive = new AtomicInteger(100);
        naive.set(50);
        naive.set(100);                            // 值回来了（A→B→A）
        System.out.println("普通 CAS 以为没变过（成功替换）？" + naive.compareAndSet(100, 999));

        AtomicStampedReference<Integer> stamp = new AtomicStampedReference<>(100, 0);
        int oldStamp = stamp.getStamp();
        stamp.set(50, oldStamp+1);
        stamp.set(100, oldStamp+2);
        boolean caught = stamp.compareAndSet(100, 888, oldStamp, oldStamp);
        System.out.println("带版本号 CAS 识破 ABA（拒绝替换）？" + !caught
                + "，当前值 " + stamp.getReference() + "，版本号 " + stamp.getStamp());


        // ③ LongAdder：模拟设备消息计数（高并发写、偶尔读）
        //LongAdder adder = new LongAdder();
        AtomicInteger adder = new AtomicInteger(0);
        Thread[] ts = new Thread[8];
        long start = System.nanoTime();
        for (int i = 0; i < 8; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < 1_000_000; j++) {
                    //adder.increment();
                    adder.incrementAndGet();
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        System.out.println("LongAdder 8 线程 × 100万 = " + adder
                + "，耗时 " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }
}
