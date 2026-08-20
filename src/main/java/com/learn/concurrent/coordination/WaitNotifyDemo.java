package com.learn.concurrent.coordination;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 9:44
 * @Description:
 */

public class WaitNotifyDemo {

    private final static ReentrantLock turnLock = new ReentrantLock();
    private final static Condition oddCondition = turnLock.newCondition();
    private final static Condition evenCondition = turnLock.newCondition();

    public static void main(String[] args) throws InterruptedException {
        // 反例：不持有 monitor 直接 wait → IllegalMonitorStateException
        Object bad = new Object();
        try {
            bad.wait();
        } catch (Exception e) {
            System.out.println("未持锁直接 wait 会抛：" + e.getClass().getSimpleName());
        }

        // 正例：奇偶两线程交替打印 1~100

        int[] next = {1};
        Runnable oddJob = ()->{
            try {
                printTurnOdd(next, "奇数线程1");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        Runnable evenJob = ()->{
            try {
                printTurnEven(next, "偶数线程");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        Thread odd = new Thread(oddJob);
        Thread even = new Thread(evenJob);
        odd.start();
        even.start();
        odd.join();
        even.join();
        System.out.println("交替打印完成");
    }

    static void printTurnOdd(int[] next, String name) throws InterruptedException {
        turnLock.lock();
        try {
            while (next[0] <= 100){
                if (next[0] % 2 == 1){
                    System.out.println(name + ": " + next[0]++);
                    evenCondition.signal();
                }else{
                    oddCondition.await();
                }
            }
            evenCondition.signal();
        }finally {
            turnLock.unlock();
        }
    }

    static void printTurnEven(int[] next, String name) throws InterruptedException {
        turnLock.lock();
        try {
            while (next[0] <= 100){
                if (next[0] % 2 == 0){
                    System.out.println(name + ": " + next[0]++);
                    oddCondition.signal();
                }else{
                    evenCondition.await();
                }
            }
            oddCondition.signal();
        }finally {
            turnLock.unlock();
        }
    }
}
