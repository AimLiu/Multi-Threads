package com.learn.concurrent.collections;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 11:48
 * @Description:
 */

public class ScheduledTaskDemo {
    public static void main(String[] args) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "iot-scheulder");
            t.setDaemon(true);
            return t;
        });
        // ① 正常的心跳扫描任务
        scheduler.scheduleAtFixedRate(()->{
            System.out.println("[心跳扫描] " + LocalTime.now());
        },0, 1, TimeUnit.SECONDS);

        // ② 埋雷任务：第 1 次执行就抛异常 → 之后被静默取消！
        scheduler.scheduleAtFixedRate(()->{
            System.out.println("危险任务，即将抛出异常...");
            try {
                throw new RuntimeException("设备连接终端");
            }catch (Exception e){
                System.out.println("捕捉到错误：" + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        Thread.sleep(5000);
        System.out.println("main 结束。注意：危险任务只出现过一次，之后无声无息地消失了");
        scheduler.shutdownNow();
    }
}
