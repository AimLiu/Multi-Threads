package com.learn.concurrent.basics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 16:32
 * @Description:
 */

public class ThreadLocalDemo {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    // 物联网场景：当前正在处理的设备 ID，随调用链隐式传递，无需层层加参数
    private static final ThreadLocal<String> CURRENT_DEVICE = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        // 仅演示用；生产环境的线程池应自定义并监控（任务 6.2）
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i <= 4; i++) {
            String deviceId = "device-" + i;

            pool.submit(() -> {
                try {
                    if (Integer.valueOf(deviceId.split("-")[1]) % 2 == 0) {
                        CURRENT_DEVICE.set(deviceId);   // 只有偶数才 set
                    }
                    parseAndReport();
                } finally {
                    //CURRENT_DEVICE.remove();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }

    static void parseAndReport() {
        System.out.println("[" + Thread.currentThread().getName() + "] "
                + TIME.format(LocalDateTime.now()) + " 处理 " + CURRENT_DEVICE.get() + " 的上报");
    }
}
