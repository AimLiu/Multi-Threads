package com.learn.concurrent.coordination;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 10:32
 * @Description:
 */

public class SyncToolsDemo {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatchDemo();
        CyclicBarrierDemo();
        SemaphoreDemo();
    }

    /** Semaphore：限制同时只允许 3 条指令发往同一网关 */
    private static void SemaphoreDemo() {
        Semaphore permits = new Semaphore(3);
        for (int i = 0; i < 10; i++) {
            String cmd = "cmd-" + i;
            new Thread(() -> {
                try {
                    permits.acquire();           // 拿许可（拿不到就等）
                    System.out.println("下发：" + cmd);
                    sleep(200);         // 模拟指令耗时
                    permits.release();      // 归还许可
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            sleep(20);
        }
        sleep(1000);
        System.out.println("\n=== 任意时刻最多 3 条指令在途 ===");
    }

    /** CyclicBarrier：3 路采集线程各自就绪后，同一时刻开始一轮采集（可多轮复用） */
    private static void CyclicBarrierDemo() {
        CyclicBarrier ready = new CyclicBarrier(3, () -> {
            System.out.println("--- 三路全部就绪，本轮采集开始 ---");
        });
        for (int i = 0; i < 6; i++) {
            new Thread(() -> {
                try {
                    sleep((long) (Math.random() * 300));
                    System.out.println(Thread.currentThread().getName() + " 就绪");
                    ready.await();
                    System.out.println(Thread.currentThread().getName() + " 开始采集");
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, "collector-" + i).start();;
        }
        sleep(1000);
        System.out.println();
    }



    /** CountDownLatch：网关重启前，等待全部 5 个子设备断开连接 */
    private static void CountDownLatchDemo() throws InterruptedException {
        CountDownLatch allDisconnected = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            String device = "sensor-" + i;
            new Thread(() -> {
                sleep((long) (Math.random() * 200));
                System.out.println(device + "已断开");
                allDisconnected.countDown();
            }).start();
        }
        allDisconnected.await();    // 阻塞到计数归零
        System.out.println("=== 全部设备已断开，网关可以重启 ===\n");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }
}
