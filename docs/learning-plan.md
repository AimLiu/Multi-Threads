# Java 多线程学习计划（物联网后端方向）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> （若由 Claude 陪同执行：每个任务独立完成、独立提交；学习者为本人时，按"学习循环"逐任务推进即可。）

**Goal:** 系统掌握 Java 并发编程（线程、锁、竞争、并发容器、线程池、虚拟线程），并落实到物联网后端的典型场景，最终了解业界更成熟的替代方案。

**Architecture:** 以"概念要点 → 可运行 Demo → 预期观察 → 思考题 → 提交"为循环，按 10 个模块递进：基础 → 并发工具 → 场景实战 → 原理深入 → IoT 方案对照。每个 Demo 都是独立的 main 类，可直接用 Maven 运行。

**Tech Stack:** Java 21（LTS，含虚拟线程）、Maven、JUnit 5（少量确定性测试）、git。

**Spec:** 用户需求（2026-08-17）：学习内容须包括 ①基本组件/类使用 ②常见场景实现 ③相关类的特性了解 ④物联网中更成熟的方案（第④点做文档对照即可）。

**创建日期：** 2026-08-17

## Global Constraints（全局约定）

- **JDK 版本：** 21（`maven.compiler.release=21`）。涉及版本差异处会标注"JDK 8 也支持"或"JDK 21 新增"。
- **构建：** Maven；所有示例通过 `mvn -q compile exec:java -Dexec.mainClass=<全限定类名>` 运行。
- **包名规范：** `com.learn.concurrent.<模块名>`（basics / race / locks / coordination / collections / pool / scenarios / internals）。
- **源码编码：** UTF-8（pom 已配置 `project.build.sourceEncoding`）。
- **学习约定：**
  - 每个 Demo 运行前先**书面预测输出**，运行后对照差异——差异点就是理解盲区。
  - 每个"思考题"的答案用自己的话写在 `docs/notes/模块X.md` 里（不查资料先写，再查证修正）。
  - 学习代码允许使用 `Executors` 工厂方法等生产禁用 API，但必须注释标明「仅演示，生产禁用原因见任务 6.2」。
- **提交规范：** 每完成一个任务提交一次，格式 `learn(mX): X.Y 主题`（如 `learn(m2): 2.1 复现竞争条件`）。
- **运行环境注意：** Demo 的输出（线程名、耗时、丢失次数）每次运行都会不同，属正常现象；关注的是"规律"而非具体数字。

---

## 如何使用本文档

每个任务都遵循同一个**学习循环**：

1. **读概念要点**——建立粗略心智模型；
2. **预测**——看代码，写下你预期的输出；
3. **运行并观察**——执行"运行"小节给出的命令，对照预期观察点；
4. **做思考题**——写进 `docs/notes/`；
5. **勾选 checkbox 并 commit**。

进度总表见文末[附录 C](#附录-c进度总表)。

### 目录

| 阶段 | 模块 | 内容 | 对应需求 |
|---|---|---|---|
| 准备 | 模块 0 | 项目初始化 | — |
| 基础篇 | 模块 1 | 线程基础：创建/状态/控制/ThreadLocal | ① |
| 基础篇 | 模块 2 | 竞争与内存可见性：JMM/volatile/synchronized/原子类 | ①③ |
| 工具篇 | 模块 3 | 显式锁：ReentrantLock/死锁/读写锁/Condition | ①③ |
| 工具篇 | 模块 4 | 线程协作：wait-notify/同步工具/CompletableFuture | ①② |
| 工具篇 | 模块 5 | 并发容器：ConcurrentHashMap/BlockingQueue/COW | ①③ |
| 工具篇 | 模块 6 | 线程池：ThreadPoolExecutor/定时任务/ForkJoin/虚拟线程 | ①③ |
| 实战篇 | 模块 7 | 物联网场景实战：会话管理/数据管道/批量指令/限流/缓存 | ② |
| 原理篇 | 模块 8 | 原理深入：AQS/ConcurrentHashMap 内部/ThreadLocal 泄漏/锁优化 | ③ |
| 对照篇 | 模块 9 | 物联网中的成熟方案（纯文档，概念对照） | ④ |

---

# 模块 0：项目初始化

### 任务 0.1：搭建 Maven 工程骨架

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/learn/concurrent/basics/.gitkeep`（占位，首个任务后可删）
- Create: `src/test/java/com/learn/concurrent/.gitkeep`
- Create: `.gitignore`

**步骤：**

- [ ] **Step 1：初始化 git 仓库**

```bash
cd "E:\Program\Java Program\Multithread-Learn"
git init
```

- [ ] **Step 2：创建 `.gitignore`**

```gitignore
target/
*.class
.idea/
*.iml
.vscode/
```

- [ ] **Step 3：创建 `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.learn</groupId>
    <artifactId>multithread-learn</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.10.2</junit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            <!-- 用于直接运行各示例的 main 方法 -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.2.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4：验证环境**

运行：`mvn -v`（确认 Maven 与 JDK 21）；`mvn -q test`（应显示 BUILD SUCCESS，无测试）。

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m0): 0.1 初始化 Maven 工程（Java 21）"
```

---

# 模块 1：线程基础（basics）

**模块目标：** 掌握线程的创建方式、六种状态、中断机制、守护线程与 ThreadLocal——这些是后面一切并发知识的载体。

### 任务 1.1：线程创建的三种方式

**Files:**
- Create: `src/main/java/com/learn/concurrent/basics/ThreadCreationDemo.java`

**概念要点：**
- 三种方式：继承 `Thread` / 实现 `Runnable`（任务与线程解耦，推荐）/ `Callable` + `FutureTask`（有返回值、可抛受检异常）。
- `start()` 才会开新线程；直接调 `run()` 只是普通方法调用（在当前线程执行）。
- `FutureTask.get()` 会阻塞直到结果就绪——这是"异步取结果"的最早形态。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.basics;

import java.util.concurrent.FutureTask;

public class ThreadCreationDemo {
    public static void main(String[] args) throws Exception {
        // 方式一：继承 Thread
        Thread t1 = new Thread("继承Thread线程") {
            @Override
            public void run() {
                System.out.println("[" + Thread.currentThread().getName() + "] 方式一运行中");
            }
        };

        // 方式二：实现 Runnable（推荐：任务与线程解耦）
        Runnable task2 = () ->
                System.out.println("[" + Thread.currentThread().getName() + "] 方式二运行中");
        Thread t2 = new Thread(task2, "Runnable线程");

        // 方式三：Callable + FutureTask，有返回值、可抛异常
        FutureTask<String> futureTask =
                new FutureTask<>(() -> "方式三的返回结果");
        Thread t3 = new Thread(futureTask, "Callable线程");

        t1.start();
        t2.start();
        t3.start();
        System.out.println("方式三返回值：" + futureTask.get()); // 阻塞等待
        System.out.println("[" + Thread.currentThread().getName() + "] 是 main 线程");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.basics.ThreadCreationDemo`
预期观察：三行输出 + main 线程名；前两行**顺序不固定**（线程调度不确定）。

- [ ] **Step 3：思考题（记入 docs/notes/m1.md）**
  1. 把 `t1.start()` 改成 `t1.run()`，输出有什么变化？为什么？
  2. `futureTask.get()` 放到三个 `start()` 之前会怎样？程序行为有区别吗？
  3. Runnable 相比继承 Thread 的优势是什么？（提示：Java 单继承、任务复用、与线程池配合）

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m1): 1.1 线程创建的三种方式"
```

### 任务 1.2：线程状态与生命周期

**Files:**
- Create: `src/main/java/com/learn/concurrent/basics/ThreadStateDemo.java`

**概念要点：**
- 六种状态（`Thread.State`）：`NEW`、`RUNNABLE`、`BLOCKED`（等监视器锁）、`WAITING`（wait/join 无参）、`TIMED_WAITING`（sleep(n)/wait(n)/join(n)）、`TERMINATED`。
- Java 把"就绪"和"运行中"合并为 `RUNNABLE`；注意 Java 的 RUNNABLE **包含了阻塞在网络 IO 上**的情况（这是与操作系统线程状态的一个重要差异，也是模块 9 里 Netty/虚拟线程要解决的问题）。
- BLOCKED 只针对 **synchronized 监视器锁**；等 `Lock.lock()` 时状态是 WAITING/TIMED_WAITING（AQS 用 LockSupport.park 实现）。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.basics;

import java.util.concurrent.TimeUnit;

public class ThreadStateDemo {
    public static void main(String[] args) throws Exception {
        Object lock = new Object();

        Thread waiting = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();                        // 释放锁 → WAITING
                } catch (InterruptedException ignored) { }
            }
        }, "waiting-thread");

        Thread timedWaiting = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);             // → TIMED_WAITING
            } catch (InterruptedException ignored) { }
        }, "timed-waiting-thread");

        Thread blocked = new Thread(() -> {
            synchronized (lock) {                       // 抢不到锁 → BLOCKED
                System.out.println("blocked-thread 拿到了锁（main 释放后）");
            }
        }, "blocked-thread");

        Thread finished = new Thread(() -> { }, "finished-thread");

        System.out.println("启动前：" + waiting.getState());          // NEW
        waiting.start();
        timedWaiting.start();
        TimeUnit.MILLISECONDS.sleep(200);                 // 等两线程就位

        System.out.println("wait() 中：" + waiting.getState());       // WAITING
        System.out.println("sleep() 中：" + timedWaiting.getState()); // TIMED_WAITING

        synchronized (lock) {                             // main 持锁，制造 blocked
            blocked.start();
            TimeUnit.MILLISECONDS.sleep(200);
            System.out.println("抢锁中：" + blocked.getState());      // BLOCKED
            System.out.println("main：" + Thread.currentThread().getState()); // RUNNABLE
            lock.notifyAll();                             // 唤醒 waiting（它还要先抢回锁）
        }

        finished.start();
        finished.join();
        System.out.println("已结束：" + finished.getState());         // TERMINATED

        // 收尾，让程序退出
        timedWaiting.interrupt();
        waiting.join(); timedWaiting.join(); blocked.join();
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.basics.ThreadStateDemo`
预期观察：依次打印 NEW / WAITING / TIMED_WAITING / BLOCKED / RUNNABLE / TERMINATED。
注意本 Demo 的关键细节：`wait()` 会**释放**锁，所以必须由 main 先 `synchronized(lock)` 占住锁，blocked 才真的无锁可抢——若 main 不持锁，blocked 会直接拿到锁跑完，你永远看不到 BLOCKED（这个细节本身就是高频面试题）。

- [ ] **Step 3：附加实验**——用 `jps` 找到本进程 pid，趁程序未结束时执行 `jstack <pid>`，在输出里按线程名搜索，看真实线程栈。

- [ ] **Step 4：思考题**
  1. `waiting.wait()` 之前为什么必须先 `synchronized (lock)`？
  2. 一个线程调 `lock.wait()` 后释放锁，另一个线程 `lock.notifyAll()` 后，前者立刻进入 RUNNABLE 吗？（提示：它要先重新抢到锁）

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m1): 1.2 线程六种状态"
```

### 任务 1.3：线程控制：中断、join、守护线程

**Files:**
- Create: `src/main/java/com/learn/concurrent/basics/InterruptDemo.java`

**概念要点：**
- 中断是**协作式**的：`interrupt()` 只是"举旗"，线程自己在安全点检查并决定如何退出。`Thread.stop()` 因破坏一致性已废弃。
- `sleep/wait/join` 被中断会抛 `InterruptedException`，**且抛出时清除中断标志位**——捕获后要么重新 `Thread.currentThread().interrupt()`，要么退出。
- `isInterrupted()`（实例方法，不清标志） vs `interrupted()`（静态方法，**读取后清除**标志）。
- 守护线程（daemon）不阻止 JVM 退出，适合日志刷盘、指标上报等辅助线程；**不要在守护线程里做关键业务**（JVM 退出时不保证执行完）。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.basics;

import java.util.concurrent.TimeUnit;

public class InterruptDemo {
    /** 模拟设备轮询任务：用中断位做退出条件，是 Java 里最标准的优雅停止写法 */
    static class DevicePollingTask implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("[" + Thread.currentThread().getName() + "] 采集一次设备数据...");
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    System.out.println("sleep 中被中断（标志位已被清除），直接退出");
                    break;
                }
            }
            System.out.println("[" + Thread.currentThread().getName() + "] 优雅退出（可在此释放资源）");
        }
    }

    public static void main(String[] args) throws Exception {
        Thread poller = new Thread(new DevicePollingTask(), "device-poller");
        poller.start();

        Thread daemon = new Thread(() -> {
            while (true) {
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ignored) { }
            }
        }, "log-flusher");
        daemon.setDaemon(true);      // 守护线程：main 一结束整个 JVM 一起退出
        daemon.start();

        TimeUnit.SECONDS.sleep(2);
        poller.interrupt();          // 发中断信号，而不是强杀
        poller.join();
        System.out.println("main 结束（daemon 随 JVM 消亡，不会打印任何退出信息）");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.basics.InterruptDemo`
预期观察：采集打印若干次 → 中断 → 优雅退出 → main 结束后程序**立即**退出（证明 daemon 未阻止退出）。

- [ ] **Step 3：思考题**
  1. 若把 `daemon.setDaemon(true)` 删掉，程序会退出吗？（动手验证）
  2. `while(!Thread.currentThread().isInterrupted())` 与 `while(true)`（依赖 catch 里 break）两种写法各覆盖什么场景？
  3. 物联网后端里，哪些后台线程适合 daemon，哪些绝不可以？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m1): 1.3 中断机制与守护线程"
```

### 任务 1.4：ThreadLocal——线程本地上下文

**Files:**
- Create: `src/main/java/com/learn/concurrent/basics/ThreadLocalDemo.java`

**概念要点：**
- 每个 `Thread` 内有一张 `ThreadLocalMap`，key 是 ThreadLocal 实例的**弱引用**，value 是变量副本。
- 两大用途：①线程不安全的对象每线程一份（经典 `SimpleDateFormat`）；②链路上下文隐式传递（traceId、当前设备 ID、租户 ID）——物联网后端链路追踪的标准做法。
- **线程池场景必须 `remove()`**：线程复用，不清理会"读到上一个任务的上下文"（串号）+ 内存泄漏（详见任务 8.3）。
- 进阶（了解即可）：`InheritableThreadLocal` 在线程池下同样失效——业界用阿里的 TransmittableThreadLocal（TTL）解决；Java 21 还在preview 的 `ScopedValue` 是官方新方向。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.basics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadLocalDemo {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // 物联网场景：当前正在处理的设备 ID，随调用链隐式传递，无需层层加参数
    private static final ThreadLocal<String> CURRENT_DEVICE = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        // 仅演示用；生产环境的线程池应自定义并监控（任务 6.2）
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 1; i <= 4; i++) {
            String deviceId = "device-" + i;
            pool.submit(() -> {
                try {
                    CURRENT_DEVICE.set(deviceId);
                    parseAndReport();      // 调用链深处直接 get()
                } finally {
                    CURRENT_DEVICE.remove();   // ★ 线程复用，必须清理
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }

    static void parseAndReport() {
        System.out.println("[" + Thread.currentThread().getName() + "] "
                + TIME.format(LocalDateTime.now()) + " 处理 " + CURRENT_DEVICE.get() + " 的上报");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.basics.ThreadLocalDemo`
预期观察：4 条日志，线程只有 2 个名字（池大小为 2），但每条设备 ID 都正确——体会"线程复用 + 每次任务设置/清理"。

- [ ] **Step 3：附加实验（复现串号 bug）**——把 `finally` 里的 `remove()` 删掉，再把 `CURRENT_DEVICE.set(deviceId)` 改成"仅偶数设备才 set"，跑几遍：某些奇数设备的日志会打出**上一个任务的设备号**。这就是线上诡异 bug 的常见来源。

- [ ] **Step 4：思考题**
  1. 为什么说"ThreadLocal 是空间换安全"？
  2. 设备上报处理链中传 deviceId，用方法参数逐层传递 vs ThreadLocal，各自优劣？

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m1): 1.4 ThreadLocal 上下文传递"
```

---

# 模块 2：竞争与内存可见性（race）

**模块目标：** 理解并发 bug 的三大根源（原子性/可见性/有序性），掌握 JMM（Java 内存模型）的核心结论 happens-before，学会用 volatile / synchronized / 原子类对症下药。**这是整个并发学习的理论地基。**

### 任务 2.1：复现竞争条件（race condition）

**Files:**
- Create: `src/main/java/com/learn/concurrent/race/RaceConditionDemo.java`

**概念要点：**
- **临界区**：访问共享可变资源的代码段；**竞争条件**：多线程交错执行临界区导致结果依赖调度时序。
- `counter++` 实际是"读→加→写回"三步，两个线程交错时更新互相覆盖。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.race;

public class RaceConditionDemo {
    private static int counter = 0;                 // 共享可变状态，无任何保护
    private static final int THREADS = 10;
    private static final int LOOPS = 100_000;

    public static void main(String[] args) throws InterruptedException {
        for (int round = 1; round <= 3; round++) {
            counter = 0;
            Thread[] threads = new Thread[THREADS];
            for (int i = 0; i < THREADS; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < LOOPS; j++) {
                        counter++;                  // 非原子的读-改-写
                    }
                });
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();
            System.out.printf("第 %d 轮：期望 %d，实际 %d，丢失 %d 次%n",
                    round, THREADS * LOOPS, counter, THREADS * LOOPS - counter);
        }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.race.RaceConditionDemo`
预期观察：三轮结果**几乎必然小于** 1000000，且每轮各不相同。若偶见正好 1000000，说明本轮竞争窗口少，多跑几次。

- [ ] **Step 3：思考题**
  1. 为什么丢失量级通常在几千~几万，而不是接近一半？
  2. 在循环里加一句 `System.out.println` 往往"看起来不丢了"，这能证明问题解决了吗？（println 是 synchronized 的——它只是缩小了窗口）

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m2): 2.1 复现竞争条件"
```

### 任务 2.2：Java 内存模型（JMM）与可见性

**Files:**
- Create: `src/main/java/com/learn/concurrent/race/VisibilityDemo.java`
- Create: `docs/notes/m2-jmm.md`（概念笔记，内容见下）

**概念要点：**
- JMM 三性问题：**原子性**（一个操作不可分）、**可见性**（写对其他线程何时可见）、**有序性**（指令重排）。
- 每个线程有工作内存（可理解为 CPU 缓存/寄存器的抽象），共享变量在线程间默认**不保证立即可见**。
- **happens-before（重点背下来）**——满足以下任一条，写就对读可见：①程序顺序规则（同线程内）；②监视器锁规则（解锁 hb 于后续加锁）；③volatile 规则（写 hb 于后续读）；④线程 start 规则；⑤线程 join 规则；⑥传递性。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.race;

public class VisibilityDemo {
    // 实验一：去掉 volatile 再跑 —— worker 可能永远读不到 flag 变 true，程序停不下来
    private static volatile boolean shutdown = false;

    public static void main(String[] args) throws InterruptedException {
        Thread deviceWorker = new Thread(() -> {
            System.out.println("worker 启动，等待停机指令...");
            while (!shutdown) { /* 忙等 */ }
            System.out.println("worker 收到停机指令，退出");
        }, "device-worker");
        deviceWorker.start();

        Thread.sleep(1000);
        shutdown = true;                          // volatile 写：对 worker 立即可见
        System.out.println("main 已发出停机指令");
        deviceWorker.join(3000);
        System.out.println("worker 最终状态：" + deviceWorker.getState());
    }
}
```

- [ ] **Step 2：运行并观察（两个实验）**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.race.VisibilityDemo`
实验 A（有 volatile）：约 1 秒后 worker 退出，状态 TERMINATED。
实验 B（删掉 volatile，多跑几次）：worker 常常**永远不退出**（join 超时后状态仍 RUNNABLE），需手动终止进程——这就是可见性问题：main 的写被缓存在工作内存里，worker 一直读旧值。

- [ ] **Step 3：整理笔记**——把六条 happens-before 规则抄进 `docs/notes/m2-jmm.md`，每条配一句自己编的物联网例子（例：②监视器锁规则——"下发指令线程释放设备锁之后，状态上报线程再拿锁，一定能看到指令标记"）。

- [ ] **Step 4：思考题**
  1. 不用 volatile，改成读写都加 `synchronized(lock)` 也能解决可见性吗？依据哪条规则？
  2. `boolean flag` 本身是原子的，为什么还会出问题？（原子性 ≠ 可见性）

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m2): 2.2 JMM 与可见性"
```

### 任务 2.3：volatile——可见性有、原子性无；双重检查锁

**Files:**
- Create: `src/main/java/com/learn/concurrent/race/VolatileNotAtomicDemo.java`
- Create: `src/main/java/com/learn/concurrent/race/DeviceConfigHolder.java`

**概念要点：**
- volatile 语义：①写立即刷回主存、读强制从主存取（可见性）；②禁止其前后指令重排（有序性）；③**不保证**复合操作原子（`count++` 照样丢）。
- volatile 的两个典型正确用法：**状态标志位**（任务 2.2）、**双重检查锁（DCL）单例的实例字段**。
- DCL 为什么必须 volatile：`new` 大致分三步（分配内存→初始化→引用赋值），若 2/3 重排，另一线程可能拿到"已赋引用但未初始化完"的半成品对象。

- [ ] **Step 1：编写 VolatileNotAtomicDemo.java**

```java
package com.learn.concurrent.race;

import java.util.concurrent.atomic.AtomicInteger;

public class VolatileNotAtomicDemo {
    private static volatile int volatileCounter = 0;   // volatile 只保证可见性
    private static final AtomicInteger atomicCounter = new AtomicInteger(); // CAS 保证原子
    private static final int THREADS = 10;
    private static final int LOOPS = 100_000;

    interface Op { void op(); }

    public static void main(String[] args) throws InterruptedException {
        runConcurrently(() -> volatileCounter++);      // 仍会丢更新！
        runConcurrently(atomicCounter::incrementAndGet);
        System.out.println("volatile 计数：" + volatileCounter + "（多半 < " + THREADS * LOOPS + "）");
        System.out.println("Atomic  计数：" + atomicCounter.get() + "（恒等于 " + THREADS * LOOPS + "）");
    }

    static void runConcurrently(Op op) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> { for (int j = 0; j < LOOPS; j++) op.op(); });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }
}
```

- [ ] **Step 2：编写 DeviceConfigHolder.java（DCL 单例）**

```java
package com.learn.concurrent.race;

/** 双重检查锁单例：第一次判空避免无谓加锁，第二次判空防止重复创建 */
public class DeviceConfigHolder {
    // ★ 去掉 volatile 在小概率下会拿到"半初始化"对象（指令重排所致）
    private static volatile DeviceConfigHolder instance;

    private DeviceConfigHolder() {
        System.out.println("加载设备配置（模拟耗时）...");
    }

    public static DeviceConfigHolder getInstance() {
        if (instance == null) {                          // 第一次检查（无锁，快路径）
            synchronized (DeviceConfigHolder.class) {
                if (instance == null) {                  // 第二次检查（有锁，防重复）
                    instance = new DeviceConfigHolder();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread[] ts = new Thread[10];
        for (int i = 0; i < 10; i++) {
            ts[i] = new Thread(() -> System.out.println(
                    Thread.currentThread().getName() + " 拿到 " + DeviceConfigHolder.getInstance()));
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }
}
```

- [ ] **Step 3：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.race.VolatileNotAtomicDemo` → volatile 计数丢更新、Atomic 恒等 1000000。
运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.race.DeviceConfigHolder` → "加载设备配置"只打印一次，10 个线程拿到同一个实例（hashCode 相同）。

- [ ] **Step 4：思考题**
  1. 什么时候 volatile 就够了？什么时候必须上锁？（口诀：**多写一步操作必上锁，单写标志用 volatile**——用自己的话重写这个判断标准）
  2. DCL 里去掉外层判空、只留 `synchronized` 内一次判空，功能还对吗？差在哪？

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m2): 2.3 volatile 语义与 DCL"
```

### 任务 2.4：synchronized——内置锁

**Files:**
- Create: `src/main/java/com/learn/concurrent/race/SynchronizedDemo.java`

**概念要点：**
- 三种形式：实例方法（锁 **this**）/ 静态方法（锁 **Class 对象**）/ 代码块（锁**指定对象**，粒度最细，推荐）。
- **锁的是对象，不是代码**——两段"不同"的代码块锁同一对象就互斥；同一方法锁不同对象不互斥（新手高频误区）。
- 可重入：同一线程可重复获取自己已持有的锁（否则递归同步方法直接自锁）。
- 每个对象关联一个 monitor；竞争失败的线程进入对象监视器队列（状态 BLOCKED）。特性深入（锁升级等）见任务 8.4。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.race;

public class SynchronizedDemo {
    private int shared = 0;
    private final Object dedicatedLock = new Object(); // 专用锁对象，避免锁 this 被外部干扰

    // ① 实例方法：锁 this
    public synchronized void incInstance() { shared++; }

    // ② 静态方法：锁 SynchronizedDemo.class
    public static synchronized void incStatic() { /* 略 */ }

    // ③ 代码块：锁指定对象（推荐）
    public void incBlock() {
        synchronized (dedicatedLock) { shared++; }
    }

    // 可重入演示：递归调用同步方法不会自锁
    public synchronized void reentrantCall(int depth) {
        if (depth > 0) reentrantCall(depth - 1);
    }

    public static void main(String[] args) throws InterruptedException {
        SynchronizedDemo demo = new SynchronizedDemo();

        Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.incBlock(); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.incBlock(); });
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("synchronized 代码块计数：" + demo.shared); // 恒为 200000

        demo.reentrantCall(3);
        System.out.println("可重入递归调用成功");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.race.SynchronizedDemo`
预期观察：计数恒为 200000（对照任务 2.1 的丢更新）。

- [ ] **Step 3：附加实验（证明"锁的是对象"）**——写两个方法 `methodA()` 和 `methodB()`，都锁 `dedicatedLock`；让线程 1 在 A 里 sleep 2 秒，线程 2 调 B 并计时：B 会被阻塞 → 同锁互斥。再把 B 改成 `synchronized(this)`：互斥消失（换成锁 this 才互斥）。把实验代码与本任务一起提交。

- [ ] **Step 4：思考题**
  1. 两个线程分别调用同一对象的 `incInstance()`（锁 this）和 `incStatic()`（锁 Class），会互斥吗？
  2. 为什么生产建议"锁专用私有对象"而不是锁 this / 字符串常量？（提示：this 和常量可能被别的代码锁住）

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m2): 2.4 synchronized 内置锁"
```

### 任务 2.5：CAS 与原子类

**Files:**
- Create: `src/main/java/com/learn/concurrent/race/AtomicDemo.java`

**概念要点：**
- CAS（Compare-And-Swap）：CPU 级原子指令"值等于期望才替换"，失败则重试（自旋）。无阻塞、无挂起开销，是 `Atomic*` 家族、`ConcurrentHashMap`、AQS 的地基。
- `AtomicInteger/AtomicLong/AtomicReference/AtomicIntegerArray` 等；`getAndIncrement`/`compareAndSet`/`getAndUpdate`。
- **ABA 问题**：值 A→B→A，普通 CAS 察觉不到"变过"。对计数无影响，但对"引用被复用"敏感的场景（如无锁链表）要用 `AtomicStampedReference`（版本号）。
- **LongAdder**（JDK 8+）：高并发计数比 AtomicLong 快——分段累加（Cell[]），读时求和。适合**写多读少**的指标统计（设备消息计数、在线连接数）；要精确及时读取则用 AtomicLong。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.race;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;

public class AtomicDemo {
    public static void main(String[] args) throws InterruptedException {
        // ① CAS 基本用法
        AtomicInteger cas = new AtomicInteger(100);
        boolean ok = cas.compareAndSet(100, 200);
        System.out.println("CAS(100→200) 成功？" + ok + "，当前值 " + cas.get());

        // ② ABA：普通 CAS 感知不到中间变化，AtomicStampedReference 用版本号识破
        AtomicInteger naive = new AtomicInteger(100);
        naive.set(50);
        naive.set(100);                            // 值回来了（A→B→A）
        System.out.println("普通 CAS 以为没变过（成功替换）？" + naive.compareAndSet(100, 999));

        AtomicStampedReference<Integer> stamped = new AtomicStampedReference<>(100, 0);
        int oldStamp = stamped.getStamp();
        stamped.set(50, oldStamp + 1);
        stamped.set(100, oldStamp + 2);            // 值回来了，但版本号变了
        boolean caught = stamped.compareAndSet(100, 888, oldStamp, oldStamp); // 旧版本号 → 失败
        System.out.println("带版本号 CAS 识破 ABA（拒绝替换）？" + !caught
                + "，当前值 " + stamped.getReference() + "，版本号 " + stamped.getStamp());

        // ③ LongAdder：模拟设备消息计数（高并发写、偶尔读）
        LongAdder adder = new LongAdder();
        Thread[] ts = new Thread[8];
        long start = System.nanoTime();
        for (int i = 0; i < 8; i++) {
            ts[i] = new Thread(() -> { for (int j = 0; j < 1_000_000; j++) adder.increment(); });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        System.out.println("LongAdder 8 线程 × 100万 = " + adder.sum()
                + "，耗时 " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.race.AtomicDemo`
预期观察：普通 CAS 替换成功（ABA 未被察觉）、带版本号 CAS 失败（ABA 被识破）、LongAdder 总和恒为 8000000。

- [ ] **Step 3：附加实验**——把 ③ 换成 `AtomicLong.incrementAndGet()` 对比耗时（8 线程竞争同一个值的场景，LongAdder 通常快数倍）。此实验与任务 8.4 的"伪共享"原理呼应。

- [ ] **Step 4：思考题**
  1. CAS 自旋在"竞争极激烈"时有什么隐患？（提示：空转烧 CPU——这正是 LongAdder 分段要解决的）
  2. 设备在线数计数：用 AtomicLong 还是 LongAdder？依据是读写比还是数值精确性？

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m2): 2.5 CAS/原子类/LongAdder"
```

---

# 模块 3：显式锁（locks）

**模块目标：** 掌握 `java.util.concurrent.locks` 包：ReentrantLock 的完整能力（tryLock/可中断/公平锁）、死锁的诊断与预防、读写锁与 StampedLock、Condition 条件队列。

### 任务 3.1：ReentrantLock——tryLock 与可中断

**Files:**
- Create: `src/main/java/com/learn/concurrent/locks/ReentrantLockDemo.java`

**概念要点：**
- `synchronized` 拿不到锁就死等；`ReentrantLock` 提供三个增强：①`tryLock(timeout)` 抢不到就放弃（**快速失败**）；②`lockInterruptibly()` 等锁期间可被中断；③公平锁（按等待顺序发放，吞吐低，少用）。
- 铁律：`lock()` 后必须紧跟 `try{...} finally{ unlock() }`，且 unlock 必须在 finally——否则异常路径锁永不释放。
- 物联网场景：同一网关的指令下发需要串行化，但绝不能让线程池线程无限等锁——`tryLock(超时)` 是标准解。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    private final ReentrantLock lock = new ReentrantLock(); // 默认非公平
    private int counter = 0;

    public void safeIncrement() {
        lock.lock();
        try {
            counter++;
        } finally {
            lock.unlock();          // ★ 铁律：unlock 放 finally
        }
    }

    /** 模拟：同一网关指令需要串行，但 3 秒抢不到锁就快速失败，避免线程池被拖死 */
    public boolean sendCommandWithTimeout(String cmd) throws InterruptedException {
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            System.out.println("[" + Thread.currentThread().getName() + "] 抢锁超时，放弃指令 " + cmd);
            return false;
        }
        try {
            System.out.println("[" + Thread.currentThread().getName() + "] 获得锁，下发 " + cmd);
            TimeUnit.MILLISECONDS.sleep(200);   // 模拟网络 IO
            return true;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();

        Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.safeIncrement(); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) demo.safeIncrement(); });
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("ReentrantLock 计数：" + demo.counter);    // 恒为 200000

        demo.lock.lock();                       // main 先占住锁
        Thread worker = new Thread(() -> {
            try { demo.sendCommandWithTimeout("restart"); } catch (InterruptedException ignored) { }
        }, "cmd-sender");
        worker.start();
        worker.join();                          // 约 3 秒后 worker 因超时放弃
        demo.lock.unlock();
        System.out.println("main 释放锁，程序结束");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.locks.ReentrantLockDemo`
预期观察：计数恒 200000；约 3 秒后打印"抢锁超时，放弃指令 restart"——对照一下：如果用 `synchronized`，这里只能永远阻塞。

- [ ] **Step 3：思考题**
  1. 把 `tryLock(3, SECONDS)` 改成无参 `tryLock()`，行为差异是什么？
  2. 公平锁为什么慢？什么场景非公平锁会造成"某线程长期饥饿"？
  3. 若 `unlock()` 忘写 finally 里而是写在最后一行，什么情况下出事？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m3): 3.1 ReentrantLock 与 tryLock"
```

### 任务 3.2：死锁——制造、诊断、预防

**Files:**
- Create: `src/main/java/com/learn/concurrent/locks/DeadLockDemo.java`
- Create: `src/main/java/com/learn/concurrent/locks/DeadLockFixedDemo.java`

**概念要点：**
- 死锁四必要条件：互斥、持有并等待、不可剥夺、循环等待——破坏任意一条即可预防；**工程上最常用：破坏循环等待（全局锁排序）**。
- 诊断工具：`jps` 找 pid → `jstack <pid>` 输出里直接搜 `Found one Java-level deadlock`；或用 jConsole / IDEA（运行中点"Dump Threads"）/ fastthread.io。
- 物联网场景：跨网关联动指令（要同时锁 A、B 两台网关的发送通道）是死锁高发区。

- [ ] **Step 1：编写 DeadLockDemo.java**

```java
package com.learn.concurrent.locks;

public class DeadLockDemo {
    public static void main(String[] args) {
        final Object gatewayA = new Object();
        final Object gatewayB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (gatewayA) {
                sleep(100);                                  // 制造交错窗口
                System.out.println("T1：持有 A，想拿 B");
                synchronized (gatewayB) { System.out.println("T1：拿到 B"); }
            }
        }, "thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (gatewayB) {
                sleep(100);
                System.out.println("T2：持有 B，想拿 A");
                synchronized (gatewayA) { System.out.println("T2：拿到 A"); }
            }
        }, "thread-2");

        t1.start(); t2.start();
        System.out.println("程序卡死不退出。另开终端：jps 找 pid → jstack <pid> 看死锁报告");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

- [ ] **Step 2：运行并诊断（本任务的重点步骤）**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.locks.DeadLockDemo`
程序卡住不退出。另开终端执行：

```bash
jps -l                          # 找到 DeadLockDemo 进程的 pid
jstack <pid> | grep -A 5 "deadlock"   # Windows 下可先 jstack <pid> > dump.txt 再搜
```

预期观察：报告 `Found one Java-level deadlock`，两个线程都处于 BLOCKED，互相"waiting to lock"对方持有的锁。看懂这段报告是面试与实战双刚需。

- [ ] **Step 3：编写 DeadLockFixedDemo.java（锁排序修复）**

```java
package com.learn.concurrent.locks;

public class DeadLockFixedDemo {
    public static void main(String[] args) {
        final Object gatewayA = new Object();
        final Object gatewayB = new Object();

        // 修复：无论先操作哪台网关，都按全局固定顺序拿锁（这里用 identityHashCode 排序）
        Runnable crossGatewayCmd = () -> {
            Object first  = System.identityHashCode(gatewayA) < System.identityHashCode(gatewayB)
                            ? gatewayA : gatewayB;
            Object second = (first == gatewayA) ? gatewayB : gatewayA;
            synchronized (first) {
                sleep(100);
                synchronized (second) {
                    System.out.println("[" + Thread.currentThread().getName() + "] 完成 A+B 联动指令");
                }
            }
        };

        Thread t1 = new Thread(crossGatewayCmd, "thread-1");
        Thread t2 = new Thread(crossGatewayCmd, "thread-2");
        t1.start(); t2.start();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

- [ ] **Step 4：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.locks.DeadLockFixedDemo`
预期观察：两条联动指令都完成，程序正常退出。

- [ ] **Step 5：思考题**
  1. 除了锁排序，`tryLock(timeout)` + 失败后释放已持有的锁重来，为什么也能破坏死锁？
  2. 活锁与死锁的区别？（两个线程都"礼貌地"退让重试导致永远做不完——联想网关指令互相让路）

- [ ] **Step 6：提交**

```bash
git add . && git commit -m "learn(m3): 3.2 死锁诊断与预防"
```

### 任务 3.3：读写锁与 StampedLock

**Files:**
- Create: `src/main/java/com/learn/concurrent/locks/ReadWriteLockDemo.java`

**概念要点：**
- `ReentrantReadWriteLock`：读读共享、读写/写写互斥。适合**读多写少**：设备配置、元数据缓存。
- 写锁可降级为读锁（先写锁→再读锁→再放写锁）；读锁**不能**升级为写锁（会死锁）。
- `StampedLock`（JDK 8+）：额外提供**乐观读**——读时完全不加锁，读后 `validate` 校验期间有没有写发生，失败再退化为悲观读。吞吐最高；但**不可重入、不支持 Condition**，适合性能敏感的只读热点路径。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

public class ReadWriteLockDemo {
    // 场景：设备配置。读极频繁（每次指令下发都查），写极少（后台偶尔刷新）
    private final Map<String, String> config = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final StampedLock stampedLock = new StampedLock();

    public String read(String key) {
        rwLock.readLock().lock();
        try { return config.get(key); } finally { rwLock.readLock().unlock(); }
    }

    public void write(String key, String value) {
        rwLock.writeLock().lock();
        try { config.put(key, value); } finally { rwLock.writeLock().unlock(); }
    }

    /** StampedLock 乐观读：不加锁读 → 校验版本 → 没被写打扰才算数，否则退化为悲观读锁重读 */
    public String optimisticRead(String key) {
        long stamp = stampedLock.tryOptimisticRead();   // 非阻塞，拿版本戳
        String v = config.get(key);
        if (!stampedLock.validate(stamp)) {             // 读期间有写发生 → 作废
            stamp = stampedLock.readLock();
            try { v = config.get(key); } finally { stampedLock.unlockRead(stamp); }
        }
        return v;
    }

    public static void main(String[] args) throws InterruptedException {
        ReadWriteLockDemo demo = new ReadWriteLockDemo();
        demo.write("report.interval", "30s");
        demo.write("fw.version", "v1.4.2");

        Runnable reader = () -> {
            for (int i = 0; i < 3; i++) {
                System.out.println("[" + Thread.currentThread().getName() + "] 读取上报周期="
                        + demo.read("report.interval") + "，固件=" + demo.optimisticRead("fw.version"));
            }
        };
        Thread r1 = new Thread(reader, "reader-1");
        Thread r2 = new Thread(reader, "reader-2");   // 读锁共享：两读线程同时进入
        r1.start(); r2.start(); r1.join(); r2.join();
        System.out.println("两读线程交错输出（未互相阻塞）");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.locks.ReadWriteLockDemo`
预期观察：两个 reader 的输出交错出现（读锁共享）。附加实验：把 read() 里的锁换成写锁，输出会变成串行块。

- [ ] **Step 3：思考题**
  1. 配置读取场景若读的次数极少、写的次数多，读写锁还划算吗？（锁开销本身也是成本）
  2. StampedLock 乐观读期间另一线程写入了同一 key，validate 返回什么？代码如何自愈？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m3): 3.3 读写锁与 StampedLock"
```

### 任务 3.4：Condition——精确唤醒

**Files:**
- Create: `src/main/java/com/learn/concurrent/locks/ConditionBoundedBuffer.java`

**概念要点：**
- `wait/notify` 只有一个等待队列，`notify` 唤醒谁不可控；`Condition` 可在**一把锁上挂多个条件队列**（如 notFull / notEmpty），`signal` 精确唤醒对应条件的线程。
- 这正是 `ArrayBlockingQueue` 的内部实现方式；本任务亲手写一遍，以后再看 BlockingQueue 源码毫无压力。
- `await/signal` 必须在持有锁时调用，否则 `IllegalMonitorStateException`——与 wait/notify 一致。
- **等待一律用 `while` 条件判断，不用 `if`**：防止**虚假唤醒**和"被唤醒时条件又被别人抢先破坏"。

- [ ] **Step 1：编写代码（有界缓冲区，生产者=传感器，消费者=上报器）**

```java
package com.learn.concurrent.locks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** ReentrantLock + 两个 Condition 手写有界缓冲（ArrayBlockingQueue 的核心思想） */
public class ConditionBoundedBuffer<T> {
    private final Deque<T> queue = new ArrayDeque<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public ConditionBoundedBuffer(int capacity) { this.capacity = capacity; }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {   // ★ while 防虚假唤醒
                notFull.await();                 // 释放锁并等待；被 signal 后重新竞争锁再回来
            }
            queue.addLast(item);
            notEmpty.signal();                   // 精确唤醒"等非空"的线程
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            T item = queue.removeFirst();
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConditionBoundedBuffer<Integer> buffer = new ConditionBoundedBuffer<>(5);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                try { buffer.put(i); System.out.println("生产 " + i); } catch (InterruptedException e) { return; }
            }
        }, "sensor-producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                try { Thread.sleep(50); System.out.println("  消费 " + buffer.take()); } catch (InterruptedException e) { return; }
            }
        }, "report-consumer");

        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("=== 有界缓冲演示结束：生产 20 = 消费 20 ===");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.locks.ConditionBoundedBuffer`
预期观察：前 5 条快速"生产"，之后生产节奏被消费拖住（缓冲满了在等）——**这就是背压的雏形**；总数守恒。

- [ ] **Step 3：思考题**
  1. 把两个 `while` 改成 `if`，构造什么场景会出错？（多生产者多消费者 + notify 抢醒）
  2. 一个锁上的两个 Condition，和"两把锁各一个条件"有什么本质区别？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m3): 3.4 Condition 有界缓冲"
```

---

# 模块 4：线程协作（coordination）

**模块目标：** 掌握 JDK 内置的"线程间协调"工具：wait/notify 经典范式、四大同步工具（CountDownLatch/CyclicBarrier/Semaphore/Exchanger）、以及现代异步编排核心 CompletableFuture。

### 任务 4.1：wait/notify 经典范式——交替打印

**Files:**
- Create: `src/main/java/com/learn/concurrent/coordination/WaitNotifyDemo.java`

**概念要点：**
- `wait/notify/notifyAll` 是 Object 的方法，**必须在 synchronized 内调用**（锁哪个对象就等待在哪个对象上），否则抛 `IllegalMonitorStateException`。
- `notify` 随机唤醒一个，`notifyAll` 全部唤醒后竞争——多条件场景用 notifyAll + while 重判最稳。
- 经典练习"两线程交替打印 1~100"是面试高频题，也是理解"等待-通知"范式的最短路径。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.coordination;

public class WaitNotifyDemo {
    public static void main(String[] args) throws InterruptedException {
        // 反例：不持有 monitor 直接 wait → IllegalMonitorStateException
        Object bad = new Object();
        try {
            bad.wait();
        } catch (IllegalMonitorStateException e) {
            System.out.println("未持锁直接 wait 会抛：" + e.getClass().getSimpleName());
        }

        // 正例：奇偶两线程交替打印 1~100
        Object turnLock = new Object();
        int[] next = {1};                        // 数组以便 lambda 内修改
        Runnable oddJob = () -> printTurn(turnLock, next, true, "奇数线程");
        Runnable evenJob = () -> printTurn(turnLock, next, false, "偶数线程");

        Thread odd = new Thread(oddJob);
        Thread even = new Thread(evenJob);
        odd.start(); even.start();
        odd.join(); even.join();
        System.out.println("交替打印完成");
    }

    static void printTurn(Object lock, int[] next, boolean wantOdd, String name) {
        synchronized (lock) {
            while (next[0] <= 100) {
                boolean myTurn = (next[0] % 2 == 1) == wantOdd;
                if (myTurn) {
                    System.out.println(name + ": " + next[0]++);
                    lock.notifyAll();            // 打完叫醒对方
                } else {
                    try { lock.wait(); } catch (InterruptedException e) { return; }
                }
            }
            lock.notifyAll();                    // 唤醒可能还在等的对方，让其退出
        }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.coordination.WaitNotifyDemo`
预期观察：第一行打印异常名；随后 1~100 严格按奇偶交替打印。

- [ ] **Step 3：思考题**
  1. 把 `notifyAll` 换成 `notify`（本例只有两个线程）还能工作吗？如果三个线程呢？
  2. 用任务 3.4 的 Condition 重写交替打印，哪部分变简单了？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m4): 4.1 wait/notify 交替打印"
```

### 任务 4.2：四大同步工具

**Files:**
- Create: `src/main/java/com/learn/concurrent/coordination/SyncToolsDemo.java`

**概念要点（一张表记住）：**

| 工具 | 语义 | 可复用? | 物联网例子 |
|---|---|---|---|
| CountDownLatch | 等 N 个事件到齐再走 | 一次性 | 等全部传感器断开后重启网关 |
| CyclicBarrier | N 个线程互相等到齐，一起冲 | 可重复 | 多路采集线程按"轮"同步开始 |
| Semaphore | 限制同时进入的线程数 | 持续有效 | 限制对同一网关的并发指令数 |
| Exchanger | 两个线程交换一个对象 | 成对使用 | 采集线程与校准线程交换缓冲区 |

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.coordination;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class SyncToolsDemo {
    public static void main(String[] args) throws Exception {
        countDownLatchDemo();     // 一次性等待
        cyclicBarrierDemo();      // 可重复的"回合制"
        semaphoreDemo();          // 并发数限流
    }

    /** CountDownLatch：网关重启前，等待全部 5 个子设备断开连接 */
    static void countDownLatchDemo() throws InterruptedException {
        CountDownLatch allDisconnected = new CountDownLatch(5);
        for (int i = 1; i <= 5; i++) {
            String device = "sensor-" + i;
            new Thread(() -> {
                sleep((long) (Math.random() * 200));
                System.out.println(device + " 已断开");
                allDisconnected.countDown();          // 完成一个，计数减一
            }).start();
        }
        allDisconnected.await();                      // 阻塞到计数归零
        System.out.println("=== 全部设备已断开，网关可以重启 ===\n");
    }

    /** CyclicBarrier：3 路采集线程各自就绪后，同一时刻开始一轮采集（可多轮复用） */
    static void cyclicBarrierDemo() {
        CyclicBarrier ready = new CyclicBarrier(3,
                () -> System.out.println("--- 三路全部就绪，本轮采集开始 ---"));
        for (int i = 1; i <= 3; i++) {
            new Thread(() -> {
                try {
                    sleep((long) (Math.random() * 300));
                    System.out.println(Thread.currentThread().getName() + " 就绪");
                    ready.await();                     // 等其他两路
                    System.out.println(Thread.currentThread().getName() + " 开始采集");
                } catch (Exception ignored) { }
            }, "collector-" + i).start();
        }
        sleep(1000);
        System.out.println();
    }

    /** Semaphore：限制同时只允许 3 条指令发往同一网关 */
    static void semaphoreDemo() throws InterruptedException {
        Semaphore permits = new Semaphore(3);
        for (int i = 1; i <= 10; i++) {
            String cmd = "cmd-" + i;
            new Thread(() -> {
                try {
                    permits.acquire();                 // 拿许可（拿不到就等）
                    System.out.println("下发 " + cmd);
                    sleep(200);                        // 模拟指令耗时
                    permits.release();                 // 归还许可
                } catch (InterruptedException ignored) { }
            }).start();
            sleep(20);                                 // 错开启动，便于观察
        }
        sleep(1500);
        System.out.println("\n=== 任意时刻最多 3 条指令在途 ===");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.coordination.SyncToolsDemo`
预期观察：①断开顺序随机但"可以重启"一定在最后；②"本轮采集开始"一定在三路就绪之后；③指令以 3 条为一波释放。

- [ ] **Step 3：思考题**
  1. CountDownLatch 为什么设计成一次性？"等待方"和"到达方"调用的方法分别是什么？
  2. Semaphore(1) 等价于什么？和 `synchronized` 差在哪？（提示：可跨方法释放、tryAcquire）
  3. Exchanger 用在什么设备数据处理场景合适？（提示：双缓冲）

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m4): 4.2 同步工具三件套"
```

### 任务 4.3：CompletableFuture——异步编排（现代后端必会）

**Files:**
- Create: `src/main/java/com/learn/concurrent/coordination/CompletableFutureDemo.java`

**概念要点：**
- Future 的升级版：支持**回调链**（不阻塞取结果）、**组合**（thenCombine/thenCompose）、**聚合**（allOf/anyOf）、**异常处理**（exceptionally/handle）、**超时**（orTimeout，JDK 9+）。
- 常用链式 API：`supplyAsync`（提交异步任务）→ `thenApply`（转换）→ `thenAccept`（消费）→ `thenCombine`（合并两路）→ `allOf`（等多路）。
- **必须显式传自定义线程池**：默认用 `ForkJoinPool.commonPool()`（CPU 核数-1），跑 I/O 任务会把公共池占满，殃及所有并行流——生产事故高发点。
- 物联网核心场景：**批量指令下发的 scatter-gather**——并行发给 N 台设备，单台超时/失败不影响整体，最后聚合结果。本任务 Demo 完整实现它。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.coordination;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class CompletableFutureDemo {
    public static void main(String[] args) {
        // 零、热身：一个最小的异步链
        CompletableFuture.supplyAsync(() -> "raw:26.5C")
                .thenApply(s -> s.split(":")[1])       // 转换
                .thenAccept(v -> System.out.println("解析温度: " + v))
                .join();                               // 仅为了让 main 等它打印

        // 一、IoT scatter-gather：并行查询 8 台设备，单台超时不影响整体
        ExecutorService pool = new ThreadPoolExecutor(
                4, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r, "cmd-io-" + tId());
                    t.setDaemon(true);
                    return t;
                });

        List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, 8)
                .mapToObj(id -> CompletableFuture
                        .supplyAsync(() -> queryDevice(id), pool)
                        .orTimeout(2, TimeUnit.SECONDS)                       // 单台最多等 2s
                        .exceptionally(e -> "device-" + id + " 查询失败（"
                                + e.getClass().getSimpleName() + "）"))      // 单台失败兜底
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))     // 等全部完成
                .thenRun(() -> {
                    System.out.println("--- 聚合结果 ---");
                    futures.forEach(f -> System.out.println(f.join()));
                })
                .join();

        pool.shutdown();
    }

    /** 模拟设备查询：device-5、device-6 网络差会超时 */
    static String queryDevice(int id) {
        sleep(id == 5 || id == 6 ? 5000 : 300);
        return "device-" + id + " 在线，电量 " + (60 + id) + "%";
    }

    static long tId() { return Thread.currentThread().getId(); }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.coordination.CompletableFutureDemo`
预期观察：第一行打印"解析温度: 26.5C"；约 2 秒后输出 8 台设备结果，其中 device-5/6 显示查询失败（CompletionException 即超时），**其余 6 台正常**——整体没有被拖到 5 秒。

- [ ] **Step 3：思考题**
  1. 去掉 `exceptionally`，device-5 超时后 `allOf().join()` 会发生什么？（整体异常，其余结果全丢）
  2. `thenApply` vs `thenCompose` 的区别？（map vs flatMap）
  3. 为什么不推荐 `supplyAsync` 用默认线程池做 I/O？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m4): 4.3 CompletableFuture 批量指令下发"
```

---

# 模块 5：并发容器（collections）

**模块目标：** 掌握三大类并发容器，能针对场景选型：ConcurrentHashMap（共享状态）、BlockingQueue（线程间传递）、CopyOnWriteArrayList（读多写极少的列表）。**后端代码 80% 的并发问题，选对容器就消灭了。**

### 任务 5.1：ConcurrentHashMap

**Files:**
- Create: `src/main/java/com/learn/concurrent/collections/ConcurrentHashMapDemo.java`

**概念要点：**
- 物联网后端最核心的数据结构之一：**设备注册表/会话表**（deviceId → session）。
- **禁止 null** key/value（并发下"get 到 null"无法区分"不存在"和"值就是 null"）。
- 关键是**原子复合操作**：`if (map.get(k)==null) map.put(k,v)` 两步之间存在竞态，必须用 `putIfAbsent` / `computeIfAbsent` / `compute` / `merge` 一步完成。
- 迭代器**弱一致**：不抛 `ConcurrentModificationException`，但不保证看到迭代期间的修改。
- `size()` 是估算（内部 CounterCell 分段统计）；内部实现（CAS + 桶级 synchronized）见任务 8.2。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.collections;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ConcurrentHashMapDemo {
    // 场景：设备会话注册表（服务里全局一份，所有请求线程并发读写）
    static final ConcurrentHashMap<String, String> DEVICE_SESSIONS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        // ① 原子复合操作：不存在才创建（会话首次注册的标准写法）
        DEVICE_SESSIONS.computeIfAbsent("device-001", id -> "session-" + id);
        System.out.println("首次注册后 size=" + DEVICE_SESSIONS.size());
        DEVICE_SESSIONS.computeIfAbsent("device-001", id -> "session-NEW"); // 已存在，loader 不执行
        System.out.println("重复注册不会覆盖: " + DEVICE_SESSIONS.get("device-001"));

        // ② 20 线程并发注册 200 台（每线程负责 10 台，无交集）
        CountDownLatch done = new CountDownLatch(20);
        for (int t = 0; t < 20; t++) {
            int base = t * 10;
            new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    String id = "device-" + (100 + base + i);
                    DEVICE_SESSIONS.computeIfAbsent(id, k -> "session-" + k);
                }
                done.countDown();
            }).start();
        }
        done.await();
        System.out.println("并发注册后 size（期望 201）= " + DEVICE_SESSIONS.size());

        // ③ merge：按消息类型统计接收量（物联网指标统计常用）
        ConcurrentHashMap<String, Long> msgCount = new ConcurrentHashMap<>();
        List<String> incoming = List.of("telemetry", "telemetry", "event", "telemetry", "event");
        incoming.forEach(type -> msgCount.merge(type, 1L, Long::sum));  // 无则置 1，有则累加
        System.out.println("消息计数: " + msgCount);

        // ④ 弱一致迭代：迭代中并发修改不抛异常
        for (var e : DEVICE_SESSIONS.entrySet()) {
            DEVICE_SESSIONS.remove("device-100");      // 迭代中删除
            break;                                      // 只演示一轮
        }
        System.out.println("迭代中并发修改未抛异常，device-100 已被移除: "
                + !DEVICE_SESSIONS.containsKey("device-100"));
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.collections.ConcurrentHashMapDemo`
预期观察：size 恒为 201；重复注册不覆盖；消息计数 {telemetry=3, event=2}；迭代不抛异常。

- [ ] **Step 3：附加实验（对照普通 HashMap）**——把容器换成 `HashMap` 跑 ②（模拟"图省事用了 HashMap"），多跑几次观察 size 偏差甚至 JDK8 下 size 基本正确但元素错乱（JDK7 的经典死循环问题可只做资料了解）。

- [ ] **Step 4：思考题**
  1. `computeIfAbsent` 的 loading 函数在别的线程看来原子吗？它执行时会锁住什么？（引出任务 8.2）
  2. 设备"最后在线时间"更新用 `put` 还是 `merge` 还是 `compute`？

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m5): 5.1 ConcurrentHashMap"
```

### 任务 5.2：BlockingQueue 家族——数据管道与背压

**Files:**
- Create: `src/main/java/com/learn/concurrent/collections/BlockingQueueDemo.java`

**概念要点（四组 API 语义必须背熟）：**

| | 抛异常 | 返回特殊值 | 一直阻塞 | 超时放弃 |
|---|---|---|---|---|
| 入队 | add | offer(e) | **put** | offer(e, t, u) |
| 出队 | remove | **poll** | **take** | poll(t, u) |

实现类选型：`ArrayBlockingQueue`（有界、数组、单锁——生产常用）、`LinkedBlockingQueue`（默认**无界**！必须显式设容量）、`SynchronousQueue`（零容量、直接交接，`newCachedThreadPool` 用它）、`PriorityBlockingQueue`（按优先级出队，如告警分级处理）、`DelayQueue`（到期才能取，如延迟重试）。
有界队列满时 `put` 阻塞 = **背压（backpressure）**——采集快于上报时向采集端反向施压，这是数据管道设计的第一原则。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.collections;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueDemo {
    public static void main(String[] args) throws Exception {
        boundedQueueBackpressure();
        delayQueueRetry();
    }

    /** 有界队列背压：采集（快）→ 缓冲(5) → 上报（慢），缓冲满则采集被拖慢，不丢数据 */
    static void boundedQueueBackpressure() throws Exception {
        BlockingQueue<Integer> buffer = new LinkedBlockingQueue<>(5);

        Thread sensor = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    buffer.put(i);                  // 满 5 条后在这里等待 = 背压
                    System.out.println("采集 " + i + "，缓冲=" + buffer.size());
                }
                buffer.put(-1);                    // 毒丸：通知上报线程结束
            } catch (InterruptedException ignored) { }
        }, "sensor");

        Thread uploader = new Thread(() -> {
            try {
                int v;
                while ((v = buffer.take()) != -1) {
                    Thread.sleep(50);               // 模拟慢速上报
                    System.out.println("  上报 " + v);
                }
            } catch (InterruptedException ignored) { }
        }, "uploader");

        sensor.start(); uploader.start();
        sensor.join(); uploader.join();
        System.out.println("=== 有界缓冲背压完成 ===\n");
    }

    /** DelayQueue：失败指令延迟重试——元素到期才能被 take */
    static void delayQueueRetry() throws Exception {
        class RetryTask implements Delayed {
            final String cmd;
            final long dueAt;   // 到期时刻（纳秒）
            RetryTask(String cmd, long delayMs) {
                this.cmd = cmd;
                this.dueAt = System.nanoTime() + delayMs * 1_000_000L;
            }
            @Override public long getDelay(TimeUnit unit) {
                return unit.convert(dueAt - System.nanoTime(), TimeUnit.NANOSECONDS);
            }
            @Override public int compareTo(Delayed other) {
                return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
            }
        }

        DelayQueue<RetryTask> retries = new DelayQueue<>();
        retries.put(new RetryTask("校时指令", 200));
        retries.put(new RetryTask("重启指令", 500));     // 后入队但更晚到期
        System.out.println("指令入队，等待到期重试...");
        for (int i = 0; i < 2; i++) {
            RetryTask t = retries.take();               // 阻塞到最早到期的任务可取
            System.out.println("重试：" + t.cmd);
        }
        System.out.println("=== DelayQueue 完成 ===");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.collections.BlockingQueueDemo`
预期观察：前 5 条"采集"迅速输出，之后采集节奏与上报一致（缓冲在 5 附近）；DelayQueue 按到期时间顺序（先"校时"后"重启"）唤醒，而不是入队顺序。

- [ ] **Step 3：思考题**
  1. `LinkedBlockingQueue` 不传容量会怎样？为什么生产环境是隐患？（无界 → OOM）
  2. 设备告警要按"紧急>重要>一般"处理，选哪个队列？元素需要实现什么接口？
  3. 毒丸（poison pill）方案有什么局限？（多消费者要放 N 颗；对照任务 6.2 的优雅关闭）

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m5): 5.2 BlockingQueue 与背压"
```

### 任务 5.3：CopyOnWriteArrayList 与无锁队列

**Files:**
- Create: `src/main/java/com/learn/concurrent/collections/CopyOnWriteDemo.java`

**概念要点：**
- COW：写时复制整个底层数组，读不加锁且**遍历的是快照**——遍历期间的写互不可见，绝不抛 `ConcurrentModificationException`。
- 适用：**读多写极少**且遍历远多于修改——事件监听器列表、路由/规则表。写频繁则每次复制整数组，性能崩塌。
- `ConcurrentLinkedQueue`：CAS 无锁链表队列，非阻塞；`size()` 要遍历全链（O(n)，别在热路径调）。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteDemo {
    // 场景：设备数据监听器注册表。注册/注销极少，数据到达时的遍历通知极频繁
    private static final List<String> LISTENERS = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        LISTENERS.add("告警服务");
        LISTENERS.add("时序库写入器");

        // 遍历期间注册新监听器：不抛异常，且新监听器不会"插进"本轮遍历
        for (String listener : LISTENERS) {
            System.out.println("通知 " + listener);
            LISTENERS.add("数据转发服务");          // 写时复制，遍历不受影响
        }
        System.out.println("遍历完成（只通知了快照里的 2 个），最终注册数=" + LISTENERS.size() + "\n");

        // 对照实验：普通 ArrayList 边遍历边加 → ConcurrentModificationException
        List<String> naive = new ArrayList<>(List.of("a", "b"));
        try {
            for (String s : naive) {
                naive.add("c");
            }
        } catch (Exception e) {
            System.out.println("ArrayList 遍历中修改: " + e.getClass().getSimpleName());
        }

        // ConcurrentLinkedQueue：无锁队列，适合高并发生产-单消费
        ConcurrentLinkedQueue<Integer> metrics = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 3; i++) metrics.offer(i);
        System.out.println("无锁队列 poll=" + metrics.poll() + "，剩余 " + metrics.size());
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.collections.CopyOnWriteDemo`
预期观察：遍历只打印 **2 次**通知（遍历的是开局快照）；`ArrayList` 对照组抛 `ConcurrentModificationException`；COW 最终 size=**4**——循环体每轮各 add 一次"数据转发服务"（2 次）+ 初始 2 个 = 4。运行前先自己算一遍这个 4，算错说明快照语义还没吃透。

- [ ] **Step 3：思考题**
  1. 什么场景 COW 是灾难？（监听器频繁上下线）
  2. 读多写少的"设备元数据缓存列表"用 COW 还是读写锁保护的 ArrayList？各给一个理由。

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m5): 5.3 COW 与无锁队列"
```

---

# 模块 6：线程池（pool）

**模块目标：** 线程池是后端工程师日常接触最多的并发组件。掌握 ThreadPoolExecutor 的参数与执行流程、自定义与监控、优雅关闭、定时任务、ForkJoin，以及 Java 21 虚拟线程对 I/O 密集物联网服务的意义。

### 任务 6.1：ThreadPoolExecutor——参数与执行流程

**Files:**
- Create: `src/main/java/com/learn/concurrent/pool/ThreadPoolExecutorDemo.java`

**概念要点：**
- 七大参数：corePoolSize、maximumPoolSize、keepAliveTime+unit、workQueue、threadFactory、rejectedExecutionHandler。
- **执行流程（必背）**：来了任务 → ①核心线程未满：建核心线程；②核心满：入队列；③队列满：建非核心线程（直到 max）；④队列满且 max 满：**触发拒绝策略**。（注意：是"先入队再扩容"，不是"先扩到 max 再入队"——高频认知误区）
- 四种拒绝策略：`AbortPolicy`（抛异常，默认）/ `CallerRunsPolicy`（提交者自己跑，天然限速）/ `DiscardPolicy`（静默丢）/ `DiscardOldestPolicy`（丢队首最老的）。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorDemo {
    public static void main(String[] args) throws InterruptedException {
        // core=2, max=4, 队列容量=10
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        for (int i = 1; i <= 8; i++) {
            pool.execute(() -> {
                System.out.printf("[%s] %s，pool=%d active=%d queue=%d%n",
                        Thread.currentThread().getName(),
                "处理设备消息", pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size());
                sleep(300);    // 模拟耗时任务
            });
        }

        Thread.sleep(2000);
        System.out.println("poolSize 最终=" + pool.getPoolSize());   // 空闲回收后回落
        pool.shutdown();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.pool.ThreadPoolExecutorDemo`
预期观察：只有 2 个线程名在跑（pool=2），其余任务在 queue 里排队——**因为队列容量 10 足够装下 8 个任务，永远轮不到扩容到 max**。

- [ ] **Step 3：两个参数实验（各改一处再运行，把结论记进笔记）**
  - 实验 A：任务数改为 16（> core2 + queue10 = 12）→ 观察第 13~16 个任务出现时 pool 变为 4（扩到 max）。
  - 实验 B：任务数改为 17 → 触发拒绝策略抛 `RejectedExecutionException`（默认 AbortPolicy）。再换 `CallerRunsPolicy` 观察 main 线程亲自执行任务（输出里出现 `[main]`）。

- [ ] **Step 4：思考题**
  1. 为什么"先入队再扩容"的设计对**短平快任务**更省资源？什么场景这个设计反而害人？（提示：任务慢 + 队列长 → max 永远不触发，延迟越积越多）
  2. 网关指令下发池：队列应该有界还是无界？拒绝策略选哪个？说出理由。

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m6): 6.1 线程池参数与执行流程"
```

### 任务 6.2：自定义线程池——命名、监控、优雅关闭

**Files:**
- Create: `src/main/java/com/learn/concurrent/pool/CustomThreadPoolDemo.java`

**概念要点：**
- **阿里 Java 开发手册强制**：不用 `Executors.newFixedThreadPool/newSingleThreadExecutor/newCachedThreadPool/newScheduledThreadPool` 创建线程池，而是 `new ThreadPoolExecutor(...)` 显式指定。原因：①Fixed/Single 用无界 `LinkedBlockingQueue` → 任务堆积 OOM；②Cached 用 `SynchronousQueue` + max=∞ → 线程暴涨 OOM。
- 三件生产标配：**命名 ThreadFactory**（排障时 jstack 能看懂）、**监控**（activeCount/queue size/completedTaskCount 定时上报）、**优雅关闭**（`shutdown()` 拒新 → `awaitTermination(timeout)` 等旧任务 → 超时 `shutdownNow()` 中断剩余 → 兜底再取消）。
- 池大小经验值：**CPU 密集 ≈ N+1；I/O 密集 ≈ N × (1 + 等待时间/计算时间)**（N=核数，最终以压测为准）。
- **线程池隔离**：指令下发、数据处理、通知推送各用独立池——避免慢任务占满公共池"雪崩串联"（舱壁模式）。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.pool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolDemo {
    public static void main(String[] args) throws InterruptedException {
        // ① 命名工厂 + 未捕获异常兜底
        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory namedFactory = r -> {
            Thread t = new Thread(r, "iot-worker-" + seq.getAndIncrement());
            t.setUncaughtExceptionHandler((thread, e) ->
                    System.out.println("[" + thread.getName() + "] 未捕获异常: " + e));
            return t;
        };

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                4, 4, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000), namedFactory);

        // ② 监控：每 500ms 打一次核心指标（生产中改为上报到监控系统）
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pool-monitor");
            t.setDaemon(true);
            return t;
        });
        monitor.scheduleAtFixedRate(() -> System.out.printf("[监控] active=%d queue=%d completed=%d%n",
                pool.getActiveCount(), pool.getQueue().size(), pool.getCompletedTaskCount()),
                0, 500, TimeUnit.MILLISECONDS);

        // ③ 业务任务
        for (int i = 1; i <= 20; i++) {
            pool.execute(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] 处理一条设备消息");
                sleep(200);
            });
        }

        // ④ 优雅关闭三步曲
        pool.shutdown();                                      // 拒新，等旧
        boolean graceful = pool.awaitTermination(5, TimeUnit.SECONDS);
        if (!graceful) {
            pool.shutdownNow();                               // 超时：中断在跑的任务
        }
        monitor.shutdownNow();
        System.out.println("线程池已" + (graceful ? "优雅" : "强制") + "关闭");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.pool.CustomThreadPoolDemo`
预期观察：任务线程名都是 `iot-worker-N`；监控行先于/穿插于任务输出；结束打印"优雅关闭"。
实验：把任务耗时 `sleep(200)` 改成 `sleep(3000)`，awaitTermination(5s) 内跑不完 20 条 → 输出"强制关闭"，且被中断的任务线程收到中断（可加 catch 打印验证）。

- [ ] **Step 3：思考题**
  1. `shutdown()` 与 `shutdownNow()` 分别对"队列中未执行的任务"做什么？（前者照常执行完；后者返回未执行列表并中断在跑线程）
  2. 你的物联网服务有"指令下发（慢，要等设备 ACK）"和"指标统计（快）"两类任务，池怎么设计？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m6): 6.2 自定义线程池与优雅关闭"
```

### 任务 6.3：ScheduledThreadPoolExecutor 定时任务

**Files:**
- Create: `src/main/java/com/learn/concurrent/pool/ScheduledTaskDemo.java`

**概念要点：**
- `scheduleAtFixedRate`：**固定频率**（上次开始时间 + 周期；任务比周期长时会"追"或顺延）；`scheduleWithFixedDelay`：**上次结束 + 延迟**。一句话：**rate 看开始，delay 看结束**。
- **头号大坑**：任务抛出未捕获异常后，**后续所有执行被静默取消**——定时心跳任务挂了却毫无报错，是物联网服务的经典事故。
- 定时任务抛异常必须 try-catch 全包；重抛/上报需另行处理。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.pool;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskDemo {
    public static void main(String[] args) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "iot-scheduler");
            t.setDaemon(true);
            return t;
        });

        // ① 正常的心跳扫描任务
        scheduler.scheduleAtFixedRate(() ->
                System.out.println("[心跳扫描] " + LocalTime.now()), 0, 1, TimeUnit.SECONDS);

        // ② 埋雷任务：第 1 次执行就抛异常 → 之后被静默取消！
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[危险任务] 即将抛出异常...");
            throw new RuntimeException("设备连接中断");
        }, 1, 1, TimeUnit.SECONDS);

        Thread.sleep(5000);
        System.out.println("main 结束。注意：危险任务只出现过一次，之后无声无息地消失了");
        scheduler.shutdownNow();
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.pool.ScheduledTaskDemo`
预期观察：心跳扫描每秒一条，持续输出；"危险任务"只出现 **1 次**，之后再也不见，且没有任何报错——坑已复现。

- [ ] **Step 3：修复实验**——把危险任务包进 try-catch（catch 里打印并吞掉/上报），再次运行：任务每秒稳定执行。体会"定时任务方法体必须自吞异常"。

- [ ] **Step 4：思考题**
  1. 心跳超时扫描用 fixedRate 还是 fixedDelay？如果扫描本身可能超过周期呢？
  2. 用任务 5.2 的 DelayQueue 也能做延迟重试，与 ScheduledExecutor 的取舍是什么？（前者在队列/任务复杂时更灵活，后者是标准工具）

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m6): 6.3 定时任务与异常大坑"
```

### 任务 6.4：ForkJoin 与并行流

**Files:**
- Create: `src/main/java/com/learn/concurrent/pool/ForkJoinDemo.java`

**概念要点：**
- ForkJoinPool：**分治（fork/join）+ 工作窃取**（空闲线程从别的线程队列尾偷任务），专为 CPU 密集型计算设计。
- 并行流 `parallelStream()` 底层就是公共 `ForkJoinPool.commonPool()`——**铁律：不要在并行流里做阻塞 I/O**（会把全 JVM 共用的池占满，殃及所有 parallelStream 与默认 CompletableFuture）。
- 物联网场景：对海量历史遥测数据做聚合分析（均值、分位数）这类纯计算适合；调设备接口、查库这类 I/O 严禁。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.pool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.LongStream;

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
                for (int i = from; i < to; i++) sum += numbers[i];
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
        for (long n : numbers) serial += n;
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
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.pool.ForkJoinDemo`
预期观察：三种结果一致；多核机器上 ForkJoin/并行流明显快于串行。改小数组（如 10 万元素）再看——小任务并行开销反超收益，可能更慢。

- [ ] **Step 3：思考题**
  1. 为什么 `left.fork(); right.compute()` 比 `left.fork(); right.fork(); ... left.join(); right.join()` 好？（提示：减少一次挂起/唤醒，当前线程不闲着）
  2. `parallelStream` 里调 `Thread.sleep(5000)` 模拟 I/O，会发生什么灾难？（引出任务 4.3 的"必须自定义池"）

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m6): 6.4 ForkJoin 与并行流"
```

### 任务 6.5：Java 21 虚拟线程（JDK 21 正式特性，JEP 444）

**Files:**
- Create: `src/main/java/com/learn/concurrent/pool/VirtualThreadDemo.java`

**概念要点：**
- 平台线程 1:1 映射内核线程（栈默认 ~1MB，创建数千个就很吃力）；**虚拟线程**由 JVM 调度、挂载在少量载体（carrier）平台线程上，**阻塞在 I/O 时自动卸载**（unmount），百万级轻松创建。
- 一句话定位：**I/O 密集服务的吞吐银弹，CPU 密集无收益**。物联网后端（每设备一个连接/任务、大量等待设备响应）是最典型的受益场景——很多"每设备一线程"的简单编程模型重新变得可行。
- 用法：`Thread.ofVirtual().start(...)`、`Executors.newVirtualThreadPerTaskExecutor()`（每任务一线程，不再池化！）。`Thread.isVirtual()` 判别。
- 注意事项：①不要池化虚拟线程（用完即弃）；②Java 21 中在 `synchronized` 块内阻塞会导致**钉住（pinning）**，高并发场景换 `ReentrantLock`（JDK 24+ 已优化此问题，了解即可）；③ThreadLocal 滥用在百万线程下放大内存开销（新方向是 ScopedValue，了解即可）。
- 与 Netty 的关系（引出模块 9）：Netty 用"少量线程+事件循环+回调"解决同样的问题；虚拟线程让你用**同步写法**获得接近的吞吐——两者是当前 I/O 高并发的两大主流路线。

- [ ] **Step 1：编写代码**

```java
package com.learn.concurrent.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadDemo {
    public static void main(String[] args) throws Exception {
        // ① 轻松创建 10 万个"每设备一个"的阻塞式任务（平台线程这么做会 OOM）
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 100_000; i++) {
                int id = i;
                pool.submit(() -> {
                    if (id <= 3) {
                        System.out.println("虚拟任务 #" + id + "，运行于 " + Thread.currentThread());
                    }
                    TimeUnit.MILLISECONDS.sleep(100);    // 模拟等待设备响应（I/O 阻塞点→自动卸载）
                    return null;
                });
            }
        }   // try-with-resources：自动等待全部任务完成后关闭
        System.out.println("10 万个虚拟任务全部完成，总耗时 "
                + (System.nanoTime() - start) / 1_000_000 + " ms（若并发上限 8 核，也应远小于 10000s）");

        // ② 直接创建虚拟线程
        Thread vt = Thread.ofVirtual().name("device-keepalive").start(() ->
                System.out.println("[" + Thread.currentThread().getName() + "] 保活任务启动，isVirtual="
                        + Thread.currentThread().isVirtual()));
        vt.join();
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.pool.VirtualThreadDemo`
预期观察：前 3 个任务的载体线程名（ForkJoinPool worker）；10 万任务约几百毫秒~1 秒级完成（sleep 100ms 只是"挂起"而非占着内核线程）。

- [ ] **Step 3：对照实验**——把 `newVirtualThreadPerTaskExecutor()` 换成 `Executors.newFixedThreadPool(100_000)`（平台线程）再跑：多半直接 `OutOfMemoryError: unable to create native thread`——一跑就懂两者量级差异。

- [ ] **Step 4：思考题**
  1. 虚拟线程能替你解决"共享状态竞争"吗？（不能——并发正确性问题依然存在，10 万线程写同一个 map 依旧要加锁/用并发容器）
  2. 老代码里"用回调/Netty 异步改造设备连接处理"的项目，迁到 Java 21 会怎么简化？

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m6): 6.5 虚拟线程"
```

---

# 模块 7：物联网场景实战（scenarios）

**模块目标：** 综合运用模块 1~6 的知识，独立实现 5 个物联网后端的高频组件。**本模块以练习为主**：每个任务给出完整骨架（可直接编译运行，核心方法抛 `UnsupportedOperationException` 留给你实现）、实现提示与验收标准。先不看提示自己设计一遍，再对照提示修正。

### 任务 7.1：设备会话管理器（ConcurrentHashMap + 定时扫描）

**Files:**
- Create: `src/main/java/com/learn/concurrent/scenarios/DeviceSessionManager.java`

**场景描述：** 设备 TCP/MQTT 接入后注册会话，周期上报心跳；服务需要支持查询在线状态，并自动把"超过 15 秒没有心跳"的设备判为离线并移除。这是所有设备接入服务的标配组件。

**骨架（创建文件后补齐 TODO 方法体）：**

```java
package com.learn.concurrent.scenarios;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceSessionManager {
    static class DeviceSession {
        final String deviceId;
        volatile long lastActiveAt;      // 心跳时间戳（毫秒）
        DeviceSession(String deviceId) {
            this.deviceId = deviceId;
            this.lastActiveAt = System.currentTimeMillis();
        }
    }

    private final ConcurrentHashMap<String, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scanner;
    private final long evictAfterMillis;   // 超过该时长无心跳 → 判离线

    public DeviceSessionManager() {
        this(15_000, 5_000);               // 生产默认：15s 超时 / 5s 扫描
    }

    public DeviceSessionManager(long evictAfterMillis, long scanPeriodMillis) {
        this.evictAfterMillis = evictAfterMillis;
        // 后台扫描线程（daemon；任务体内必须 try-catch —— 任务 6.3 的坑！）
        scanner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-scanner");
            t.setDaemon(true);
            return t;
        });
        scanner.scheduleAtFixedRate(this::evictTimeoutSessions,
                scanPeriodMillis, scanPeriodMillis, TimeUnit.MILLISECONDS);
    }

    /** 注册设备（已存在则不覆盖，只刷新心跳） */
    public void register(String deviceId) {
        // TODO(实现)：computeIfAbsent 注册新会话；已存在时刷新 lastActiveAt
        throw new UnsupportedOperationException("任务 7.1：实现 register");
    }

    /** 心跳：刷新设备的 lastActiveAt；设备不在线返回 false */
    public boolean heartbeat(String deviceId) {
        // TODO(实现)：提示 —— computeIfPresent(deviceId, (k, s) -> { s.lastActiveAt = now; return s; }) != null
        throw new UnsupportedOperationException("任务 7.1：实现 heartbeat");
    }

    /** 注销设备 */
    public void unregister(String deviceId) {
        // TODO(实现)
        throw new UnsupportedOperationException("任务 7.1：实现 unregister");
    }

    /** 扫描并移除超时会话（超过 evictAfterMillis 未心跳 → 打印下线日志并移除） */
    void evictTimeoutSessions() {
        // TODO(实现)：遍历 sessions，now - lastActiveAt > evictAfterMillis 的 remove 并打印 "[下线] xxx"
        // 思考：remove 前要不要再校验一次时间戳？（提示：遍历时设备恰好心跳了 —— 竞态！）
        throw new UnsupportedOperationException("任务 7.1：实现 evictTimeoutSessions");
    }

    public int onlineCount() { return sessions.size(); }
    public boolean isOnline(String deviceId) { return sessions.containsKey(deviceId); }

    /** 模拟验收（main 自带，实现完上面四个方法后直接运行） */
    public static void main(String[] args) throws Exception {
        DeviceSessionManager mgr = new DeviceSessionManager();

        // 验收 1：单线程注册 100 台
        for (int i = 1; i <= 100; i++) mgr.register("device-" + i);
        System.out.println("验收1 在线数（期望100）= " + mgr.onlineCount());

        // 验收 2：10 线程并发注册 1000 台（每个 id 只注册一次）
        Thread[] ts = new Thread[10];
        for (int t = 0; t < 10; t++) {
            int base = t * 100;
            ts[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++) mgr.register("d-" + (base + i));
            });
        }
        for (Thread x : ts) x.start();
        for (Thread x : ts) x.join();
        System.out.println("验收2 总在线（期望1100）= " + mgr.onlineCount());

        // 验收 3：超时剔除（用短超时配置 3s/1s，独立实例，确定性验证）
        DeviceSessionManager fast = new DeviceSessionManager(3_000, 1_000);
        for (int i = 1; i <= 5; i++) fast.register("fd-" + i);
        Thread heartbeater = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                fast.heartbeat("fd-1");               // 唯一持续心跳的设备
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            }
        });
        heartbeater.setDaemon(true);
        heartbeater.start();
        Thread.sleep(5_000);                          // 等扫描跑几轮
        System.out.println("验收3 fd-1 存活（期望true）= " + fast.isOnline("fd-1")
                + "，总在线（期望1）= " + fast.onlineCount());
        heartbeater.interrupt();
    }
}
```

**验收标准：** ①验收 1 输出 100；②验收 2 输出 1100（多跑 10 次全对）；③验收 3 中 `[下线] fd-2~fd-5` 日志出现、fd-1 存活、总数为 1；④整个 main 期间无异常（扫描线程的 try-catch 生效）。

- [ ] Step 1: 复制骨架到文件，实现 4 个方法
- [ ] Step 2: 运行 main，通过全部验收
- [ ] Step 3: 思考题——`evictTimeoutSessions` 里"遍历时 remove"安全吗？`keySet().removeIf(...)` 与 `entrySet().iterator().remove()` 与 `map.remove(k, v)` 两参版本哪个能避免误删刚心跳的设备？（两参 remove = CAS 语义）
- [ ] Step 4: 提交 `git add . && git commit -m "learn(m7): 7.1 设备会话管理器"`

### 任务 7.2：传感器数据管道（生产者-消费者 + 批量聚合）

**Files:**
- Create: `src/main/java/com/learn/concurrent/scenarios/SensorDataPipeline.java`

**场景描述：** 3 个采集线程持续产生温度数据（模拟快生产），1 个上报线程聚合：**攒满 100 条或等满 3 秒**就"批量上报"。要求无丢失、有界缓冲、可优雅停机。这是遥测上报、日志批量落库的通用模型。

**骨架：**

```java
package com.learn.concurrent.scenarios;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SensorDataPipeline {
    private final LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>(1_000);
    private final AtomicLong produced = new AtomicLong();   // 用于验收：无丢失
    private final AtomicLong consumed = new AtomicLong();
    private volatile boolean running = true;                 // 状态标志（任务 2.3 的用法）

    /** 采集线程调用：入队，满则等待（背压），running=false 时停止 */
    void collect(int reading) throws InterruptedException {
        // TODO(实现)：入队并 produced.incrementAndGet()；提示：先判断 running，用 offer(超时) 或 put
        throw new UnsupportedOperationException("任务 7.2：实现 collect");
    }

    /** 上报线程主体：攒满 BATCH_SIZE 或等满 FLUSH_TIMEOUT 就 flush */
    void runUploader() {
        List<Integer> batch = new ArrayList<>(100);
        while (running || !buffer.isEmpty()) {          // 停机后还要把余量消费完（drain）
            // TODO(实现)：
            //  1. buffer.poll(1, SECONDS) 取一条，取到则 batch.add + consumed.incrementAndGet()
            //  2. batch.size() >= 100 → flush(batch)
            //  3. 或距上次 flush 已超 3 秒且 batch 非空 → flush(batch)（提示：记录 lastFlushAt）
            throw new UnsupportedOperationException("任务 7.2：实现 runUploader 主循环");
        }
        if (!batch.isEmpty()) flush(batch);             // 收尾
    }

    void flush(List<Integer> batch) {
        System.out.println("[" + Thread.currentThread().getName() + "] 批量上报 "
                + batch.size() + " 条");
        batch.clear();
    }

    public static void main(String[] args) throws Exception {
        SensorDataPipeline pipeline = new SensorDataPipeline();

        Thread uploader = new Thread(pipeline::runUploader, "uploader");
        uploader.start();

        Thread[] collectors = new Thread[3];
        for (int c = 0; c < 3; c++) {
            final int id = c;
            collectors[c] = new Thread(() -> {
                try {
                    for (int i = 0; i < 200; i++) {       // 每线程 200 条，共 600 条
                        pipeline.collect(20 + id * 10 + (i % 5));
                        Thread.sleep(2);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "collector-" + id);
        }
        for (Thread t : collectors) t.start();
        for (Thread t : collectors) t.join();

        pipeline.running = false;                        // 通知停机
        uploader.join(10_000);
        System.out.println("验收: produced=" + pipeline.produced.get()
                + ", consumed=" + pipeline.consumed.get()
                + "（两者必须相等 = 无丢失）");
    }
}
```

**验收标准：** ①`produced == consumed == 600`（跑 10 次全对）；②观察到"批量上报 100 条"为主，结尾出现一次不满 100 的尾批（3 秒超时触发或余量 drain）；③uploader 在停机后 10 秒内退出（main 不卡死）。
**已知陷阱：** `poll(1s)` 超时返回 null——直接 `batch.add(null)` 会 NPE，先判空。

- [ ] Step 1: 实现 collect 与 runUploader 主循环
- [ ] Step 2: 运行 main，通过全部验收（连续 10 次）
- [ ] Step 3: 思考题——①为什么 uploader 退出条件是 `running || !buffer.isEmpty()` 而不是只看 running？②若采集速率远高于上报，系统行为是什么？（有界队列 → put 阻塞 → 反压到采集端）③`drain(batch)` 一把抢（`buffer.drainTo`）为什么比循环 poll 高效？
- [ ] Step 4: 提交 `git add . && git commit -m "learn(m7): 7.2 传感器数据管道"`

### 任务 7.3：批量指令下发器（CompletableFuture 编排复用版）

**Files:**
- Create: `src/main/java/com/learn/concurrent/scenarios/BatchCommandDispatcher.java`

**场景描述：** 把任务 4.3 的 scatter-gather 封装成可复用组件：`dispatch(List<String> deviceIds, String command)` 并行下发，单台超时 2s、失败重试 1 次，返回每台的结果（成功/失败+原因）。**要求失败设备自动重试一次**——比 4.3 多一层"编排"。

**骨架：**

```java
package com.learn.concurrent.scenarios;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BatchCommandDispatcher {
    record CommandResult(String deviceId, boolean success, String detail) { }

    private final ExecutorService pool;

    public BatchCommandDispatcher() {
        // TODO(实现)：创建 I/O 型线程池（自定义命名，任务 6.2 的三件套可简化）
        // 提示：虚拟线程时代也可以直接 Executors.newVirtualThreadPerTaskExecutor()（任务 6.5），二选一并注释理由
        throw new UnsupportedOperationException("任务 7.3：初始化线程池");
    }

    /** 下发到单台设备：随机失败 30%，耗时 100~500ms */
    boolean sendToDevice(String deviceId, String command) {
        try {
            Thread.sleep(100 + (long) (Math.random() * 400));
            return Math.random() > 0.3;                 // 30% 失败
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 并行下发 + 超时 + 失败重试一次 + 聚合 */
    public Map<String, CommandResult> dispatch(List<String> deviceIds, String command) {
        // TODO(实现)：
        //  1. 每台设备一个 CompletableFuture.supplyAsync(...)
        //  2. 单台流程：sendToDevice 成功 → CommandResult(true)
        //     失败 → 重试一次 sendToDevice，再失败 → CommandResult(false, "重试后仍失败")
        //  3. 整链 orTimeout(2, SECONDS)，超时/异常 → CommandResult(false, e.getClass().getSimpleName())
        //  4. allOf 聚合后收集为 Map<deviceId, CommandResult>
        throw new UnsupportedOperationException("任务 7.3：实现 dispatch");
    }

    public void shutdown() { pool.shutdown(); }

    public static void main(String[] args) {
        BatchCommandDispatcher d = new BatchCommandDispatcher();
        List<String> devices = List.of("dev-1", "dev-2", "dev-3", "dev-4", "dev-5", "dev-6");
        Map<String, CommandResult> results = d.dispatch(devices, "setTemp=26");
        results.forEach((id, r) -> System.out.println(id + " → " + (r.success() ? "成功" : "失败: " + r.detail())));
        d.shutdown();
    }
}
```

**验收标准：** ①单次运行总耗时 < 3 秒（并行证据——串行最坏 6×0.5×2 > 3s）；②大部分设备成功，个别"重试后仍失败"（30% 失败率下几乎每次都有，若没有属正常波动）；③无任何未捕获异常打印；④再跑 5 次，成功率稳定在 60%~100% 区间且程序总是正常结束。
**提示：** "重试一次"不要嵌套 future，直接在 supplyAsync 的 lambda 里写 `send || send` 的顺序逻辑更简单；orTimeout 会把超时变成 CompletionException，在 exceptionally/handle 里翻译成 CommandResult。

- [ ] Step 1: 实现构造器与 dispatch
- [ ] Step 2: 运行 main，通过全部验收
- [ ] Step 3: 思考题——①orTimeout 抛出后，还在跑的 sendToDevice 线程能被取消吗？（不能中断业务逻辑——超时只是"不等了"，引出真正可中断的设计）②把池换成虚拟线程后，池大小 4→∞ 的行为差异？
- [ ] Step 4: 提交 `git add . && git commit -m "learn(m7): 7.3 批量指令下发"`

### 任务 7.4：网关并发限流器（Semaphore 实战）

**Files:**
- Create: `src/main/java/com/learn/concurrent/scenarios/GatewayRateLimiter.java`
- Test: `src/test/java/com/learn/concurrent/scenarios/GatewayRateLimiterTest.java`

**场景描述：** 每台网关同一时刻最多接受 3 条在途指令；超出的指令在 500ms 内抢不到"通行证"就拒绝。需要统计**实际峰值并发**证明限流生效，并统计拒绝数。

**骨架：**

```java
package com.learn.concurrent.scenarios;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayRateLimiter {
    private final int maxConcurrent;
    private final Map<String, Semaphore> gatewayPermits = new ConcurrentHashMap<>();
    private final AtomicInteger rejected = new AtomicInteger();
    private final AtomicInteger currentConcurrent = new AtomicInteger();   // 观测用
    private final AtomicInteger peakConcurrent = new AtomicInteger();

    public GatewayRateLimiter(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }

    private Semaphore permitsOf(String gatewayId) {
        // TODO(实现)：每台网关一个 Semaphore(maxConcurrent)，用 computeIfAbsent 惰性创建
        throw new UnsupportedOperationException("任务 7.4：实现 permitsOf");
    }

    /** 尝试在 gatewayId 上执行指令：500ms 内拿不到许可 → 拒绝并返回 false */
    public boolean trySend(String gatewayId, Runnable command) {
        // TODO(实现)：
        //  1. permitsOf(gatewayId).tryAcquire(500, TimeUnit.MILLISECONDS)，失败 → rejected++ 并 return false
        //  2. 成功后维护 currentConcurrent / peakConcurrent（peak 用 CAS 循环更新最大值）
        //  3. try/finally：执行 command（模拟耗时 100~300ms），finally 里 release 并 currentConcurrent--
        throw new UnsupportedOperationException("任务 7.4：实现 trySend");
    }

    public int getRejected() { return rejected.get(); }
    public int getPeakConcurrent() { return peakConcurrent.get(); }

    public static void main(String[] args) throws Exception {
        GatewayRateLimiter limiter = new GatewayRateLimiter(3);
        Thread[] senders = new Thread[50];
        for (int i = 0; i < 50; i++) {
            final int id = i;
            senders[i] = new Thread(() ->
                    limiter.trySend("gateway-1", () -> {
                        try { Thread.sleep(100 + (long) (Math.random() * 200)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }));
        }
        for (Thread t : senders) t.start();
        for (Thread t : senders) t.join();
        System.out.println("验收: 峰值并发（期望≤3）= " + limiter.getPeakConcurrent()
                + "，拒绝数（>0）= " + limiter.getRejected());
    }
}
```

**JUnit 测试（`src/test/java/com/learn/concurrent/scenarios/GatewayRateLimiterTest.java`）：**

```java
package com.learn.concurrent.scenarios;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatewayRateLimiterTest {
    @Test
    void 并发峰值不超过限制且总数守恒() throws Exception {
        GatewayRateLimiter limiter = new GatewayRateLimiter(3);
        Thread[] ts = new Thread[50];
        for (int i = 0; i < 50; i++) {
            ts[i] = new Thread(() -> limiter.trySend("g1", () -> {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        assertTrue(limiter.getPeakConcurrent() <= 3, "峰值=" + limiter.getPeakConcurrent());
        assertEquals(50, limiter.getSucceeded() + limiter.getRejected(), "成功+拒绝 必须等于 50");
    }
}
```

> 前置要求：给骨架补一个 `private final AtomicInteger succeeded` 计数器（trySend 成功路径 +1，配好 `getSucceeded()`）。运行：`mvn -q test -Dtest=GatewayRateLimiterTest`

**验收标准：** ①峰值并发 ≤ 3（多跑几次都成立）；②拒绝数 > 0（50 条挤 3 个坑、每条占 100~300ms，必有拒绝）；③JUnit 测试通过。

- [ ] Step 1: 实现 permitsOf 与 trySend（顺手加 succeeded 计数器）
- [ ] Step 2: 运行 main 与 JUnit 测试，通过全部验收
- [ ] Step 3: 思考题——①`Semaphore` 释放的线程和获取的线程可以不同吗？（可以！这是它与锁的本质区别，也是"借走通行证"式编程的基础）②这个方案限的是"并发数"还是"速率"？（并发数；限 QPS 要漏桶/令牌桶，见模块 9）
- [ ] Step 4: 提交 `git add . && git commit -m "learn(m7): 7.4 网关并发限流"`

### 任务 7.5：并发安全本地缓存（computeIfAbsent + 过期）

**Files:**
- Create: `src/main/java/com/learn/concurrent/scenarios/SimpleCache.java`
- Test: `src/test/java/com/learn/concurrent/scenarios/SimpleCacheTest.java`

**场景描述：** 设备影子/元数据的进程内缓存：`getOrLoad(key, loader)`——命中且未过期直接返回；否则调 loader（如查库）并缓存。核心难点：**并发下同一 key 的 loader 只允许执行一次**（防止缓存击穿打爆数据库）。附带 TTL 过期。

**骨架：**

```java
package com.learn.concurrent.scenarios;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SimpleCache<V> {
    record Entry<V>(V value, long expireAt) {
        boolean expired() { return System.currentTimeMillis() > expireAt; }
    }

    private final Map<String, Entry<V>> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public SimpleCache(long ttlMillis) { this.ttlMillis = ttlMillis; }

    /** 命中且未过期 → 返回；否则 loader 加载并缓存。同 key 并发时 loader 只执行一次 */
    public V getOrLoad(String key, Supplier<V> loader) {
        // TODO(实现)：
        //  1. 快路径：cache.get(key)，非 null 且未过期 → 直接返回 value
        //  2. 慢路径：cache.compute(key, (k, old) -> {
        //        old != null && !old.expired() → 复用 old
        //        否则 new Entry(loader.get(), now + ttl) —— compute 的 lambda 内只有一个线程会执行 loader
        //     }) 后返回 value
        //  3. 思考：过期条目要不要主动 remove？compute 内返回 null 即删除该 entry
        throw new UnsupportedOperationException("任务 7.5：实现 getOrLoad");
    }

    public int size() { return cache.size(); }
}
```

**JUnit 测试（这是全项目第一个"硬"测试——确定性断言）：**

```java
package com.learn.concurrent.scenarios;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SimpleCacheTest {
    @Test
    void 同key并发100线程_loader只执行一次() throws Exception {
        SimpleCache<String> cache = new SimpleCache<>(60_000);
        AtomicInteger loadCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(100);
        CountDownLatch done = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                ready.countDown();
                try { ready.await(); } catch (InterruptedException e) { return; }   // 100 线程对齐起跑
                cache.getOrLoad("device-meta-001", () -> {
                    loadCount.incrementAndGet();
                    try { Thread.sleep(10); } catch (InterruptedException ignored) { }
                    return "meta";
                });
                done.countDown();
            }).start();
        }
        done.await();
        assertEquals(1, loadCount.get(), "loader 应只执行一次，实际 " + loadCount.get());
        assertEquals("meta", cache.getOrLoad("device-meta-001", () -> "不应再加载"));
    }

    @Test
    void 过期后重新加载() throws Exception {
        SimpleCache<String> cache = new SimpleCache<>(50);
        assertEquals("v1", cache.getOrLoad("k", () -> "v1"));
        Thread.sleep(80);                       // 超过 TTL
        assertEquals("v2", cache.getOrLoad("k", () -> "v2"));
    }
}
```

**验收标准：** `mvn -q test -Dtest=SimpleCacheTest` 两个测试全绿；第一个测试连跑 20 次全绿（并发下 loader 恰好一次是硬指标）。
**已知陷阱：** `compute` 的 lambda 里调 `loader.get()` 且 loader 慢时，会**锁住该桶**——同桶其他 key 的写操作也会被拖住。这是"缓存放 ConcurrentHashMap + compute"方案的真实代价，记入笔记（业界更完善的方案：Caffeine，见模块 9）。

- [ ] Step 1: 实现 getOrLoad
- [ ] Step 2: 编写/运行测试，20 次全绿
- [ ] Step 3: 思考题——①为什么不用 任务 2.3 的 DCL 来做这个缓存？②把 TTL 改成"定时全量清理"怎么做？代价是什么？
- [ ] Step 4: 提交 `git add . && git commit -m "learn(m7): 7.5 并发本地缓存"`

---

# 模块 8：原理深入（internals）

**模块目标：** 从"会用"到"懂原理"。本模块以概念学习 + 带问题的源码阅读为主（读 JDK 源码就用 IDEA 的 Ctrl+点击，配合本任务的导读问题清单），只写少量验证代码。**面试与线上疑难排查的差距就在这里。**

### 任务 8.1：AQS——juc 的骨架

**Files:**
- Create: `src/main/java/com/learn/concurrent/internals/AqsMutex.java`

**概念要点：**
- AQS（AbstractQueuedSynchronizer）= **一个 volatile int state（同步状态）+ 一条 CLH 变体的 FIFO 等待队列**。ReentrantLock、Semaphore、CountDownLatch、ReentrantReadWriteLock、线程池的 Worker 全部基于它。
- 通用骨架：子类只需实现 `tryAcquire/tryRelease`（独占）或 `tryAcquireShared/tryReleaseShared`（共享），**排队、阻塞、唤醒全由框架完成**（`acquire→tryAcquire 失败→入队→LockSupport.park`；`release→唤醒队首→unpark`）。
- 各组件的 state 语义：ReentrantLock=重入次数；Semaphore=剩余许可；CountDownLatch=计数；ReadWriteLock=高低 16 位拆成写/读计数。

- [ ] **Step 1：亲手实现一个互斥锁（JDK 文档经典示例）**

```java
package com.learn.concurrent.internals;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/** 用 AQS 手写互斥锁：state 0=空闲，1=持有 */
public class AqsMutex {
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {                    // CAS 抢 state
                setExclusiveOwnerThread(Thread.currentThread()); // 记录持有者（可重入判断用）
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            setExclusiveOwnerThread(null);
            setState(0);            // 已处于独占模式，无需 CAS
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    private final Sync sync = new Sync();

    public void lock() { sync.acquire(1); }          // 排队/阻塞由 AQS 负责
    public void unlock() { sync.release(1); }
    public boolean tryLock() { return sync.tryAcquire(1); }

    public static void main(String[] args) throws InterruptedException {
        AqsMutex mutex = new AqsMutex();
        Runnable job = () -> {
            mutex.lock();
            try {
                System.out.println("[" + Thread.currentThread().getName() + "] 进入临界区");
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            } finally {
                mutex.unlock();
            }
        };
        Thread a = new Thread(job, "A"), b = new Thread(job, "B");
        a.start(); b.start(); a.join(); b.join();
        System.out.println("两线程通过自写互斥锁串行进入临界区 —— AQS 工作正常");
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.internals.AqsMutex`
预期观察：A、B 严格串行进出临界区。

- [ ] **Step 3：源码导读（IDEA 打开 `AbstractQueuedSynchronizer`，带着问题读）**
  1. `acquire(1)` 的四步流程：tryAcquire → addWaiter（入队）→ acquireQueued（自旋+park）→ selfInterrupt。线程在哪一步真正阻塞？（LockSupport.park）
  2. `release` 怎样找到该唤醒的节点？为什么要从**队尾往回**找？（唤醒 head 后继；并发入队时从尾遍历保证不漏）
  3. 仿照本例，把 tryAcquire 改造成"允许 3 个线程同时进入"——恭喜，你写出了 Semaphore（共享模式 tryAcquireShared 返回剩余许可数）。

- [ ] **Step 4：思考题**
  1. 为什么 `state` 必须 volatile？（可见性：释放线程的写要对获取线程可见——happens-before）
  2. park 一个线程后如果无人 unpark，它会怎样？（永远 WAITING——引出任务 3.2 死锁与"锁泄漏"）

- [ ] **Step 5：提交**

```bash
git add . && git commit -m "learn(m8): 8.1 AQS 手写互斥锁"
```

### 任务 8.2：ConcurrentHashMap 内部实现

**Files:**
- Create: `docs/notes/m8-chm.md`（阅读笔记）

**概念要点：**
- JDK 7：分段锁 Segment（默认 16 段，锁粒度=段）→ JDK 8 起弃用，改为 **CAS 初始化 + 桶级 synchronized + 链表/红黑树**，锁粒度细化到单个桶。
- 关键机制（读源码时对号入座）：`sizeCtl`（初始化/扩容状态戳）、`transfer`（多线程**协助扩容**：各认领一段区间迁移）、`CounterCell[]`（分段计数，size() 求和估算）、`tabAt/casTabAt`（用 `VarHandle` 做的 volatile 元素访问）。
- `computeIfAbsent` 的 loader **在该桶的锁内执行**——所以 loader 里再操作同一 map 可能死锁/死循环，且 loader 慢会阻塞同桶写（任务 7.5 已体验）。

- [ ] **Step 1：带问题读源码（IDEA: ConcurrentHashMap）**
  1. `putVal` 里，桶为空、桶非空、正在扩容三种情况分别怎么处理？（CAS 放入 / synchronized(首节点) / helpTransfer 协助迁移）
  2. `get` 为什么完全不加锁？（Node.val 与 next 都是 volatile；读天然弱一致）
  3. `addCount` 为什么不直接维护一个计数？（单点 AtomicLong 是竞争热点——与 LongAdder 同一思想，呼应任务 2.5/8.4）
  4. 扩容期间读写还能进行吗？（能——这就是它优于"复制整个数组"方案的原因）

- [ ] **Step 2：写笔记**——用一张自己画的文字示意图描述"put 一个 key 的完整旅程（hash→定位桶→CAS或锁→链表/树→计数）"，存入 `docs/notes/m8-chm.md`。

- [ ] **Step 3：思考题**
  1. 桶级锁意味着什么？两个不同 hashCode（不同桶）的写并发吗？
  2. 为什么 key/value 都不允许 null 而 HashMap 允许？

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m8): 8.2 ConcurrentHashMap 源码导读"
```

### 任务 8.3：ThreadLocal 内存泄漏真相

**Files:**
- Create: `src/main/java/com/learn/concurrent/internals/ThreadLocalLeakDemo.java`
- Create: `docs/notes/m8-threadlocal.md`

**概念要点：**
- 结构：`Thread → ThreadLocalMap → Entry[key=ThreadLocal 的**弱引用**, value=强引用]`。
- 泄漏路径：ThreadLocal 实例被外部置 null 后，key 变成可回收（弱引用自动清理），但 **value 仍被 Entry 强引用**，而 Entry 被 Thread 持着——线程池里线程不死，value 就一直堆积。
- 为什么 key 用弱引用？这是一种**自我纠错设计**：key 清理后成为 stale entry，`set/get/remove` 路径上的启发式清理（`expungeStaleEntry`）有机会顺带清 value——但**不能依赖它**，正解是**用完必须 `remove()`**（任务 1.4 的铁律的底层原因）。
- 线程池 + `InheritableThreadLocal` 的坑：只在**创建子线程时**复制一次，池里线程复用根本不会重新继承——链路追踪透传要用阿里 **TransmittableThreadLocal（TTL）**（包装 Runnable/线程池），物联网后端的 traceId/deviceId 透传几乎都靠它。

- [ ] **Step 1：编写观察 Demo**

```java
package com.learn.concurrent.internals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadLocalLeakDemo {
    static ThreadLocal<byte[]> payload = new ThreadLocal<>();   // 每次放 10MB

    public static void main(String[] args) throws Exception {
        // 仅演示；生产禁用 newFixedThreadPool（任务 6.2 的规约）
        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int round = 1; round <= 6; round++) {
            pool.execute(() -> {
                payload.set(new byte[10 * 1024 * 1024]);       // 10MB
                // 场景 A：不 remove → 4 个线程各攒 10MB，共 40MB 常驻
                // 场景 B：finally 里 payload.remove() → 随任务结束回收
            });
        }
        Thread.sleep(500);
        System.out.println("打开 jvisualvm / jconsole 观察堆占用，切换 A/B 场景对比");
        pool.shutdown();
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.internals.ThreadLocalLeakDemo`
分别用场景 A（不 remove）/ 场景 B（finally 里 remove）各跑一次，趁进程存活时用 jconsole 或 `jmap -histo <pid> | head` 对比 `[B`（byte 数组）数量差异；把两幅"内存画像"结论写进笔记。

- [ ] **Step 3：思考题**
  1. 如果 Entry 的 key 也是强引用，泄漏的是 value 还是 key+value 都漏？（都漏——弱引用至少给了纠错机会）
  2. TTL（TransmittableThreadLocal）解决的是什么问题？一句话说清。

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m8): 8.3 ThreadLocal 泄漏"
```

### 任务 8.4：锁优化与伪共享

**Files:**
- Create: `src/main/java/com/learn/concurrent/internals/LockOptimizationDemo.java`
- Create: `docs/notes/m8-lockopt.md`

**概念要点：**
- **synchronized 锁升级**（概念即可）：无锁 → 偏向锁（单线程反复获取，只记线程 ID）→ 轻量级锁（CAS 自旋）→ 重量级锁（park 挂起）。JDK 15 起默认禁用偏向锁（维护成本高，收益小）。
- JIT 锁优化：**锁消除**（逃逸分析证明对象不会跨线程，直接删锁，如局部 StringBuffer）、**锁粗化**（循环内反复锁同一对象合并为一次）。
- **伪共享（false sharing）**：CPU 缓存行 64 字节，两个线程各写相邻的独立变量若同处一行，缓存一致性协议（MESI）会让两核互相失效缓存行——性能塌方。`LongAdder` 的 Cell、`ConcurrentHashMap` 的 CounterCell 都用 `@Contended` 注解做**缓存行填充**隔离。
- 这解释了任务 2.5 的现象：LongAdder 快 = ①分段减少竞争 ②填充避免伪共享 ③读时求和牺牲即时精确性。

- [ ] **Step 1：编写基准对比（复用任务 2.5 思路，线程数加大）**

```java
package com.learn.concurrent.internals;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class LockOptimizationDemo {
    static final int THREADS = 16;
    static final int LOOPS = 5_000_000;

    public static void main(String[] args) throws InterruptedException {
        runAtomicLong();                      // 第 1 轮：JIT 预热，数据作废
        long a = runAtomicLong();
        long b = runLongAdder();
        System.out.println("AtomicLong 16线程×500万 = " + a + " ms");
        System.out.println("LongAdder  16线程×500万 = " + b + " ms（通常快 2~5 倍）");
    }

    static long runAtomicLong() throws InterruptedException {
        AtomicLong v = new AtomicLong();
        return time(() -> {
            Thread[] ts = new Thread[THREADS];
            for (int i = 0; i < THREADS; i++) {
                ts[i] = new Thread(() -> { for (int j = 0; j < LOOPS; j++) v.incrementAndGet(); });
                ts[i].start();
            }
            joinAll(ts);
        });
    }

    static long runLongAdder() throws InterruptedException {
        LongAdder v = new LongAdder();
        return time(() -> {
            Thread[] ts = new Thread[THREADS];
            for (int i = 0; i < THREADS; i++) {
                ts[i] = new Thread(() -> { for (int j = 0; j < LOOPS; j++) v.increment(); });
                ts[i].start();
            }
            joinAll(ts);
        });
    }

    static long time(Runnable r) {
        long t = System.nanoTime();
        r.run();
        return (System.nanoTime() - t) / 1_000_000;
    }

    static void joinAll(Thread[] ts) {
        for (Thread t : ts) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
```

- [ ] **Step 2：运行并观察**

运行：`mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.internals.LockOptimizationDemo`
预期观察：LongAdder 明显快于 AtomicLong；把两者结果差距与"分段+伪共享填充"两条原理对应写入笔记。

- [ ] **Step 3：思考题**
  1. `@Contended` 生效需要 JVM 参数 `-XX:-RestrictContended`（JDK 内部类除外）——为什么 JDK 不默认开放？（内存换性能，滥用会膨胀对象）
  2. 两个不相干的 volatile 变量声明在同一个类里相邻位置，一定伪共享吗？（不一定——取决于运行时内存布局，但热点下值得防范）

- [ ] **Step 4：提交**

```bash
git add . && git commit -m "learn(m8): 8.4 锁优化与伪共享基准"
```

---

# 模块 9：物联网中的成熟方案（对照篇，纯文档）

**模块目标：** 回答"单机 JDK 并发之外，物联网业界用什么"。目的是**建立选型坐标系**：知道每个自写组件在业界对应什么成熟轮子、什么时候不该自己写。本模块不写代码，读文档 + 做一张自己的选型笔记即可。

### 9.1 I/O 并发模型：从线程池到 Netty/虚拟线程

设备接入层（TCP/UDP/MQTT 网关）的核心问题是"万级长连接"。三条技术路线：

| 路线 | 原理 | 代表 | 特点 |
|---|---|---|---|
| 线程池 + 阻塞 I/O | 一连接/一请求占一线程 | 传统 Tomcat（BIO 时代）、任务 6 的线程池 | 写法直观；线程数=并发数，万级连接撑不住 |
| 事件循环 + 异步回调 | 少量线程 + IO 多路复用（epoll），连接上的事件分发到 EventLoop 串行处理 | **Netty**（设备接入事实标准）、Vert.x | 万级连接、极低线程数；**回调地狱**、单 EventLoop 不能阻塞（一阻塞全连接卡死——与任务 6.4"并行流禁 I/O"同一道理） |
| 虚拟线程 + 阻塞写法 | 海量虚拟线程，阻塞点自动卸载 | Java 21+（任务 6.5）、Loom 生态 | 用同步写法获得接近 Netty 的吞吐；适合业务层 |

**对照点：** Netty 的"单 EventLoop 串行处理某连接的所有事件"本质是**用串行化消灭竞争**（同连接的事件不需要加锁）——这与模块 2~3 的"加锁保护共享状态"是同一问题的两种解法。值得读：Netty 官方文档的 Threading Model 章节。

### 9.2 框架层：响应式与 Actor

- **Spring WebFlux / Project Reactor、RxJava**：响应式流（Reactive Streams），核心贡献是**背压（backpressure）标准化**——任务 7.2 手写的"有界队列背压"，在 Reactor 里是 `onBackpressureBuffer/Drop/Latest` 一等公民。物联网遥测流（采集快、消费慢）的天然选型；代价是调试链路陡峭。
- **Vert.x**：物联网后端热门框架（-event loop + Verticle + EventBus），单机内"线程数=核数"，Verticle 间通信靠消息（Actor 风格），又一层"用消息传递消灭共享"。
- **Akka（Actor 模型）**：每个设备/网关建模为一个 Actor，信箱（mailbox）天然串行化——"一个设备一个轻量线程"的抽象。ThingsBoard（开源 IoT 平台）早期即大量使用 Akka。

**对照点：** 你在任务 7.1 手写的 DeviceSessionManager，在 Actor 模型下就是"每个设备一个 Actor"，无锁、无扫描线程。

### 9.3 数据管道：MQTT Broker 与消息中间件

- **设备接入协议层**：MQTT Broker——**EMQX**（国内主流，支持百万连接、规则引擎）、Mosquitto（轻量）、HiveMQ。后端服务不直接扛设备连接，而是从 Broker 订阅/发布（Pub/Sub 解耦）。协议层还包括 CoAP（UDP 受限设备）、LoRaWAN/NB-IoT（运营商侧）。
- **服务间数据管道**：Kafka / RocketMQ / Pulsar。设备消息先入消息中间件再消费——**削峰、解耦、重放**三大收益。你在任务 7.2 手写的"缓冲队列+批量上报"，生产上= Kafka + 消费者批量拉取（`poll()` 批量语义与你的 `drain` 同构）。
- **流计算**：Flink/Spark Streaming 对遥测做窗口聚合（5 分钟均值、异常检测）——任务 6.4 的"分治聚合"放大到分布式版。

**对照点：** 单机 BlockingQueue 解决的是**进程内**生产消费；跨进程/跨服务就必须换成消息中间件，且获得持久化与重放能力。

### 9.4 分布式并发控制：单机锁失效之后

服务一旦多实例部署（高可用刚需），模块 3 的所有锁、模块 2 的 volatile 全部失效——**进程内存不可跨 JVM 共享**：

| 单机方案 | 多实例后的替代 | 说明 |
|---|---|---|
| synchronized / ReentrantLock | **Redis 分布式锁（Redisson）**、ZooKeeper、etcd | Redisson 的看门狗自动续期；ZK 可靠性更高、性能低 |
| ConcurrentHashMap 会话表 | Redis / 集群会话（设备海量时用 Redis + 本地 Caffeine 二级缓存） | |
| 单机限流（任务 7.4 Semaphore） | Guava RateLimiter（仍是单机！）/ **Sentinel / Redis+Lua 令牌桶** | 限"并发数"→ 限"QPS"用令牌桶/漏桶 |
| ScheduledExecutor 定时任务 | **xxl-job**（国内主流）、Quartz 集群模式 | 多实例定时任务需要调度中心防重复执行 |
| 手写缓存（任务 7.5） | **Caffeine**（W-TinyLFU、异步刷新、事件监听，公认 JVM 缓存之王） | Guava Cache 的继任者 |
| @Async/@Scheduled 裸用 | Spring 线程池规范化封装、ThreadPoolTaskExecutor | 对应任务 6.2 的三件套 |

**对照点：** 任务 7.5 的"compute 桶锁"陷阱，Caffeine 用更细粒度的机制解决了；自己的 SimpleCache 只用于学原理。

### 9.5 参考架构：读一个开源 IoT 平台

推荐通读 **ThingsBoard**（Java 技术栈、文档完善）的架构文档（thingsboard.io/docs/pe/reference/ 或 GitHub README）：设备接入（Netty 实现 MQTT/CoAP/LwM2M 网关）→ Actor 系统（设备/租户 Actor）→ Kafka 规则引擎 → TS 存储。把它的每个组件与本文档对应章节连线（如"device actors ↔ 任务 7.1 会话管理"、"Kafka ↔ 任务 7.2 管道"），写成 `docs/notes/m9-thingsboard.md`。

- [ ] Step 1: 通读 9.1~9.4，把每个"对照点"用自己的话写成 1~2 句结论，存入 `docs/notes/m9-notes.md`
- [ ] Step 2: 完成 ThingsBoard 架构连线笔记
- [ ] Step 3: 思考题——若让你设计"10 万台设备、3 实例部署"的接入后端：连接层选什么？会话放哪？指令互斥怎么做？（答案不唯一，关键是能自圆其说地引用本模块的对照表）
- [ ] Step 4: 提交 `git add . && git commit -m "learn(m9): 物联网成熟方案对照笔记"`

---

## 附录 A：建议学习节奏

每周 3~4 个任务、每次 1~1.5 小时，约 6 周完成主体：

| 周 | 内容 |
|---|---|
| 1 | 模块 0 + 模块 1（基础篇） |
| 2 | 模块 2（JMM/竞争——理论重头，笔记多写） |
| 3 | 模块 3 + 模块 4 |
| 4 | 模块 5 + 模块 6（6.5 虚拟线程可单独安排） |
| 5 | 模块 7（实战篇，每个练习至少独立实现一遍再对提示） |
| 6 | 模块 8（原理篇，配合面试题自测）+ 模块 9（对照篇） |

## 附录 B：工具与参考资料

**排障工具：** `jps`（找进程）、`jstack <pid>`（线程 dump，死锁分析）、`jconsole`/`jvisualvm`（GUI 观察）、`jmap -histo`（内存直方图，任务 8.3 用）、JFR（Java Flight Recorder，生产级低开销 profiling）、IDEA"捕获线程转储"按钮。

**书籍（按优先级）：**
1. 《Java 并发编程实战》（Java Concurrency in Practice）——圣经，模块 2/3 的理论源头，选读前 10 章。
2. 《Java 并发编程的艺术》（方腾飞）——中文，JMM/AQS 讲得细，配合模块 8。
3. 《阿里巴巴 Java 开发手册》并发处理章节——工程红线，15 分钟读完，对应任务 6.2。

**官方资料：** `java.util.concurrent` 包文档、JEP 444（虚拟线程）、JLS §17（线程与锁，happens-before 权威定义）。

**扩展阅读（模块 9 延伸）：** Netty User Guide（Threading Model 章）、Reactor 参考文档（Backpressure 章）、Redisson 文档（分布式锁）、Caffeine GitHub README、ThingsBoard 架构文档。

## 附录 C：进度总表

| 模块 | 任务 | 状态 |
|---|---|---|
| 0 | 0.1 工程初始化 | ☐ |
| 1 | 1.1 创建 / 1.2 状态 / 1.3 中断 / 1.4 ThreadLocal | ☐ ☐ ☐ ☐ |
| 2 | 2.1 竞争 / 2.2 JMM / 2.3 volatile / 2.4 synchronized / 2.5 原子类 | ☐ ☐ ☐ ☐ ☐ |
| 3 | 3.1 ReentrantLock / 3.2 死锁 / 3.3 读写锁 / 3.4 Condition | ☐ ☐ ☐ ☐ |
| 4 | 4.1 wait-notify / 4.2 同步工具 / 4.3 CompletableFuture | ☐ ☐ ☐ |
| 5 | 5.1 CHM / 5.2 BlockingQueue / 5.3 COW | ☐ ☐ ☐ |
| 6 | 6.1 参数 / 6.2 自定义池 / 6.3 定时 / 6.4 ForkJoin / 6.5 虚拟线程 | ☐ ☐ ☐ ☐ ☐ |
| 7 | 7.1 会话 / 7.2 管道 / 7.3 指令 / 7.4 限流 / 7.5 缓存 | ☐ ☐ ☐ ☐ ☐ |
| 8 | 8.1 AQS / 8.2 CHM 内部 / 8.3 ThreadLocal / 8.4 锁优化 | ☐ ☐ ☐ ☐ |
| 9 | 9.1~9.5 对照笔记 | ☐ |

---

## 计划自查记录（写完计划时的自检，学习时可忽略）

- 需求①基本组件/类使用 → 模块 1~6（Thread/锁/原子类/同步工具/并发容器/线程池全覆盖）✔
- 需求②常见场景实现 → 模块 4.3、5.x 场景化 demo + 模块 7 五个实战练习 ✔
- 需求③相关类的特性了解 → 各任务"概念要点" + 模块 8 原理深入 ✔
- 需求④物联网成熟方案 → 模块 9 纯文档对照 ✔
- 一致性：包名/类名/运行命令按 `com.learn.concurrent.<模块>` 统一；模块 7 骨架可直接编译（TODO 方法抛异常占位属刻意设计）✔




