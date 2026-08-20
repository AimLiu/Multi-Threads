package com.learn.concurrent.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 17:15
 * @Description:
 */

public class Ex7WhichThread {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(2,
                r -> {
            Thread t = new Thread(r, "io-pool");
            t.setDaemon(true);
            return t;
        });

        tag("① supplyAsync(pool)", CompletableFuture.supplyAsync(() -> {
            show();
            sleep(200);
            return 1;                    // 任务本体
        }, pool).thenApply(v -> {
            show();
            return v + 1;
        })); // 回调 A：紧跟着注册

        CompletableFuture<Integer> done = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return 1;
        }, pool);

        done.join();                                          // 先等它完成
        done.thenApply(v -> {
            show();
            return v + 1;
        });       // 回调 B：完成后才注册

        tag("② thenApplyAsync(不带池)", CompletableFuture.supplyAsync(() -> 1, pool)
                .thenApplyAsync(v -> {
                    show();
                    return v + 1;
                })
        );   // 回调 C

        pool.shutdown();
    }

    static void show() { System.out.println("    跑在: " + Thread.currentThread().getName()); }
    static void tag(String s, Object ignored) { System.out.println(s); }
    static void sleep(long ms) { try { Thread.sleep(ms);} catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
