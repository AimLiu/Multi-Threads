# 模块 5：并发容器（collections）

> 对应 `docs/learning-plan.md` 模块 5。当前已覆盖：任务 5.1 ~ 5.3（模块收官）。

## 任务 5.1：ConcurrentHashMap

### 原子复合操作五连（选型口诀）

| API | key 不存在时 | 适用 |
|---|---|---|
| put | 直接放入 | **新值不依赖旧值**（无条件覆盖） |
| merge(k, v, f) | 旧值当 null | **依赖旧值的累加/计数**（compute 的累加特化版） |
| compute(k, f) | old 为 null，自行决定 | 依赖旧值的**复杂逻辑**；lambda 返回 null = 删除 |
| computeIfAbsent | 执行 loading 放入 | **只对不存在的 key**（首次注册） |
| computeIfPresent | 不动 | **只对已存在的 key**（心跳只对在线设备生效） |

场景速答：设备 lastActiveAt 更新 → **put**（新时间戳不依赖旧值）；消息计数 → merge；注册会话 → computeIfAbsent；已注册才刷心跳 → computeIfPresent。

### computeIfAbsent 的原子性（Q1 精解）

- **loading 函数 = 第二个参数 lambda**（key 不存在时才调用它装载初始值）。
- **锁粒度 = 单个桶**：`synchronized(桶首节点)`。不同桶的写完全并行——CHM 高并发的根源（8.2 源码：synchronized(f)）。
- **原子性的含义**：从"查 key"到"装载放入"一体完成，不存在"两个线程都发现 key 不在、都执行 loading"的窗口（7.5 缓存防击穿靠它）。
- **代价（标红）**：loading 在锁内执行，**期间整个桶被锁**，同桶其他 key 的写也被挡。→ loading 里禁慢操作（查库/网络）、禁操作同一 map（自锁）。轻量构造没问题。
- 精确边界："同一桶的操作互斥且原子；不同桶之间无关系也不需要"。

### 禁 null 与弱一致

- key/value 都禁 null：并发下 get 到 null 无法区分"不存在"与"值就是 null"（HashMap 单线程下可以区分，所以允许）。
- 迭代器**弱一致**：不抛 ConcurrentModificationException，但不保证看到迭代期间的修改。
- size() 是估算（CounterCell 分段统计，LongAdder 同思想——8.2/8.4）。

### HashMap 并发写 = 未定义行为（对照实验）

实测：并发写 HashMap 出现异常/丢数据。JDK 7 时代还有扩容链表成环 → get 死循环的著名 bug（头插法）；JDK 8 改尾插后大幅缓解但**并发写依然不安全**。症状随机：丢条目 / ConcurrentModificationException / size 错乱。

工程结论：排查"偶发丢数据/诡异异常"时第一问——**这里是不是有并发写的 HashMap**。

### 思考题结论

1. **computeIfAbsent 原子吗？锁什么？** 原子（桶首节点 synchronized，粒度=单桶，不同桶并行）。loading 函数即第二参 lambda，在锁内执行——原子性来源，也是"loading 必须轻量"的原因。
2. **lastActiveAt 用 put/merge/compute？** put——新值不依赖旧值。merge 是累加场景、compute 是复杂逻辑场景、computeIfPresent 是"仅对已存在 key"场景。

## 任务 5.2：BlockingQueue 家族

### 四组 API（入队/出队 × 四种脾气）

| | 抛异常 | 返回特殊值 | 一直阻塞 | 超时放弃 |
|---|---|---|---|---|
| 入队 | add | offer(e) | **put** | offer(e, t, u) |
| 出队 | remove | **poll** | **take** | poll(t, u) |

### 实现类选型

- `ArrayBlockingQueue`：有界、数组、**单锁双条件**（3.4 手写版）。
- `LinkedBlockingQueue`：**两把锁**（putLock/takeLock）+ 两个 Condition，存取可并行（吞吐更高）；默认容量 Integer.MAX_VALUE。
- `SynchronousQueue`：零容量直接交接（newCachedThreadPool 用）。
- `PriorityBlockingQueue` / `DelayQueue`：见下面对照。

### PriorityBlockingQueue vs DelayQueue（易混）

| | PriorityBlockingQueue | DelayQueue |
|---|---|---|
| 出队依据 | 优先级（compareTo/Comparator） | 到期时间 |
| 元素实现 | Comparable | Delayed（getDelay + compareTo） |
| 锚点 | **谁重要谁先出** | **没到点谁也别出** |
| 场景 | 告警分级：紧急>重要>一般 | 失败指令延迟重试、超时单 |

坑：优先级相同的元素间出队顺序不保证（堆非稳定）；要 FIFO 就把序号编进 compareTo。

### DelayQueue 精解

- 结构：按 **deadline（到期时刻）** 排的优先级堆——不是按"延迟时长"存放。`getDelay()` 返回剩余时间（递减），`compareTo` 比"谁先到期"。
- take：堆顶到期 → 取出；没到期 → **leader/follower 精准等待**（只有 leader 按剩余时间 awaitNanos，其余无限等，避免群体反复醒来看表）。

### 无界队列为什么是隐患（Q1 深挖）

不传容量 = Integer.MAX_VALUE，堆积 OOM 只是表象。**真正的隐蔽性：渐变型故障**——put 永不阻塞 → 背压失效 → 数据默默堆积（系统看着正常）→ 到警戒线 OOM 一次性爆掉，**没有任何中间信号**。有界队列满时 put 阻塞不是缺点，是系统在喊"消费跟不上了"。

连带伤害（呼应 6.1）：队列不满，线程池**永远不扩容到 max**——无界队列废掉线程池弹性，一个坑变两个。生产铁律：**队列必须有界 + 拒绝/阻塞策略明确**。

### 毒丸（poison pill）的局限（Q3 + 实测踩坑）

实测：删掉毒丸 → uploader 阻塞在空队列 take 上无限等待 → join 不返回 → 后续方法永不执行。诊断姿势：看最后一行输出是谁打的 + jstack 看 WAITING 栈顶。

局限四条：①投毒失败（put 抛异常）则消费者停不下；②容易忘（人为失误高发）；③多消费者要投 N 颗、不能动态扩缩；④**污染数据语义**——魔法值与合法数据共享通道，一处漏判就是 bug（实测中失效的 `!= -1` 判断即活例），且只能"全停"。

生产主流替代：**shutdown + 中断组合**——volatile running 标志位 → interrupt 阻塞中的消费者 → catch InterruptedException 后检查标志退出。能停单个线程、区分关闭与异常、不污染数据（7.2 再遇）。

### 思考题结论

1. **LBQ 不传容量？** Integer.MAX_VALUE 无界，渐变型 OOM（背压失效、无中间信号），且连带废掉线程池扩容。必须有界。
2. **告警分级选哪个队列？** PriorityBlockingQueue（元素实现 Comparable）。不是 DelayQueue——那是"到点才能处理"的场景。
3. **毒丸局限？** 投毒失败停不下、易忘、多消费者 N 颗且不动态、污染数据语义只能全停。替代：标志位 + 中断。

## 任务 5.3：CopyOnWriteArrayList 与无锁队列

### COW 机制（实测观察确认）

写 = 复制整个底层数组 → 新数组尾部加元素/删元素 → 新数组替换旧数组（array 引用一换）。迭代器是快照：创建时拿住当时的数组引用，遍历期间不受后续写影响 → 永不抛 ConcurrentModificationException。读零锁。

### COW 的两个成本维度（灾难场景精确定位）

1. **写放大**：add O(n)——10 万元素的列表每秒写 100 次就开始疼；写吞吐随规模线性恶化。
2. **瞬时双倍内存**：复制期间新旧数组并存，大列表一次写可触发 Full GC。

IoT 例：10 万设备、每分钟几十次上下线 → COW 灾难 → 换 ConcurrentHashMap.keySet() 或读写锁。

### COW 是通用思想（不只是 Java 集合）

- **OS 的 fork()**：父子共享物理页，写时才复制**那一页**（缺页中断）——粒度=页。
- 数据库 MVCC 快照、Redis BGSAVE 同思想。
- **JDK 的 COW 集合是粗粒度版**：每次写复制**整个数组**（哪怕只加一个元素）。写极少读极多时总成本被海量读摊薄，粗粒度也够用。

### 无锁队列 linkLast 源码导读（ConcurrentLinkedDeque 尾插）

**JUC 无锁结构配方：状态 + CAS 竞争 + 失败重来**（与 CF 的"结果槽+纸条"同一家族）。

```java
restartFromTail:
for (;;)                        // ① CAS 自旋骨架：失败重来
    for (Node t = tail, p = t, q;;) {   // ② 从 tail 出发
        // ③ 追尾：tail 只是提示值(hint)不保证精确，从它走到真正的尾
        //    "每两跳看一次表"=性能与新鲜度折中
        // ④ 哨兵：p.prev == p（自指）→ 链不可信，跳回外层重来
        // ⑤ 接链（核心）：
        PREV.set(newNode, p);
        if (NEXT.compareAndSet(p, null, newNode)) {  // ★ 线性化点
            if (p != t)
                TAIL.weakCompareAndSet(this, t, newNode);  // ⑥ 尽力推进 tail，失败无所谓
            return;
        }
        // CAS 输给别人 → 重读重来
    }
}
```

三个要点：

- **线性化点**：`NEXT.cas(p, null, newNode)` 成功的那一纳秒，元素才真正存在。互斥不靠锁，靠"同一 CAS 只能赢一个"。
- **tail 允许过期**：⑥推进 tail 失败也 OK，因为③会追尾——**用最终一致换掉精确维护 tail 的竞争**（weakCAS 还是弱内存序版本，更便宜）。
- 呼应链：2.5 CAS 自旋 → CF 结果槽 → 此处链表接驳 → 8.1 AQS，同一配方。

`ConcurrentLinkedQueue.size()` 是 O(n) 遍历，别在热路径调。

### COW vs 读写锁 ArrayList（选型分水岭）

- **要快照式遍历**（遍历需一致完整视图，不许中间态）→ COW：迭代器天生快照、读零锁。
- **列表大且写非零** → 读写锁：写原地 O(1) 不复制；代价是读过锁。
- 数据形态本是 map（deviceId → meta）→ 直接 **ConcurrentHashMap**，锁粒度更细（桶级 vs 全表级）。

**选型顺序：先问数据形态，再问读写比，最后问迭代语义。**"空间换时间"不是这题的本质——读写锁省的是**写成本**（O(1) vs O(n) 复制）。

### 思考题结论

1. **COW 什么时候是灾难？** 写远多于读：写放大 O(n)/次 + 瞬时双倍内存 → GC 压力。规模×写频一起恶化。
2. **元数据缓存选 COW 还是读写锁 ArrayList？** 要快照遍历选 COW；列表大且写非零选读写锁；map 形态直接 CHM。
