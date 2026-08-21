package com.learn.concurrent.pool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.LongStream;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/21 14:59
 * @Description:
 */

public class ForkJoinDemo {
    /** 分治求和：区间足够小直接算，否则拆两半并行（理解工作窃取的最小示例） */
    static class SumTask extends RecursiveTask<Long> {
        private final long[] numbers;
        private final int from, to;
        static final int THRESHOLD = 10_000;

        SumTask(long[] numbers, int from, int to) {
            this.numbers = numbers;
            this.from = from;
            this.to = to;
        }

        @Override
        protected Long compute() {
            if (to - from <= THRESHOLD) {
                long sum = 0;
                for (int i = from; i < to; i++) {
                    sum += numbers[i];
                }
                return sum;
            }
            int mid = (from + to) >>> 1;
            SumTask left = new SumTask(numbers, from, mid);
            SumTask right = new SumTask(numbers, mid, to);
            left.fork();                          // 左半压入本线程队列（可能被别人偷走执行）
            return right.compute() + left.join(); // 当前线程顺手算右半
        }
    }
    public static void main(String[] args) {
        long[] numbers = LongStream.rangeClosed(1, 100_000_000).toArray();

        long t1 = System.nanoTime();
        long serial = 0;
        for (long n : numbers) {
            serial += n;
        }
        System.out.println("串行    = " + serial + "，耗时 " + ms(t1));

        long t2 = System.nanoTime();
        long forkJoin = ForkJoinPool.commonPool().invoke(new SumTask(numbers, 0, numbers.length));
        System.out.println("ForkJoin = " + forkJoin + "，耗时 " + ms(t2));

        long t3 = System.nanoTime();
        long parallel = LongStream.of(numbers).parallel().sum();
        System.out.println("并行流  = " + parallel + "，耗时 " + ms(t3));
    }

    static long ms(long fromNano) { return (System.nanoTime() - fromNano) / 1_000_000; }
}
