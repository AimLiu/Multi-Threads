package com.learn.concurrent.exercises;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 16:26
 * @Description:
 */

public class Ex3Combine {
    public static void main(String[] args) {
        long start = System.nanoTime();
        // TODO:
        //  left  = supplyAsync: sleep 300 → "在线"
        CompletableFuture<String> left = CompletableFuture.supplyAsync(() -> {
            sleep(300);              // 模拟远程查询耗时
            return "在线";
        });
        //  right = supplyAsync: sleep 100 → 87
        CompletableFuture<Integer> right = CompletableFuture.supplyAsync(() -> {
            sleep(100);              // 模拟远程查询耗时
            return 87;
        });
        //  thenCombine 合并 → 打印 "状态=在线, 电量=87%"
        left.thenCombine(right,(status, charge)->{
            return "设备当前的状态为：" + status + "，电量为: " + charge;
        }).thenAccept(System.out::println).join();
        //  join 收尾，打印总耗时
        long end = System.nanoTime();
        System.out.println("耗时为：" + (end-start)/1_000_000  + "ms");
    }

    static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}