package com.learn.concurrent.locks;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/18 10:53
 * @Description:
 */

public class DeadLockFixedDemo {
    public static void main(String[] args) {
        final Object gatewayA = new Object();
        final Object gatewayB = new Object();

        // 修复：无论先操作哪台网关，都按全局固定顺序拿锁（这里用 identityHashCode 排序）
        Runnable crossGatewayCmd = () -> {
            Object first  = System.identityHashCode(gatewayA) < System.identityHashCode(gatewayB)
                    ? gatewayA : gatewayB;
            Object second = (first == gatewayA) ? gatewayB : gatewayA;
            synchronized (first) {
                sleep(100);
                synchronized (second) {
                    System.out.println("[" + Thread.currentThread().getName() + "] 完成 A+B 联动指令");
                }
            }
        };

        Thread t1 = new Thread(crossGatewayCmd, "thread-1");
        Thread t2 = new Thread(crossGatewayCmd, "thread-2");
        t1.start(); t2.start();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}