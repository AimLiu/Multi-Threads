package com.learn.concurrent.collections;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 18:03
 * @Description:
 */

public class BlockingQueueDemo {
    public static void main(String[] args) throws InterruptedException {
        buoundedQueueBackPressure();
        delayQueueRetry();
    }

    /** 有界队列背压：采集（快）→ 缓冲(5) → 上报（慢），缓冲满则采集被拖慢，不丢数据 */
    private static void buoundedQueueBackPressure() throws InterruptedException {
        LinkedBlockingDeque<Integer> buffer = new LinkedBlockingDeque<>(5);

        Thread sensor = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                        buffer.put(i);   // 满 5 条后在这里等待 = 背压
                        System.out.println("采集 " + i + "，缓冲=" + buffer.size());

                }
                buffer.put(-1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "sensor");
        Thread uploader = new Thread(() -> {
            try {
                int v;
                while ((v = buffer.take()) != -1) {
                    Thread.sleep(50);
                    System.out.println(" 上报：" + v);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "uploader");

        sensor.start();
        uploader.start();
        sensor.join();
        uploader.join();
        System.out.println("=== 有界缓冲背压完成 ===\n");
    }

    static void delayQueueRetry() throws InterruptedException {
        class RetryTask implements Delayed{
            final String cmd;
            final long dueAt;

            RetryTask(String cmd, long delayMs) {
                this.cmd = cmd;
                this.dueAt = System.nanoTime() + delayMs * 1_000_000L;
            }

            @Override public long getDelay(TimeUnit unit) {
                return unit.convert(dueAt - System.nanoTime(), TimeUnit.NANOSECONDS);
            }
            @Override public int compareTo(Delayed other) {
                return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
            }
        }

        DelayQueue<RetryTask> retires = new DelayQueue<>();
        retires.put(new RetryTask("校时指令", 200));
        retires.put(new RetryTask("重启指令", 500));    // 后入队但更晚到期

        System.out.println("指令入队，等待到期重试...");
        for (int i = 0; i < 2; i++) {
            RetryTask t = retires.take();   // 阻塞到最早到期的任务可取
            System.out.println("重试：" + t.cmd);
        }
        System.out.println("=== DelayQueue 完成 ===");
    }
}
