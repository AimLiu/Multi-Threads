package com.learn.concurrent.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 16:49
 * @Description:
 */

public class Ex5TimeoutFallback {
    static CompletableFuture<String> fetchConfig(long delayMs) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(delayMs);
            return "cfg-v1-real";
        });
    }

    public static void main(String[] args) {
        // 注意：handle 返回的还是 CF<String>（装着"兜底后的值"），打印前要 join
        String fast = fetchConfig(200)
                .orTimeout(500, TimeUnit.MILLISECONDS)
                .handle((v, e) -> {
                    if (e != null) {
                        System.out.println("发生异常：" + e.getMessage());
                        return "cfg-default";
                    }
                    return v;
                })          // ← 你来写：e != null ? "cfg-default" : v
                .join();
        System.out.println("快路径: " + fast);

        String slow = fetchConfig(1000)
                .completeOnTimeout("这是completeOnTimeout的slow",500, TimeUnit.MILLISECONDS)
                .handle((v, e) -> {
                    if (e != null) {
                        System.out.println("发生异常：" + e.getMessage());
                        return "cfg-default";
                    }
                    return v;
                })          // ← 你来写：e != null ? "cfg-default" : v
                .join();                     // ← 你来写：同款，delay 改 1000ms
        System.out.println("慢路径: " + slow);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
