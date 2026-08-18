package com.learn.concurrent.locks;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/18 10:44
 * @Description:
 */

public class DeadLockDemo {
    public static void main(String[] args) {
        final Object gatewayA = new Object();
        final Object gatewayB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (gatewayA) {
                sleep(100);
                System.out.println("T1：持有 A，想拿 B");
                synchronized (gatewayB) {
                    System.out.println("T1：拿到 B");
                }
            }
        }, "thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (gatewayB) {
                sleep(100);
                System.out.println("T2：持有 B，想拿 A");
                synchronized (gatewayA) {
                    System.out.println("T2：拿到 A");
                }
            }
        }, "thread-2");
        t1.start();
        t2.start();
        System.out.println("程序卡死不退出。另开终端：jps 找 pid → jstack <pid> 看死锁报告");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
