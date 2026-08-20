package com.learn.concurrent.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 15:35
 * @Description:
 */

public class Ex2Compose {
    record DeviceInfo(int id, String config) { }

    static CompletableFuture<Integer> fetchIdByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);                      // 模拟查注册表
            return name.hashCode() & 7;
        });
    }

    static CompletableFuture<String> fetchConfigById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);                      // 模拟查配置中心
            return "cfg-v2-" + id;
        });
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        long start = System.currentTimeMillis();

        // TODO 1（先撞墙）：thenApply 版 —— 类型是 CF<CF<Integer>>，套娃
        CompletableFuture<CompletableFuture<Integer>> nested =
                CompletableFuture.supplyAsync(() -> "device-A")
                        .thenApply(name -> fetchIdByName(name));
        System.out.println("套娃类型：" + nested.getClass().getSimpleName()  // 验证它就是个 CF
                + "（里面装的是 CF<Integer>）");

        // TODO 2: 用 thenCompose 重写，得到 CompletableFuture<DeviceInfo>
        CompletableFuture<DeviceInfo> info = CompletableFuture.supplyAsync(() -> "device-A")
                .thenCompose(name -> fetchIdByName(name))
                .thenCompose(id -> fetchConfigById(id)
                        .thenApply(cfg -> new DeviceInfo(id, cfg)));
        // TODO 3: 打印最终 DeviceInfo
        System.out.println(info.join());
        System.out.println("总耗时 ≈ " + (System.currentTimeMillis() - start) + "ms");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}