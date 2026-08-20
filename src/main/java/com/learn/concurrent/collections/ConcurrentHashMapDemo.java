package com.learn.concurrent.collections;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 17:32
 * @Description:
 */

public class ConcurrentHashMapDemo {
    // 场景：设备会话注册表（服务里全局一份，所有请求线程并发读写）
    static final Map<String, String> DEVICE_SESSION = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        // ① 原子复合操作：不存在才创建（会话首次注册的标准写法）
        DEVICE_SESSION.computeIfAbsent("device-001", id-> "session-"+ id);
        System.out.println("首次注册后, size = ：" + DEVICE_SESSION.size());
        // 已存在，loader 不执行
        DEVICE_SESSION.computeIfAbsent("device-001", id-> "session-new");
        System.out.println("重复注册不会覆盖: " + DEVICE_SESSION.get("device-001"));

        // ② 20 线程并发注册 200 台（每线程负责 10 台，无交集）
        CountDownLatch done = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            int base = i * 10;
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    String id = "device-" + (100 + base + j);
                    DEVICE_SESSION.computeIfAbsent(id, k-> "session-" + k);
                }
                done.countDown();
            }).start();
        }

        done.await();
        System.out.println("并发注册后 size（期望 201）= " + DEVICE_SESSION.size());

        // ③ merge：按消息类型统计接收量（物联网指标统计常用）
        ConcurrentHashMap<String, Long> msgCount = new ConcurrentHashMap<>();
        List<String> incoming = List.of("telemetry", "telemetry", "event", "telemetry", "event");
        incoming.forEach(type -> msgCount.merge(type, 1L, Long::sum));  // 无则置 1，有则累加

        // ④ 弱一致迭代：迭代中并发修改不抛异常
        for (Map.Entry<String, String> e : DEVICE_SESSION.entrySet()) {
            DEVICE_SESSION.remove("device-100");
            break;
        }
        System.out.println("迭代中并发修改未抛异常，device-100 已被移除: "
                + !DEVICE_SESSION.containsKey("device-100"));
    }
}
