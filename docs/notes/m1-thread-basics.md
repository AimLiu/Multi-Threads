# 模块 1：线程基础

> 对应 `docs/learning-plan.md` 模块 1。本文件随学习进度追加，当前已覆盖：任务 1.1。

## 任务 1.1：线程创建的三种方式

### 三种方式对比

| 方式 | 写法 | 返回值 | 能否抛受检异常 | 适用 |
|---|---|---|---|---|
| 继承 Thread | `class X extends Thread` | 无 | 不能 | 最简单，但占用单继承名额 |
| 实现 Runnable | `new Thread(runnable)` | 无 | 不能 | 任务与线程解耦，**推荐** |
| Callable + FutureTask | `new Thread(futureTask)` | 有（get 取） | 可以 | 需要返回结果 / 抛出异常时 |

**核心思想：** 任务（做什么）与线程（怎么执行）是两个概念。`Runnable`/`Callable` 描述任务，`Thread` 负责执行。把两者分开，是后面线程池、`CompletableFuture` 一切设计的地基。

### start() 与 run() 的区别

**结论先行：`start()` 开新线程，`run()` 不开线程。**

- `start()`：JVM 创建一个新的操作系统线程，由**新线程**去调用 `run()`。异步返回（不等待线程跑完）；一个 Thread 只能调一次 `start()`，调第二次抛 `IllegalThreadStateException`。
- `run()`：一次普通方法调用，在**调用它的线程**里同步执行。可以反复直接调用。

**判定"代码在哪个线程跑"的唯一依据是 `start()`。**

易混点：`Thread.run()` 的默认实现会委托调用 `Runnable.run()`（源码注释："如果此线程是使用 Runnable 任务创建的平台线程，那么调用此方法将调用该任务的 run 方法"）。但这句话说的是**任务内容从哪来**，不是**任务在哪个线程跑**。直接调 `run()` 时，`Runnable.run()` 依然跑在当前线程上。

**最常见的 bug：** 想开线程却写了 `t.run()` —— 结果单线程串行执行，程序"看起来能用但其实是同步的"。

### FutureTask.get() 为什么阻塞

`FutureTask.get()` 无参版本 → `awaitDone(false, 0L)`。关键源码逻辑（简写）：

```java
for (;;) {
    int s = state;
    if (s > COMPLETING)          return s;             // 已完成：返回状态
    else if (Thread.interrupted()) throw new InterruptedException();
    else if (q == null) {
        if (timed && nanos <= 0L) return s;            // ★ 只有 timed=true 才走这里
        q = new WaitNode();
    }
    else if (timed)              LockSupport.parkNanos(this, parkNanos);
    else                         LockSupport.park(this);   // ★ 无参 get 走到这里：挂起
}
```

要点：

1. 无参 `get()` 传的是 `timed=false`，所以 `if (timed && nanos <= 0L)` 这条**立即返回的分支不会触发**，代码最终执行 `LockSupport.park(this)` 把当前线程挂起（进入 WAITING 状态）。
2. 直到任务完成后，`FutureTask.finishCompletion()` 里 `unpark` 唤醒所有等待线程，`get()` 才返回结果。
3. "立即返回"只对定时版 `get(timeout, unit)` 生效，而且它返回的是**状态 state** 而非结果；定时版发现状态仍未完成，就抛 `TimeoutException`。

**验证实验：** 把 `futureTask.get()` 换成 `futureTask.get(1, TimeUnit.SECONDS)` → 约 1 秒后抛 `TimeoutException`，而不是无限阻塞。

**延伸：** 这里的 `park/unpark` 就是任务 1.2 的 WAITING 状态、任务 8.1 AQS 的底层机制。`get()` 是"阻塞式取结果"；任务 4.3 的 `CompletableFuture` 是"回调式取结果"（不阻塞）。

### Runnable 相对继承 Thread 的优势

1. **职责分离**：Runnable 只描述任务，Thread 只负责执行；任务可以独立复用、独立测试。
2. **Java 单继承**：继承了 Thread 就不能再继承别的类（业务基类等）；Runnable 是接口，不占用继承名额。这是面试最爱问、也最本质的一点。
3. **可复用**：Runnable 是普通对象，同一个实例可以交给多个线程、反复提交；Thread 本身就是一个线程，无法被"复用"。
4. **配合线程池**：线程池复用的是**线程**，它要求任务对象与线程对象分离（池里线程固定，任务源源不断投进来）。这也是第 3 点的深层原因。
5. **Callable 是 Runnable 的增强**：带返回值、能抛受检异常，任务 4.3 的 CompletableFuture 大量使用。

### 思考题结论

1. **`t1.start()` 改成 `t1.run()` 有什么变化？** 输出线程名从 `继承Thread线程` 变成 `main`，且变成同步执行（main 要等 run 跑完才继续）。因为 run() 只是普通方法调用，不开新线程。
2. **`futureTask.get()` 放到三个 `start()` 之前会怎样？** 会阻塞。三个线程尚未 start 时，FutureTask 永远不会完成，`get()` 的 `awaitDone` 里 `park` 挂起 main，后面的 start() 永远执行不到，程序卡死。源码里 `timed && nanos <= 0L` 的立即返回只对定时版 get 生效。
3. **Runnable 的优势？** 见上表 5 点，核心是"任务与线程解耦 + 单继承 + 可复用"。

## 任务 1.2：线程状态与生命周期

### 六种状态

| 状态 | 进入条件 | 典型场景 |
|---|---|---|
| NEW | new 完还没 start | — |
| RUNNABLE | 就绪或正在 CPU 上运行 | 正常执行（**含阻塞在网络 I/O 上**） |
| BLOCKED | 竞争 synchronized 监视器锁失败 | 抢锁 |
| WAITING | wait()/join()/LockSupport.park() 无参 | 等通知 |
| TIMED_WAITING | sleep(n)/wait(n)/join(n)/parkNanos | 定时等待 |
| TERMINATED | run 结束或异常退出 | 结束 |

关键点：

- Java 的 RUNNABLE 合并了"就绪"和"运行中"，而且**包含阻塞在网络 I/O 上的线程**（这点与操作系统线程状态不同，是后面 Netty/虚拟线程要解决的问题——`jstack` 里你会看到阻塞在 `socketRead0` 的线程仍标为 RUNNABLE）。
- BLOCKED 只针对 synchronized 监视器锁；等 `Lock.lock()` 时状态是 WAITING/TIMED_WAITING（AQS 用 park 实现，见任务 8.1）。

### wait() 为什么要先 synchronized(lock)

两层原因：

1. **规则层面**：`wait()`/`notify()`/`notifyAll()` 要求当前线程必须持有该对象的 monitor 锁，否则直接抛 `IllegalMonitorStateException`。
2. **设计层面**：`wait()` 要原子地完成"释放锁 + 进入该对象等待队列"。若没有锁保护，在"检查条件"和"进入等待"之间有空窗，别的线程可能已经改条件并 notify 了，这个通知就永久丢失（**丢失唤醒 / lost wakeup**）。synchronized 保证"检查条件 → wait"是原子的。

> 常见误区：把原因答成"不持锁的话别的线程会先抢到锁跑完"——那是 Demo 里的现象，不是 wait 要求持锁的原因。

### notify 后线程立刻 RUNNABLE 吗？

**不是。** 完整流程：

1. A 调 `lock.wait()` → 释放锁，进入等待队列，状态 WAITING。
2. B 在 `synchronized(lock)` 内调 `notifyAll()` → A 被唤醒，但 **B 此刻还持有锁**。
3. 所以 A 无法立刻从 wait() 返回，它要重新竞争锁 → 状态变为 **BLOCKED**。
4. B 退出 synchronized 释放锁 → A 与其它被唤醒线程竞争；抢到锁的那个 → **RUNNABLE**（从 wait() 返回，重新持有锁），其余继续 BLOCKED。

一句话：**被 notify ≠ 立刻能跑，中间一定先经过 BLOCKED。**

### 思考题结论

1. **wait() 前为什么要先 synchronized？** 规则上不持锁会抛 IllegalMonitorStateException；设计上是让"检查条件 → wait"原子化，避免丢失唤醒。
2. **notifyAll 后立刻 RUNNABLE 吗？** 不。被唤醒线程先 BLOCKED（notifier 还持锁），等锁释放并抢到后才 RUNNABLE。

## 任务 1.3：线程控制（中断 / join / 守护线程）

### 中断是协作式的

`interrupt()` 只是"举旗"，线程自己在安全点检查并决定如何退出。`Thread.stop()` 因破坏一致性已废弃。

**线程响应中断有且仅有两条通道：**

| 通道 | 机制 | 适用场景 |
|---|---|---|
| 轮询标志位 | `isInterrupted()` / `interrupted()` | 任务主体是**计算/忙等**（线程在跑普通代码，能主动看标志） |
| 阻塞点抛异常 | `InterruptedException` | 任务主体是**阻塞等待**（sleep/wait/join/take，卡着轮询不到标志） |

关键坑：`InterruptedException` 抛出时会**自动清除中断标志位**。所以 catch 里不退出、想继续循环时，`while(!isInterrupted())` 会因标志被清而永远为 true——因此 catch 里要么 break 退出，要么重新 `Thread.currentThread().interrupt()` 恢复标志。

最健壮写法是**两者结合**：外层 `while(!isInterrupted())` 覆盖计算阶段，catch 里 break 覆盖阻塞阶段（任务 1.3 的 DevicePollingTask 即如此）。

### 守护线程（daemon）

- JVM 退出条件：**所有非守护（用户）线程都结束**。
- 守护线程与用户线程唯一区别 = JVM 是否等它。`setDaemon(true)` 的线程不阻止 JVM 退出，适合日志刷盘、指标上报等辅助线程；**绝不要在守护线程里做关键业务**（JVM 退出时不保证它执行完）。
- 一个永不终止的用户线程（如 `while(true){sleep(1)}`）会一直吊住 JVM，main 结束后进程依然存活。
- `System.exit()` 无视守护/用户线程，直接终止 JVM——唯一"不等任何线程"的退出方式。
- `isInterrupted()`（实例方法，不清标志）vs `interrupted()`（静态方法，**读取后清除**标志）。

### 思考题结论

1. **删掉 `setDaemon(true)` 程序会退出吗？** 不会。那个线程是 while(true) 死循环、永不终止，删掉后变成用户线程，JVM 要等所有用户线程结束，于是进程一直挂着。原因不在 sleep(1)，而在"永不终止的用户线程"。
2. **`while(!isInterrupted())` 与 `while(true)+catch break` 各覆盖什么场景？** 前者覆盖任务主体是计算/忙等的场景（靠轮询标志位响应中断）；后者覆盖任务主体是阻塞等待的场景（靠 catch InterruptedException 响应中断）。区分标准是"任务主体是否阻塞"，而非"生命周期控制 vs 常驻服务"。

## 任务 1.4：ThreadLocal

### 内部结构（正确版）

每个 `Thread` 持有一个 `threadLocals` 字段（ThreadLocalMap），结构是 `Entry[key = ThreadLocal 实例（弱引用）, value = 副本（强引用）]`。

- **key 是 ThreadLocal 实例**（不是线程！）；线程是 map 的**持有者**。
- 线程池场景必须 `remove()`：线程复用，不清理会"读到上一个任务的上下文"（串号）+ 内存泄漏。

### 空间换安全的含义

ThreadLocal 是线程安全的一种**实现策略**：与其多线程共享一个可变对象 + 加锁（时间成本 + 竞争），不如给每个线程一份独立副本，互不共享 → 天然无竞争 → 不用加锁就安全。代价是**空间**：N 个线程 N 份拷贝。

> 易混：这是"隔离换取安全"，与后面的内存泄漏是两回事（泄漏机制在任务 8.3 展开）。

### 两大用途

1. 线程不安全对象每线程一份（经典 `SimpleDateFormat`）。
2. 链路上下文隐式传递（traceId、deviceId、租户 ID）——物联网后端链路追踪标准做法。

### 不跨线程传递

ThreadLocal **不**跨线程传递，每个线程副本互不相干。跨线程靠 `InheritableThreadLocal`（仅在创建子线程那一刻复制一次，线程池下失效）；工程上的正解是阿里 TransmittableThreadLocal（TTL）。Java 21 的 `ScopedValue` 是官方新方向（preview）。

### 复现串号 bug 的正确姿势

污染来自"不清理 + 不重新 set"。要复现：删掉 `finally` 里的 `remove()`，且**奇数设备完全不 set**（不是 set null）：

```java
if (i % 2 == 0) {
    CURRENT_DEVICE.set(deviceId);   // 只有偶数才 set
}
parseAndReport();                    // 奇数不 set → 读线程上残留的上一次值
```

> 注意：每次 `set(null)` 也会覆盖上一次的值，所以"偶数 set null"无法复现串号——必须是"跳过 set 本身"。

### 思考题结论

1. **为什么说"空间换安全"？** 每个线程持有变量独立副本，互不共享 → 无竞争、无需加锁；代价是内存随线程数翻倍。key 是 ThreadLocal 实例而非线程；ThreadLocal 不跨线程传递。
2. **方法参数逐层传 vs ThreadLocal？** 参数=显式而啰嗦（可读、易测、无清理负担、无隐藏耦合）；ThreadLocal=隐式而危险（签名干净、可配 AOP 解耦，但依赖隐式上下文、必须 remove、跨线程不自动跟随）。工程上常配合：链路级信息（traceId/deviceId）走 ThreadLocal，业务参数走方法参数。
