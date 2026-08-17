package com.learn.concurrent.race;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 17:48
 * @Description:
 */

/** 双重检查锁单例：第一次判空避免无谓加锁，第二次判空防止重复创建 */
public class DeviceConfigHolder {
    // ★ 去掉 volatile 在小概率下会拿到"半初始化"对象（指令重排所致）

    private static volatile DeviceConfigHolder instance;

    private  DeviceConfigHolder() {
        System.out.println("加载设备配置（模拟耗时）...");
    }

    public static DeviceConfigHolder getInstance() {
        if (instance == null) {         // 第一次检查（无锁，快路径
            synchronized (DeviceConfigHolder.class) {
                if (instance == null) {     // 第二次检查（有锁，防重复）
                    instance = new DeviceConfigHolder();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread[] ts = new Thread[10];
        for (int i = 0; i < 10; i++) {
            ts[i] = new Thread(()->{
                System.out.println(Thread.currentThread().getName() + "拿到" + DeviceConfigHolder.getInstance());
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }
}
