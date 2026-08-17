package com.learn.concurrent.race;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/17 18:30
 * @Description:
 */

public class AtomicDemo {
    public static void main(String[] args) {
        // ① CAS 基本用法
        AtomicInteger cas = new AtomicInteger(100);
        boolean ok = cas.compareAndSet(100, 200);
        System.out.println("CAS(100→200) 成功？" + ok + "，当前值 " + cas.get());

        // ② ABA：普通 CAS 感知不到中间变化，AtomicStampedReference 用版本号识破
        AtomicInteger naive = new AtomicInteger(100);
        naive.set(50);
        naive.set(100);                            // 值回来了（A→B→A）
        System.out.println("普通 CAS 以为没变过（成功替换）？" + naive.compareAndSet(100, 999));

        new AtomicReference<>()
    }
}
