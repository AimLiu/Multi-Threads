package com.learn.concurrent.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/18 16:46
 * @Description:
 */

public class ReadWriteLockDemo {
    private final Map<String, String> config = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new  ReentrantReadWriteLock();
    private final StampedLock stampedLock = new  StampedLock();

    public String read(String key) {
        rwLock.writeLock().lock();
        try {
            return config.get(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void write(String key, String value) {
        rwLock.writeLock().lock();
        try {
            config.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** StampedLock 乐观读：不加锁读 → 校验版本 → 没被写打扰才算数，否则退化为悲观读锁重读 */
    public String optimisticRead(String key) {
        long stamp = stampedLock.tryOptimisticRead();
        String v = config.get(key);
        if (!stampedLock.validate(stamp)) {     // 读期间有写发生 → 作废
            stamp = stampedLock.readLock();
            try {
                v = config.get(key);
            }finally {
                stampedLock.unlockRead(stamp);
            }
        }
        return v;
    }

    public static void main(String[] args) throws InterruptedException {
        ReadWriteLockDemo demo = new ReadWriteLockDemo();
        demo.write("report.interval", "30s");
        demo.write("fw.version", "v1.4.2");

        Runnable reader = ()->{
            System.out.println("[" + Thread.currentThread().getName() + "] 读取上报周期="
                    + demo.read("report.interval") + "，固件=" + demo.optimisticRead("fw.version"));
        };
        Thread t1 = new Thread(reader, "reader-1");
        Thread t2 = new Thread(reader, "reader-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("两读线程交错输出（未互相阻塞）");
    }

}
