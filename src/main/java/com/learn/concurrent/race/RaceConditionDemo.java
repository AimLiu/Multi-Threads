package com.learn.concurrent.race;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 17:09
 * @Description:
 */

public class RaceConditionDemo {
    private static int counter = 0;                 // 共享可变状态，无任何保护
    private static final int THREADS = 10;
    private static final int LOOPS = 100_000;

    public static void main(String[] args) throws InterruptedException {
        for (int round = 0; round <= 1; round++) {
            counter = 0;
            Thread[] threads = new Thread[THREADS];
            for (int i = 0; i < THREADS; i++) {
                threads[i] = new Thread(()->{
                    for (int j = 0; j < LOOPS; j++) {
                        System.out.println("count is : " + counter);
                        counter++;     // 非原子的读-改-写
                    }
                });
            }
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
            System.out.printf("第 %d 轮：期望 %d，实际 %d，丢失 %d 次%n",
                    round, THREADS * LOOPS, counter, THREADS * LOOPS - counter);

        }
    }
}
