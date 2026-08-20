# 模块 4：线程协作（coordination）

> 对应 `docs/learning-plan.md` 模块 4。当前已覆盖：任务 4.1 ~ 4.3（模块收官）。

## 任务 4.1：wait/notify 交替打印

### 关键认知：「在循环里」≠「持有锁」

wait 释放锁后线程停在 await/wait 那一行——**代码位置还在 while 里，但已不持有任何锁**（在锁的等待队列里，WAITING）。被唤醒后也不能瞬间恢复执行：先重新竞争锁（BLOCKED），抢到才从 wait() 返回回到条件判断处。

**任意时刻持锁者至多一个**；另一个要么在等（WAITING，不持锁），要么在抢（BLOCKED）。"两个线程同时在 while 里"说的是代码位置，不是同时执行。

推论：不调 wait 直接 notifyAll → 唤醒者自己一直持锁、等待队列是空的（没人等）→ 自己把回合全打完才放锁，其他线程才第一次进门。synchronized 的互斥性保证"一份数据不会被两个线程同时操作"。

### notify vs notifyAll（本例的选型）

- **两线程**：notify 也能工作（唯一可能的唤醒对象就是对方）。
- **三线程及以上**：notify 可能唤醒"错误的线程"——被唤醒者重验条件发现不是自己回合，睡回去，但这次 notify 已花掉，该出手的线程没被唤醒 → **通知丢失，全员睡觉，程序挂死**。
- notify 只在"等待者完全同质"时安全；等待者异质（奇/偶/第 3 角色不同）一律 notifyAll。

### 用 Condition 重写：哪部分变简单了

**notifyAll 降级为 signal——唤醒从"广播+自筛"变成"点对点"。**

两个 Condition（odd/even）各自只睡一种角色的线程，`evenCondition.signal()` 精确命中偶线程，"叫醒错的人"在结构上不可能——notify 的通知丢失风险被消灭。

正确结构（先判断回合，不是回合才睡）：

```java
turnLock.lock();                          // 铁律：lock 在 try 之前
try {
    while (next[0] <= 100) {
        if (next[0] % 2 == 1) {           // 我的回合
            System.out.println(name + ": " + next[0]++);
            evenCondition.signal();       // 精确唤醒对方
        } else {
            oddCondition.await();         // 不是我的回合才睡
        }
    }
    evenCondition.signal();               // 出循环后唤醒对方，让它重验条件退出
} finally {
    turnLock.unlock();
}
```

### notifyAll 之后当前线程去哪？（高频混淆点）

`notifyAll()` **不放锁、不等待、不阻塞**。它只做一件事：把等待队列里睡着的线程标记为可唤醒（让它们去竞争锁），**然后当前线程继续往下执行**，锁仍在自己手里。

```
当前线程：notifyAll() → 继续执行剩余代码 → 走出 synchronized } → 释放锁
被唤醒者：从 wait 苏醒 → BLOCKED 排队等锁 → 锁空出 → 抢到 → 从 wait() 返回继续
```

**真正让出锁只有三件事**：走出同步块、调 wait()、线程死亡。"notifyAll 之后对方才开始动"是因为当前线程紧接着执行完退出同步块——两步离得近，看起来像 notifyAll 干的。

### notify 安全的同质场景也是"碰巧安全"

即使所有等待者同质（如两个奇数线程，notify 醒谁都能干活），链条能续上也依赖"醒来的人干完活会再 notify"这个实现细节。若某线程还在"尚未进入 wait"的路上，notify 对它无效（只作用于**已在等待队列中**的线程），仍可能断链。工程结论：等待者多于一个或有异质可能 → 一律 notifyAll + while 重验。

### 实测踩坑记录

1. **await 写在循环体开头（无条件先睡）**：两线程进门第一件事就是 await，signal 又写在"打印之后"——打印永远执行不到、signal 永远发不出，全员睡死、零输出、程序挂死。正确顺序是"先判回合：是→打印+signal；否→await"。
2. **`try { lock(); ... } finally { unlock() }`**：违反 3.1 铁律。lock() 本身抛异常时，finally 会对未持有的锁 unlock → IllegalMonitorStateException。必须 `lock(); try {...} finally { unlock(); }`。
3. **收尾 signal 不能忘**：最后一个数打完后对方还在 await 里睡着，没人叫醒就永远退不出循环——"醒来重验条件"的又一应用。

### wait set 与锁入口队列是两条队列（收尾 notifyAll 为什么必不可少）

`wait()` 的线程睡在对象的**等待集合（wait set）**；抢锁失败的线程排在**入口队列**。两条队列独立：退出 synchronized 释放锁只放行**入口队列**，对 wait set 里睡觉的线程**毫无影响**——沉睡者只能被 notify/notifyAll 叫醒。

所以最后打完 100 的线程即使退出同步块、锁已空闲，另一个线程也不会自己醒来——循环外那句 `notifyAll()` 是它唯一的出口。且 notifyAll 不是"命令退出"：它只提供"重验机会"，叫醒后线程自己重判 while 条件（101 <= 100 不成立）才自己退出。

**稳态流转**：打印 → notifyAll（不影响自己）→ while 重判 → 不是我的回合 → wait() 放锁睡去 → 被对方 notifyAll 唤醒 → 抢锁 → 从 wait() 返回 → while 重判 → 轮到我 → 打印。**例外**：打完 100 的线程 while 条件直接不成立，不进 else，直接退出。

### 思考题结论

1. **notifyAll 换 notify？** 两线程可以；三线程可能通知丢失挂死（唤醒错人+重验睡回+notify 已花掉）。异质等待者必须 notifyAll。
2. **Condition 重写简化了什么？** notifyAll→signal 点对点精确唤醒；条件队列按角色分开，结构上消灭"叫错人"。注意 await/signal 必须持锁调用（同 wait/notify），lock 要在 try 之前。

## 任务 4.2：四大同步工具

### 一张表

| 工具 | 语义 | 可复用 | 物联网例子 |
|---|---|---|---|
| CountDownLatch | 等 N 个事件到齐再走 | 一次性 | 全部传感器断开后再重启网关 |
| CyclicBarrier | N 线程互相等齐一起冲 | 可重复（generation） | 多路采集按轮同步开始 |
| Semaphore | 限制并发进入数 | 持续有效 | 网关并发指令限流 |
| Exchanger | 两线程交换对象 | 成对 | 采集/上报双缓冲互换 |

### Semaphore 不是锁：许可计数器（无所有权）

- 锁（mutex）核心是**所有权**：谁加谁解，ReentrantLock 非持有者 unlock 抛 IllegalMonitorStateException。
- 信号量管理的是"池里还有几个许可"：acquire=取走，release=放回，**许可上没写名字**，放回者不必是取走者。
- 这是能力不是疏忽：生产者-消费者信号天生跨线程（消费者 acquire"有数据"，生产者 release"有数据"）——5.2 有界缓冲的 Empty/Full 信号量即此用法。
- 代价：当 Semaphore(1) 被当互斥锁用时，少了"只有持有者能解"的保险，误 release 会破坏互斥。**用灵活性换安全校验，明码标价。**
- "死锁恢复"生产真相：**非主流**。主流仍是锁排序/tryLock 超时/jstack+重启。release-by-other 的真实形态是资源泄漏补偿（工作线程挂掉没还许可，监控线程代为 release）——小众逃生舱。

### Semaphore(1) vs synchronized

| | Semaphore(1) | synchronized/ReentrantLock |
|---|---|---|
| 概念 | 许可计数器（无所有权） | 锁（有所有权） |
| 可重入 | ❌（同线程二次 acquire 自卡） | ✅ |
| 非持有者释放 | ✅ | ❌ |
| 超时/立即放弃 | ✅ | synchronized ❌ / RL ✅ |
| 跨线程释放 | ✅ | ❌ |
| 公平性 | 可选 | 非公平/可选 |

记忆点：Semaphore(1) = **不可重入**的互斥锁——临界区内再 acquire = 自己等自己，永久卡死。

### CyclicBarrier 内部流程（源码视角）

await()：拿锁 → `--count` → **最后到达者（index==0）在持锁状态下执行 barrier 命令** → `nextGeneration()`（重置 count=parties + signalAll 唤醒全部）→ 三路一起放行。

- await 返回各自到达序号（最后到 = 0），可用来"选主"做本轮收尾汇总。
- 暗坑：任一等待线程被中断 → 栅栏 **broken**，全员抛 BrokenBarrierException，一轮坏轮轮废，需重建。
- 一次性 vs 可复用的分界：latch 建模一次性事件；barrier 建模按轮次同步（generation 保证上轮完全放行后才开下轮）。

### 思考题结论

1. **CountDownLatch 为什么一次性？谁是等待方/到达方？** latch 建模不可撤销的一次性事件（门闩打开就是打开）；实现上归零瞬间正批量释放等待者，重置会让新 await 语义歧义。等待方调 `await`（**阻塞的是它**）；到达方调 `countDown`（**从不阻塞**，立刻返回）。归零后被放行执行后续步骤的是 **await 方**，不是 countDown 方——"后续步骤永远属于 await 方"。
2. **Semaphore(1) 等价于什么？** 不可重入互斥锁。差异见上表：无所有权（非持有者可释放）、不可重入、可超时/跨线程释放、可选公平。
3. **Exchanger 适合什么场景？** 双缓冲：采集填缓冲 A、上报同时用缓冲 B，各自完成后 exchange 互换——零拷贝交替，适合采集/上报速率接近且想避免队列拷贝开销的场景。

## 任务 4.3：CompletableFuture 异步编排

### 常用 API 速记

`supplyAsync`（提交异步任务）→ `thenApply`（同步转换）→ `thenCompose`（接异步）→ `thenCombine`（合并两路）→ `allOf`（等多路）→ `exceptionally/handle`（异常兜底）→ `orTimeout`（超时，JDK 9+）。

### 异常流约定（易混）

- `orTimeout` 内部用 `new TimeoutException()`——**无 message**，`getMessage()` 返回 null（Ex5 实测打出"发生异常：null"）。打异常用 `e.toString()` 或直接拼 `e`，别依赖 getMessage()（NPE 也常无 message）。
- **包装规则**：`join()`/`get()` 抛出时**总是包成 CompletionException**；而**直接挂在失败 stage 上的回调**（exceptionally/handle/whenComplete）拿到的是**裸异常**；隔了中间 stage 的回调可能拿到包过的。不确定时打印一次 `e.toString()` 一验便知。
- **allOf 语义**：所有 future 完成它才完成；**任一异常完成 → allOf 异常完成**。`join()` 直接抛 CompletionException，后续 thenRun 不执行 → **其余结果全丢**。
- exceptionally 的作用：把"单台失败"翻译成"正常完成的失败结果字符串"，让 future 正常完成，保住全局聚合（scatter-gather 的标准兜底姿势）。
- **completeOnTimeout(v, t, u) vs orTimeout+handle**：前者超时直接给默认值、更直白，但①超时变成"无声分支"，降级不可观察（要埋点得另做）；②**只兜超时不兜业务异常**（需再补 exceptionally）。分水岭：**要不要"感知超时发生了"**。两者都不取消底层任务（超时≠取消）。

### thenApply vs thenCompose（map vs flatMap）

| | lambda 签名 | 用途 | 错用后果 |
|---|---|---|---|
| thenApply | T → U（普通值） | 同步加工数据 | — |
| thenCompose | T → CompletableFuture\<U\> | 上一步结果发起下一个异步 | 用错 thenApply 得到 CF\<CF\<U\>\> 套娃 |

IoT 例：deviceId → 异步查设备详情（thenCompose）→ 取电量数值（thenApply）。
口诀：**thenApply=map，thenCompose=flatMap；下一步是异步的就 Compose。**

### 为什么默认线程池做 I/O 是灾难

ForkJoinPool.commonPool() 两个特性 × I/O 一个特性 = 灾难组合：

1. **全 JVM 共享单例**：不传池的 supplyAsync、所有 parallelStream、不传池的回调，全挤同一个池。
2. **容量 ≈ CPU 核数-1**（为 CPU 密集设计）。4 核机 = 3 线程。
3. I/O 任务**占着槽位等设备**（线程阻塞在 socket，CPU 空转）。

3 个槽位被 3 次"等 ACK"占满 → 全 JVM 所有依赖 commonPool 的功能排队 → **一个模块的慢 I/O 瘫痪整个进程的默认池用户**（6.4"并行流禁 I/O"同源）。

**规矩：I/O 类异步必须显式传自定义池，隔离"等 I/O"的线程。**

### 思考题结论

1. **去掉 exceptionally，allOf().join() 会怎样？** device-5 的 future 以 TimeoutException 异常完成 → allOf 异常完成 → **join() 抛出时包成 CompletionException** → 后续聚合不执行，其余 7 台结果全丢。不是"返回 null"。
2. **thenApply vs thenCompose？** map vs flatMap：前者 lambda 返回普通值（同步加工），后者返回 CompletableFuture（异步接异步）；错用会得到套娃类型。
3. **为什么不推荐默认池做 I/O？** commonPool 全 JVM 共享且容量≈核数（CPU 密集尺寸）；I/O 占槽不干活，一个慢模块瘫痪所有默认池用户。
