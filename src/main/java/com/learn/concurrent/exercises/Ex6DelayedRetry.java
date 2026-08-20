package com.learn.concurrent.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.delayedExecutor;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 17:01
 * @Description:
 */

public class Ex6DelayedRetry {
    static boolean sendOnce(String cmd) {
        sleep(100);
        return Math.random() < 0.5;
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        CompletableFuture<Boolean> result = CompletableFuture
                .supplyAsync(() -> sendOnce("sync"))
                .thenCompose(ok -> ok
                        ? CompletableFuture.completedFuture(true)
                        : CompletableFuture.supplyAsync(() -> sendOnce("sync-retry"),
                        delayedExecutor(1000, TimeUnit.MILLISECONDS)));
        Boolean ok = result.join();          // ★ 接住 + 等
        long end = System.nanoTime();
        System.out.println("最终" + (ok ? "成功" : "失败"));
        System.out.println("运行结束，耗时为：" + (end - start)/1_000_000 );
    }

    public static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}