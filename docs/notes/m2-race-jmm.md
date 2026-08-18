# 模块 2：竞争与内存可见性

> 对应 `docs/learning-plan.md` 模块 2。当前已覆盖：任务 2.1。

## 任务 2.1：竞争条件（race condition）

### 核心概念

- **临界区**：访问共享可变资源的代码段。
- **竞争条件**：多线程交错执行临界区，结果依赖调度时序。
- `counter++` 实际是"读 → 加 → 写回"三步，两个线程交错时更新互相覆盖。

### 丢失量级：一个光谱，不是固定比例

丢失多少取决于：线程数、循环紧凑度、JIT 状态。从 0 到超过一半都可能。

**为什么能超过一半**：N 个线程在同一时间窗都读到同一个旧值、各自 +1、再几乎同时写回 → N 次自增只净增 1。窗口重叠越狠，丢得越狠。

**实测数据（本机 10 线程 × 10 万次，多轮）**：

```
第 0 轮：丢 830666 次（83%）
第 1 轮：丢 576128 次（57%）
第 2 轮：丢 0 次
第 3 轮：丢 0 次
```

> 文档里"丢几千~几万"是简化说法，实测更接近真相：丢失量级波动极大。

### JIT 热身会让竞争"消失"

第 2、3 轮丢 0 的原因是 **JIT 热身**：前两轮还是解释执行（慢、交错窗口大）；JIT 把热点循环编译成本地代码后，10 万次自增微秒级跑完，线程几乎不重叠，竞争窗口塌缩。

**教训（与 Q2 同一主题）：竞争条件不重现 ≠ 不存在，只是时序 / JIT 把它藏起来了。**

### 如何稳定复现

1. 加宽读-改-写窗口：

```java
int local = counter;              // 读
int dummy = 0;
for (int k = 0; k < 1000; k++) dummy += k;   // 拉长窗口
counter = local + 1;              // 写
```

2. 禁用 JIT：`mvn -q compile exec:java -Dexec.mainClass=... -Dexec.jvmArgs="-Xint"`（`-Xint` = 纯解释执行），丢更新稳定复现。

### println 为什么让丢失"看起来变小"

两个效应：

1. **串行化 + 可见性**：println 内部 `synchronized`（PrintStream 的锁），既制造 happens-before 边界提升可见性，又把线程串行化（同一时刻只一个线程进 println），压缩读写窗口重叠。
2. **慢 I/O 拉长间隔**：线程从"疯狂锤 counter"变成"慢慢打日志"，交错机会变少。

结论：它**降低概率，不是消除 bug**。实测"仍丢 6 次"就是佐证——**"看起来好了" ≠ "好了"**。正解是 `AtomicInteger` 或加锁。

### 思考题结论

1. **为什么丢失量级波动大，甚至能超过一半？** 丢失是光谱而非固定比例；窗口重叠越狠丢越多，N 线程读同一旧值各自写回时 N 次自增只净增 1。JIT 热身会让竞争窗口塌缩、丢 0——但那是"藏起来"不是"修好"。
2. **println 让丢失变小，能证明问题解决了吗？** 不能。它只是（synchronized 串行化 + 慢 I/O）改变了时序、降低了概率；原子性缺陷仍在，正确做法是 AtomicInteger / 加锁。

## 任务 2.2：JMM 与可见性

### 三性问题

| 问题 | 含义 | 解法 |
|---|---|---|
| 原子性 | 一个操作不可分割 | 锁 / Atomic* / 并发容器 |
| 可见性 | 写何时被其它线程看到 | volatile / synchronized / final |
| 有序性 | 指令重排 | volatile / synchronized |

### happens-before（规范层 vs 实现层）

**关键框架认知**：happens-before 是 JMM 的**规范层**（承诺"满足关系则写对读可见"）；内存屏障是**实现层**（JVM 具体怎么做到）。别把两层混为一谈。

总括：**happens-before 是偏序关系，若"写 W hb 读 R"，则 R 保证看到 W 及其之前的一切效果。** 它管可见性+有序性；原子性另由锁/原子类解决。

六条规则：

| # | 规则 | 一句话 | 例子 |
|---|---|---|---|
| ① | 程序顺序规则 | 同线程内，前面语句 hb 后面语句 | 单线程正确性基础 |
| ② | 监视器锁规则 | 同一锁：unlock hb 后续 lock | A 解锁后，B 加同一锁必然看到 A 的写 |
| ③ | volatile 规则 | volatile 写 hb 后续读 | main 写 shutdown=true，worker 读必见 |
| ④ | start 规则 | t.start() hb 新线程内所有动作 | 启动前设的字段，新线程可见 |
| ⑤ | join 规则 | 线程内动作 hb 其它线程 join() 返回 | join 后读结果字段，必读到 |
| ⑥ | 传递性 | A hb B 且 B hb C → A hb C | 把①②③⑤串成链的胶水 |

### 可见性实验要点

VisibilityDemo 是**概率性**的：删掉 volatile 后"有时能退"不代表没问题（同 2.1 教训——时序把 bug 藏起来了）。想要稳定复现可用 `-Xint` 禁用 JIT。

### 思考题结论

1. **读写都加 synchronized 也能解决可见性吗？依据哪条规则？** 能。依据是**规则②监视器锁规则**（不是笼统的"synchronized 保证可见性"，也不是内存屏障）：写线程 unlock hb 读线程 lock，再经①⑥推出"写 hb 读"。

**坑 1（死锁）**：若 worker `synchronized(lock){ while(!shutdown){} }` 全程占锁忙等，main 拿不到锁写不了 → 死锁。

**坑 2（读在锁外 / 无条件 break）**：读必须发生在锁**内**，且循环要等条件成立才退，不能无条件 break。实测踩坑：`while(!shutdown){ synchronized(lock){ break; } }` —— 读在锁外、break 无条件，线程一启动就退出，根本没在等。

正确写法（锁只包住"读一下"/"写一下"）：

```java
private static boolean shutdown = false;
private static final Object lock = new Object();   // 锁对象要 final

static boolean isShutdown() {
    synchronized (lock) { return shutdown; }       // 读：锁内
}
static void setShutdown() {
    synchronized (lock) { shutdown = true; }       // 写：锁内
}
// worker: while (!isShutdown()) {}  —— 锁内读 + 循环等条件成立
```

2. **boolean 原子为什么还出问题？** 原子性 ≠ 可见性。单次 boolean 写是原子的，但无 volatile 就无 happens-before 边，worker 可能一直读旧值 false 停不下来；volatile 建立规则③的边后才可见。

## 任务 2.3：volatile 语义与 DCL

### volatile 三性对照

| 性质 | volatile 保证? | 说明 |
|---|---|---|
| 可见性 | ✅ | 写刷主存、读取主存（规则③） |
| 有序性 | ✅ | 禁止前后指令重排 |
| 原子性 | ❌ | 复合操作（count++）照样丢更新 |

**核心记忆：volatile 和 synchronized 都能保证可见性 + 有序性，差别只在原子性。**

### volatile 够 vs 必须上锁（判断口诀）

看"写"这一步是否依赖旧值：

- 写不依赖旧值（单写：`flag = true`、状态 = 常量）→ volatile 够（单写本身原子，可见性+有序性正好补齐）。
- 写依赖旧值（读-改-写：`count++`、check-then-act）→ 必须上锁 / 原子类 / CAS。

**口诀：单写标志用 volatile，多写一步操作必上锁。**

### DCL 三要素各司其职

```java
private static volatile DeviceConfigHolder instance;   // volatile：防半初始化（重排）

public static DeviceConfigHolder getInstance() {
    if (instance == null) {                            // ① 外层判空：快路径，省锁
        synchronized (DeviceConfigHolder.class) {
            if (instance == null)                      // ② 内层判空：防重复创建
                instance = new DeviceConfigHolder();
        }
    }
    return instance;
}
```

- **外层判空 = 性能优化**：实例建好后绝大多数调用命中 false 直接返回，不碰锁。去掉它**功能正确、只是慢**（每次抢锁）。
- **内层判空 = 正确性**：锁内再判一次，保证只建一个。去掉它会建多个实例。
- **volatile = 正确性**：`new` 三步（分配内存→初始化→赋值引用）可能重排，别的线程会拿到"引用已赋值但未初始化"的半成品。去掉它小概率出错。

**正确性完全由"锁 + 内层判空 + volatile"保证；外层判空纯粹是性能。**

### 思考题结论

1. **volatile 够 vs 上锁？** 单写（写不依赖旧值）用 volatile；读-改-写（写依赖旧值）必须上锁/原子类。volatile 与 synchronized 都保可见性+有序性，差在原子性。
2. **去掉外层判空、只留锁内判空，对吗？差在哪？** 功能对。正确性由锁+内层判空保证；外层判空只是快路径（避免每次抢锁），去掉后每次 getInstance 都抢锁、变慢。

## 任务 2.4：synchronized 内置锁

### 三种形式

| 形式 | 锁对象 |
|---|---|
| 实例方法 | this |
| 静态方法 | Class 对象（Xxx.class） |
| 代码块 | 指定对象（粒度最细，推荐） |

### 锁的是对象，不是代码

两段"不同"的代码锁同一对象 → 互斥；锁不同对象 → 不互斥。

**实测复现**：t1 循环调 `incInstance()`（锁 this），t2 循环调 `incBlock()`（锁 dedicatedLock），结果 192485 而非 200000——两把不同的锁互不排斥，`shared++` 照样丢更新。结论：光加锁不够，还得加"同一把"锁。

### 可重入

同一线程可重复获取自己已持有的锁（否则递归同步方法直接自锁）。synchronized 和 ReentrantLock 都支持。

### 锁对象选择铁律

**必须 `private final`、专用、非公开、非 intern 的对象。**

- 不锁 this：this 公开，任何拿到引用的代码都能 `synchronized(obj)` 或调同步方法，锁被外部共享，可能意外竞争/死锁。
- 不锁字符串常量：字符串会被驻留（intern），两个同字面量 `"lock"` 是**同一个对象**；不同模块的 `synchronized("lock")` 全抢同一把锁，甚至与 JDK/三方库内部加锁冲突。
- 粒度：锁专用私有对象通常粒度更小、竞争更少。

### 思考题结论

1. **incInstance（锁 this）与 incStatic（锁 Class）互斥吗？** 不互斥——它们锁的是两个不同对象（this vs Xxx.class）。判断方法间是否互斥，只看是否锁同一对象。同实例的 incInstance 之间会阻塞（同锁 this）；incStatic 之间也会阻塞（同锁 Class，与实例无关）——但这两个事实不改变"incInstance 与 incStatic 不互斥"的答案。
2. **为什么锁专用私有对象而非 this/字符串常量？** this 公开、锁被外部共享；字符串常量被 intern，同字面量全局同对象，跨模块甚至与三方库抢同一把锁。生产要求锁对象 private final + 专用。

## 任务 2.5：CAS 与原子类

### CAS 原理

Compare-And-Swap：CPU 级原子指令"值等于期望才替换，失败则重试（自旋）"。无阻塞、无挂起开销，是 `Atomic*` 家族、`ConcurrentHashMap`、AQS 的地基。

### ABA 问题

值 A→B→A，普通 CAS 觉察不到"变过"。对计数无影响；对"引用复用"敏感场景（无锁链表）用 `AtomicStampedReference`（版本号）识破。

### AtomicLong vs LongAdder（实测对比）

| | AtomicLong | LongAdder |
|---|---|---|
| 结构 | 单值，所有线程 CAS 同一个数 | base + Cell[] 分段累加，读时求和 |
| 高并发写 | 慢（自旋+缓存行乒乓） | 快（热点分散） |
| 读成本 | O(1) 精确 | O(Cell 数) 遍历 |
| sum 语义 | get() 即时精确 | **非原子快照**：与写并发时弱一致；写全部结束后（如 join 之后）是精确值 |

> 易错表述："竞争状态下 sum 不准确"——不准确的原因是"sum 与写**同时发生**"，不是"存在竞争"本身。join 提供 happens-before，之后 sum 精确。

### 思考题结论

1. **CAS 自旋在竞争极激烈时的隐患？** ①CPU 空转重试烧资源；②缓存行乒乓（每次写使其他核缓存失效，一致性协议来回搬同一行，与伪共享同源）；③可能饥饿（某线程一直失败）；④反直觉结论：竞争极激烈时 CAS 可能**比锁还慢**（锁 park 挂起不占 CPU，CAS 站着烧）。LongAdder 分段同时解这几层。
2. **在线计数选 AtomicLong 还是 LongAdder？** 两个维度：**读写比** + **精确性需求**。写多读少（心跳计数、监控面板偶尔读）→ LongAdder；读频繁或需要精确瞬时值做判断（如连接数超限拒绝接入）→ AtomicLong（读 O(1) 且精确）。
