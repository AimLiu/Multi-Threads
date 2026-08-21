# 模块 7：物联网场景实战（scenarios）

> 对应 `docs/learning-plan.md` 模块 7。当前进行中：任务 7.1。

## 任务 7.1：设备会话管理器

### 实测故障：代码对但"功能整体消失"（隐藏考题）

现象：验收 3 count=5（期望 1），fd-1 存活正常、验收 1/2 全过——**唯独调度器调用的 evict 没生效**。

诊断三问：①控制台有无下线日志？②有无异常堆栈（**evict 忘 try-catch → 任务抛异常 → 定时任务静默取消全部后续**，6.3 坑的实战版）？③Rebuild Project 重跑（旧 class 残留）。

教训：**故障在"只有调度器调用"的代码路径里时，症状是功能整体消失而非报错**。定时任务两件套：方法体 try-catch 全包 + 加扫描开始日志（`[扫描] N 个会话`）让它显形。

### register：check-then-act 的 NPE 竞态

`containsKey → get → set` 之间条目可能被删，get 返回 null → NPE。正解 **compute 两分支合一**：

```java
sessions.compute(deviceId, (k, s) -> {
    if (s == null) return new DeviceSession(k);      // 不存在 → 建
    s.lastActiveAt = System.currentTimeMillis();     // 存在 → 刷心跳
    return s;
});
```

五连口诀应用：register（两分支都处理）→ compute；heartbeat（仅已存在）→ computeIfPresent。

### evict 误删竞态（本题核心）

**心跳更新字段，保护不了条目本身**：扫描读到超时旧值 → 判断该踢 → 判断与 remove 之间心跳刷新了时间戳 → 照样被踢 = **设备刚证明活着却被踢 → 僵尸会话**（设备侧心跳 true，服务端已删）。

三个候选为什么都不彻底：removeIf / iterator.remove() / remove(k,v) 都是"先判断后删"，**窗口缩小但仍在**。唯一彻底解 = **判断与删除合并在 computeIfPresent 里（返回 null 即原子删除）**：

```java
for (String deviceId : sessions.keySet()) {
    sessions.computeIfPresent(deviceId, (k, s) ->
            now - s.lastActiveAt > evictAfterMillis ? null : s);
}
```

原理：compute 系 lambda 在**桶锁内**执行，heartbeat 的 computeIfPresent 锁同一桶 → 两者串行化：要么心跳先进（已新，保留），要么剔除先进（删除，heartbeat 返回 false 触发重连）。**判断与删除之间零窗口。**

### 其他

- unregister 用 `sessions.remove(id) != null` 判断，避免对不在线设备误打日志。
- 扫描循环外捕获 now 一次即可（age 只会变老，不影响正确性）。

### 思考题结论

1. **remove 前要不要再校验时间戳？** 要。心跳只更新字段不能阻止条目被删（判断→remove 窗口内的心跳会被误杀）。且重验必须与删除同一原子操作（computeIfPresent 返回 null），否则重验后仍有窗口。
2. **removeIf / iterator.remove / remove(k,v) 哪个能避免误删？** 都不能彻底避免（都是先判断后删，窗口变窄）。彻底解：computeIfPresent 条件删除（桶锁串行化判断与删除）。
