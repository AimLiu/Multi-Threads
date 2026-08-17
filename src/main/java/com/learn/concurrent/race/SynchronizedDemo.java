package com.learn.concurrent.race;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 18:13
 * @Description:
 */

public class SynchronizedDemo {
    private int shared = 0;
    private final Object dedicatedLock = new Object(); // 专用锁对象，避免锁 this 被外部干扰

    public synchronized void incInstance(){ shared++; }

    public static synchronized void incStatic() { /* */}

    public void incBlock(){
        synchronized(dedicatedLock){
            shared++;
        }
    }

    // 可重入演示：递归调用同步方法不会自锁
    public synchronized  void reentrantCall(int depth) {
        if (depth > 0) {
            reentrantCall(depth - 1);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SynchronizedDemo demo = new SynchronizedDemo();

        Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.incInstance(); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.incBlock(); });
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("synchronized 代码块计数：" + demo.shared); // 恒为 200000

        demo.reentrantCall(3);
        System.out.println("可重入递归调用成功");
    }
}
