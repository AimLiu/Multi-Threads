package com.learn.concurrent.scenarios;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 16:53
 * @Description:
 */

public class DeviceSessionManager {
    static class DeviceSession {
        final String deviceId;
        volatile long lastActiveAt;      // 心跳时间戳（毫秒）
        DeviceSession(String deviceId) {
            this.deviceId = deviceId;
            this.lastActiveAt = System.currentTimeMillis();
        }
    }

    private final ConcurrentHashMap<String, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scanner;
    private final long evictAfterMillis;   // 超过该时长无心跳 → 判离线

    public DeviceSessionManager() {
        this(15_000, 5_000);               // 生产默认：15s 超时 / 5s 扫描
    }

    public DeviceSessionManager(long evictAfterMillis, long scanPeriodMillis) {
        this.evictAfterMillis = evictAfterMillis;
        // 后台扫描线程（daemon；任务体内必须 try-catch —— 任务 6.3 的坑！）
        scanner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-scanner");
            t.setDaemon(true);
            return t;
        });
        scanner.scheduleAtFixedRate(this::evictTimeoutSessions,
                scanPeriodMillis, scanPeriodMillis, TimeUnit.MILLISECONDS);
    }

    /** 注册设备（已存在则不覆盖，只刷新心跳） */
    public void register(String deviceId) {
        sessions.compute(deviceId,(k, s)->{
            if(s == null) {
                return new DeviceSession(deviceId);
            }
            s.lastActiveAt = System.currentTimeMillis();
            return s;
        });
    }

    /** 心跳：刷新设备的 lastActiveAt；设备不在线返回 false */
    public boolean heartbeat(String deviceId) {
        // TODO(实现)：提示 —— computeIfPresent(deviceId, (k, s) -> { s.lastActiveAt = now; return s; }) != null
        DeviceSession deviceSession = sessions.computeIfPresent(deviceId, (k, s) -> {
            s.lastActiveAt = System.currentTimeMillis();
            return s;
        });
        if (deviceSession == null) {
            return false;
        }
        return true;
    }

    /** 注销设备 */
    public void unregister(String deviceId) {
        // TODO(实现)
        System.out.println("设备["+deviceId+"] 进行了注销");
        sessions.remove(deviceId);
    }

    /** 扫描并移除超时会话（超过 evictAfterMillis 未心跳 → 打印下线日志并移除） */
    void evictTimeoutSessions() {
        // TODO(实现)：遍历 sessions，now - lastActiveAt > evictAfterMillis 的 remove 并打印 "[下线] xxx"

        long currentTimes = System.currentTimeMillis();
        System.out.println("[扫描] " + java.time.LocalTime.now() + "，当前 " + sessions.size() + " 个会话");
        try {
            for (String deviceId : sessions.keySet()) {
                // 思考：remove 前要不要再校验一次时间戳？（提示：遍历时设备恰好心跳了 —— 竞态！）
                sessions.computeIfPresent(deviceId,(k,s)->{
                    if(s.lastActiveAt + evictAfterMillis < currentTimes) {
                        return null;
                    }
                    return s;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();     // 6.3 的坑：不 catch 就静默取消
        }


    }

    public int onlineCount() { return sessions.size(); }
    public void close() { scanner.shutdownNow(); }
    public boolean isOnline(String deviceId) { return sessions.containsKey(deviceId); }

    /** 模拟验收（main 自带，实现完上面四个方法后直接运行） */
    public static void main(String[] args) throws Exception {
        DeviceSessionManager mgr = new DeviceSessionManager();

        // 验收 1：单线程注册 100 台
        for (int i = 1; i <= 100; i++){
            mgr.register("device-" + i);
        }
        System.out.println("验收1 在线数（期望100）= " + mgr.onlineCount());

        // 验收 2：10 线程并发注册 1000 台（每个 id 只注册一次）
        Thread[] ts = new Thread[10];
        for (int t = 0; t < 10; t++) {
            int base = t * 100;
            ts[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++){
                    mgr.register("d-" + (base + i));
                }
            });
        }
        for (Thread x : ts) {
            x.start();
        }
        for (Thread x : ts){
            x.join();
        }
        System.out.println("验收2 总在线（期望1100）= " + mgr.onlineCount());

        // 验收 3：超时剔除（用短超时配置 3s/1s，独立实例，确定性验证）
        DeviceSessionManager fast = new DeviceSessionManager(3_000, 1_000);
        for (int i = 1; i <= 5; i++) {
            fast.register("fd-" + i);
        }
        Thread heartbeater = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                fast.heartbeat("fd-1");               // 唯一持续心跳的设备
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            }
        });
        heartbeater.setDaemon(true);
        heartbeater.start();
        Thread.sleep(15_000);                          // 等扫描跑几轮
        System.out.println("验收3 fd-1 存活（期望true）= " + fast.isOnline("fd-1")
                + "，总在线（期望1）= " + fast.onlineCount());
        heartbeater.interrupt();
    }
}