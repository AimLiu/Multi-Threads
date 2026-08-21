package com.learn.concurrent.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 15:27
 * @Description:
 */

public class VirtualThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        // ① 轻松创建 10 万个"每设备一个"的阻塞式任务（平台线程这么做会 OOM）
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()){
//        try (ExecutorService pool = Executors.newFixedThreadPool(100_000)){
            for (int i = 0; i < 100_000; i++) {
                int id = i;
                pool.submit(()->{
                    try {
                        if (id <= 3) {
                            System.out.println("虚拟任务 #" + id + "，运行于 " + Thread.currentThread());
                        }
                        TimeUnit.MILLISECONDS.sleep(100);    // 模拟等待设备响应（I/O 阻塞点→自动卸载）
                        return null;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }// try-with-resources：自动等待全部任务完成后关闭

        System.out.println("10 万个虚拟任务全部完成，总耗时 "
                + (System.nanoTime() - start) / 1_000_000 + " ms（若并发上限 8 核，也应远小于 10000s）");

        // ② 直接创建虚拟线程
        Thread vt = Thread.ofVirtual().name("device-keepalive").start(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] 保活任务启动，isVirtual="
                    + Thread.currentThread().isVirtual());
        });
        vt.join();
    }
}
