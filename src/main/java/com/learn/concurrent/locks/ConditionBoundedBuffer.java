package com.learn.concurrent.locks;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/18 17:32
 * @Description:
 */

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** ReentrantLock + 两个 Condition 手写有界缓冲（ArrayBlockingQueue 的核心思想） */
public class ConditionBoundedBuffer<T> {
    private final Deque<T> queue = new ArrayDeque<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public ConditionBoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {  // ★ while 防虚假唤醒
                notFull.await();        // 释放锁并等待；被 signal 后重新竞争锁再回来
                System.out.println("缓存满仓等待中...");
            }
            queue.add(item);
            notEmpty.signal();      // 精确唤醒"等非空"的线程
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
                System.out.println("缓存空仓等待中...");
            }
            T t = queue.removeFirst();
            notFull.signal();
            return t;
        }finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConditionBoundedBuffer<Object> buffer = new ConditionBoundedBuffer<>(5);
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                try {
                    buffer.put(i);
                    System.out.println("生产：" + i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "sensor-producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                try {
                    Thread.sleep(50);
                    System.out.println("  消费 " + buffer.take());
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "report-consumer");

        consumer.start();
        TimeUnit.SECONDS.sleep(1);
        producer.start();
        producer.join();
        consumer.join();
        System.out.println("=== 有界缓冲演示结束：生产 20 = 消费 20 ===");
    }
}
