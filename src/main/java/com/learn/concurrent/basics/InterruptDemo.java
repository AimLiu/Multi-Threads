package com.learn.concurrent.basics;

import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 16:17
 * @Description:
 */

public class InterruptDemo {
    /** 模拟设备轮询任务：用中断位做退出条件，是 Java 里最标准的优雅停止写法 */
    static class DevicePollingTask implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("[" +Thread.currentThread().getName() +"] 采集一次设备数据");
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    System.out.println("sleep 中被中断（标志位已被清除），直接退出");
                    break;
                }
            }
            System.out.println("[" + Thread.currentThread().getName() + "] 优雅退出（可在此释放资源）");
        }
    }
    public static void main(String[] args) throws InterruptedException {
        Thread poller = new Thread(new DevicePollingTask(), "device-poller");
        poller.start();

        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }, "log-flusher");
        //daemon.setDaemon(true);  // 守护线程：main 一结束整个 JVM 一起退出
        daemon.start();

        TimeUnit.SECONDS.sleep(2);
        poller.interrupt();
        poller.join();
        System.out.println("main 结束（daemon 随 JVM 消亡，不会打印任何退出信息）");
    }
}
