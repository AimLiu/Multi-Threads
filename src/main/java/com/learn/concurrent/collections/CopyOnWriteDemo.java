package com.learn.concurrent.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 9:53
 * @Description:
 */

public class CopyOnWriteDemo {
    // 场景：设备数据监听器注册表。注册/注销极少，数据到达时的遍历通知极频繁
    private static final List<String> LiSTENERS = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        LiSTENERS.add("告警服务");
        LiSTENERS.add("时序库写入器");

        // 遍历期间注册新监听器：不抛异常，且新监听器不会"插进"本轮遍历
        for (String listener : LiSTENERS) {
            System.out.println("通知 " + listener);
            LiSTENERS.add("数据转发服务");    // 写时复制，遍历不受影响
        }

        // 对照实验：普通 ArrayList 边遍历边加 → ConcurrentModificationException
        ArrayList<String> naive = new ArrayList<>(List.of("a", "b"));
        try {
            for (String s : naive) {
                naive.add("c");
            }
        } catch (Exception e) {
            System.out.println("ArrayList 遍历中修改: " + e.getClass().getSimpleName());
        }
        // ConcurrentLinkedQueue：无锁队列，适合高并发生产-单消费
        ConcurrentLinkedDeque<Integer> metrics = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 3; i++) {
            metrics.offer(i);
            System.out.println("无锁队列 poll=" + metrics.poll() + "，剩余 " + metrics.size());
        }
    }
}
