package com.learn.concurrent.pool;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 10:48
 * @Description:
 */

public class CustomThreadPoolDemo {
    public static void main(String[] args) throws InterruptedException {
        // ① 命名工厂 + 未捕获异常兜底
        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory namedFactory = r->{
            Thread t = new Thread(r, "iot-worker-" + seq.getAndIncrement());
            t.setUncaughtExceptionHandler((thread,e)->{
                System.out.println("[" + thread.getName() + "] 未捕获异常: " + e);
            });
            return t;
        };

        ThreadPoolExecutor pool = new ThreadPoolExecutor(4,
                4,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                namedFactory);
        // ② 监控：每 500ms 打一次核心指标（生产中改为上报到监控系统）
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pool-monitor");
            t.setDaemon(true);
            return t;
        });
        monitor.scheduleAtFixedRate(() -> System.out.printf("[监控] active=%d queue=%d completed=%d%n",
                        pool.getActiveCount(), pool.getQueue().size(), pool.getCompletedTaskCount()),
                0, 100, TimeUnit.MILLISECONDS);
        // ③ 业务任务
        for (int i = 0; i < 20; i++) {
            pool.execute(()->{
                System.out.println("[" + Thread.currentThread().getName() + "] 处理一条设备消息");
                sleep(3000);
            });
        }
        // ④ 优雅关闭三步曲
        pool.shutdown();        // 拒新，等旧
        boolean graceful = pool.awaitTermination(5, TimeUnit.SECONDS);
        if (!graceful) {
            pool.shutdownNow();
        }
        List<Runnable> runnables = monitor.shutdownNow();// 超时：中断在跑的任务
        System.out.println("线程池已" + (graceful ? "优雅" : "强制") + "关闭");
        runnables.forEach(System.out::println);
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
