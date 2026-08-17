package com.learn.concurrent.basics;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 15:27
 * @Description:
 */

public class ThreadCreationDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 方式一：继承 Thread
        Thread t1 = new Thread("继承Thread线程") {
            @Override
            public void run() {
                System.out.println("[" + Thread.currentThread().getName() + "] 方式一运行中");
            }
        };

        // 方式二：实现 Runnable（推荐：任务与线程解耦）
        Runnable task2 = () -> System.out.println("["+Thread.currentThread().getName()+"] 方式二运行中");
        Thread t2 = new Thread(task2, "Runable线程");

        // 方式三：Callable + FutureTask，有返回值、可抛异常
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] 方式三运行中");
            return "方式三的结果返回";
        });
        Thread t3 = new Thread(futureTask, "Callable线程");

        // 使用run 显示的Thread.currentThread().getName()得到的值是main,
        t1.run();
        t2.start();
        t3.start();

        System.out.println("[方式三返回值]：" + futureTask.get()); // 阻塞等待
        System.out.println("[" +Thread.currentThread().getName()+"] 是main线程");
    }
}
