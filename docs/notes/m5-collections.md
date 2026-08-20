# 模块 5：并发容器（collections）

> 对应 `docs/learning-plan.md` 模块 5。当前已覆盖：任务 5.1。

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
