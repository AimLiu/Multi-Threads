package com.learn.concurrent.race;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 17:30
 * @Description:
 */

public class VisibilityDemo {
    // 实验一：去掉 volatile 再跑 —— worker 可能永远读不到 flag 变 true，程序停不下来
    private static boolean shutdown = false;
    private static Object lock = new Object();
    public static void main(String[] args) throws InterruptedException {
        Thread deviceWorker = new Thread(() -> {
            System.out.println("worker 启动，等待停机指令...");
            while (!shutdown) { /* 忙等 */ }
            System.out.println("worker 收到停机指令，退出");
        });

        Thread deviceTest = new Thread(() -> {
            System.out.println("tester 启动，等待停机指令...");
            while (!shutdown) {
                synchronized (lock) {
                    /* 忙等 */
                    break;
                }
            }
            System.out.println("tester 收到停机指令，退出");
        });

        deviceWorker.start();
        deviceTest.start();
        Thread.sleep(1000);

        synchronized (lock) {
            shutdown = true;                          // volatile 写：对 worker 立即可见
        }
        System.out.println("main 已发出停机指令");
        deviceWorker.join(3000);
        System.out.println("worker 最终状态：" + deviceWorker.getState());
        System.out.println("tester 最终状态：" + deviceTest.getState());

    }
}
