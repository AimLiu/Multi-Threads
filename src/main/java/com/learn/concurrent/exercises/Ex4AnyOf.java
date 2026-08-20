package com.learn.concurrent.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 16:46
 * @Description:
 */

public class Ex4AnyOf {
    public static void main(String[] args) {
        long start = System.nanoTime();

        CompletableFuture<String> gw1 = CompletableFuture.supplyAsync(
                () -> sendVia("gw-1", 100 + (long) (Math.random() * 400)));
        CompletableFuture<String> gw2 = CompletableFuture.supplyAsync(
                ()->sendVia("gw-2", 100 + (long) (Math.random() * 400))
        );   // ← 你来写：同款 gw-2
        CompletableFuture<String> gw3 = CompletableFuture.supplyAsync(
                ()->sendVia("gw-3", 100 + (long) (Math.random() * 400))
        );   // ← 你来写：同款 gw-3

        CompletableFuture<Object> first = CompletableFuture.anyOf(gw1, gw2, gw3)
                .orTimeout(1, TimeUnit.SECONDS);   // 整体兜底
        System.out.println("最先返回: " + first.join());   // anyOf 泛型是 Object
        System.out.println("总耗时 ≈ " + (System.nanoTime() - start) / 1_000_000 + "ms");
    }

    static String sendVia(String gateway, long delayMs) {
        try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return gateway + ": ack-ok";
    }
}
