package com.learn.concurrent.coordination;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 11:24
 * @Description:
 */

public class CompletableFutureDemo {
    public static void main(String[] args) {
        // 零、热身：一个最小的异步链
        CompletableFuture.supplyAsync(()->"raw:26.5C")
                .thenApply(s->s.split(":")[1])  //转换
                .thenAccept(v-> System.out.println("解析温度：" + v))
                .join();     // 仅为了让 main 等它打印

        // 一、IoT scatter-gather：并行查询 8 台设备，单台超时不影响整体
        ThreadPoolExecutor pool = new ThreadPoolExecutor(4,
                8,
                60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r, "cmd-io-" + tId());
                    t.setDaemon(true);
                    return t;
                });
        List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, 8)
                .mapToObj(id -> CompletableFuture
                        .supplyAsync(() -> queryDevice(id), pool)
                        .orTimeout(2, TimeUnit.SECONDS)                       // 单台最多等 2s
                        .exceptionally(e -> "device-" + id + " 查询失败（"
                                + e.getClass().getSimpleName() + "）"))      // 单台失败兜底
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])) // 等全部完成
                .thenRun(()->{
                        System.out.println("--- 聚合结果 ---");
                        futures.forEach(f-> System.out.println(f.join()));
                })
                .join();

        pool.shutdown();
    }

    /** 模拟设备查询：device-5、device-6 网络差会超时 */
    static String queryDevice(int id) {
        sleep(id == 5 || id == 6 ? 5000 : 300);
        return "device-" + id + " 在线，电量 " + (60 + id) + "%";
    }

    static long tId() {
        return Thread.currentThread().getId();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
