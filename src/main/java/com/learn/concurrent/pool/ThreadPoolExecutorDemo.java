package com.learn.concurrent.pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 10:24
 * @Description:
 */

public class ThreadPoolExecutorDemo {
    public static void main(String[] args) throws InterruptedException {
        // core=2, max=4, 队列容量=10
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2,
                4,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        for (int i = 0; i < 8; i++) {
            pool.execute(()->{
                System.out.printf("[%s] %s，pool=%d active=%d queue=%d%n",
                        Thread.currentThread().getName(),
                        "处理设备消息", pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size());
                sleep(300);
            });
        }

        Thread.sleep(2000);
        // 空闲回收后回落
        System.out.println("poolSize 最终=" + pool.getPoolSize());
        pool.shutdown();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
