# CompletableFuture 专项练习

> 承接 `docs/learning-plan.md` 任务 4.3。共 7 道梯度题 + 1 道综合大题 + 2 道加分题。
> 约定：包名 `com.learn.concurrent.exercises.cf`，每题一个类，运行方式同主计划：
> `mvn -q compile exec:java -Dexec.mainClass=com.learn.concurrent.exercises.cf.ExN类名`
> 老规矩：先预测输出 → 运行对照 → 思考题写答案。全部完成后可对照文末"自查要点"。

## 知识点地图（做题前扫一遍）

### 心智模型：盒子与纸条（练习 1 前必读）

- **CompletableFuture 是"一个未来才有值的盒子"**：`supplyAsync(任务)` 瞬间返回一个空盒子，任务跑完盒子自动被填上。
- **thenXxx 是给盒子贴纸条**："填上之后请帮我做这件事"。贴纸条的动作瞬间完成，执行发生在"盒子被填上"那一刻。每贴一张返回一个**新盒子**，所以能串成链。
- **join() 是"等盒子填好再往下走"**：main 若不等，程序可能在盒子填完前就退出了。

```java
CompletableFuture.supplyAsync(() -> "26.5")  // 盒1：将来装 "26.5"
        .thenApply(s -> s + "°C")            // 盒2：将来装 "26.5°C"
        .thenAccept(System.out::println)     // 空盒：帮我打印
        .join();                             // main 等收尾
```

**四类操作：**

| 类别 | API | 一句话 |
|---|---|---|
| 提交 | supplyAsync / runAsync | 丢任务进池 |
| 加工 | thenApply(map) / thenCompose(flatMap) | 单路变换 |
| 合并 | thenCombine(两路) / allOf(多路等全) / anyOf(多路竞速) | 多路汇聚 |
| 兜底 | exceptionally / handle / whenComplete / orTimeout / getNow | 异常与超时 |

**三条隐规则（练习 1、6、7 会踩到）：**

1. 非异步回调（thenApply 等**不带 Async 后缀**）由"**完成上一阶段的线程**"执行；若调用时上一阶段已完成，则由**当前调用线程**执行。
2. 带 Async 后缀不传池 → ForkJoinPool.commonPool()；传池 → 指定池。
3. 异步阶段的异常一律包成 CompletionException；orTimeout 内部是 TimeoutException。

---

## 练习 1（热身）：遥测报文解析链

**场景：** 收到 JSON 报文串，异步解析温度、判断阈值、打印；10% 报文损坏需丢弃不中断整体。

**要求：** supplyAsync → thenApply 解析 → thenApply 判阈值 → thenAccept 打印；损坏报文用 exceptionally 打"丢弃"。

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;

public class Ex1TelemetryParse {
    public static void main(String[] args) {
        String[] packets = {"{\"temp\":26.5}", "{\"temp\":31.2}", "###损坏###",
                "{\"temp\":28.0}", "{\"temp\":35.7}"};
        // TODO: 对每个 packet 构建 CF 链：
        //  supplyAsync: 模拟网络接收（sleep 50ms），返回 packet
        //  thenApply:   解析温度（损坏报文这里抛 RuntimeException("bad packet")）
        //  thenApply:   温度 > 30 ? 打"告警:" + t : 打"正常:" + t —— 用 thenAccept 也行
        //  exceptionally: 打 "[丢弃] " + e.getMessage()，返回 null
        // 最后 allOf(...).join() 等全部完成
    }
}
```

**验收：** 5 条处理完；31.2 与 35.7 打告警；损坏包打 [丢弃]；程序正常退出。
**思考：** exceptionally 放在链中间和放在链尾，保护范围有什么区别？

---

## 练习 2：thenCompose 防套娃——设备详情聚合

**场景：** 设备名 → 异步查设备 ID → 拿 ID 异步查配置 → 组装 DeviceInfo。

**要求：** 先**故意用 thenApply** 写一遍，观察返回类型变成 `CompletableFuture<CompletableFuture<...>>`（编译器报错或类型套娃）；再改 thenCompose 拍平。

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ex2Compose {
    record DeviceInfo(int id, String config) { }

    static CompletableFuture<Integer> fetchIdByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);                      // 模拟查注册表
            return name.hashCode() & 7;
        });
    }

    static CompletableFuture<String> fetchConfigById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);                      // 模拟查配置中心
            return "cfg-v2-" + id;
        });
    }

    public static void main(String[] args) {
        // TODO 1: 用 thenApply 组合两步，观察类型套娃（这行会编译报错或类型不对，体会后注释掉）
        // TODO 2: 用 thenCompose 重写，得到 CompletableFuture<DeviceInfo>
        // TODO 3: 打印最终 DeviceInfo
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

**验收：** 输出一行 DeviceInfo[id=.., config=cfg-v2-..]；类型无套娃。
**思考：** 练习 1 的 thenApply 链为什么不用换成 thenCompose？（提示：下一步是不是"又一个异步调用"）

---

## 练习 3：thenCombine——双路合并与"max 而非 sum"

**场景：** 一路查设备在线状态（300ms），一路查电量（100ms），合并打印摘要。两路**互不依赖**，同时发出去。

**数据流（盒子图）：**

```
left:  CF<String>   ──(sleep 300)──→ "在线"
right: CF<Integer>  ──(sleep 100)──→ 87
              ↓ thenCombine((status, battery) -> 拼接) ↓
        CF<String> = "状态=在线, 电量=87%"
```

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;

public class Ex3Combine {
    public static void main(String[] args) {
        long start = System.nanoTime();

        CompletableFuture<String> left = CompletableFuture.supplyAsync(() -> {
            sleep(300);                        // 模拟查在线状态
            return "在线";
        });
        CompletableFuture<Integer> right = null;   // ← 你来写：supplyAsync，sleep(100) → 87

        String summary = left
                .thenCombine(right, (status, battery) -> null)  // ← 你来写：拼 "状态=xx, 电量=xx%"
                .join();                       // 汇合点：两路都完成才返回
        System.out.println(summary);
        System.out.println("总耗时 ≈ " + (System.nanoTime() - start) / 1_000_000 + "ms");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

> `thenCombine(other, (v1, v2) -> r)`：两个盒子的值**各占一个参数**，两路都完成时触发一次。

**验收：** 摘要正确；**总耗时 ≈ 300ms（两路的 max）而不是 400ms（sum）**——用实测数字证明两路并发。
**思考：** 把两路改成串行（left.thenCombine(right) 且 right 在 combine 里创建），耗时变多少？什么时候"合并"其实没并发？

---

## 练习 4：anyOf 竞速——三网关冗余下发

**场景：** 同一指令发 3 台冗余网关（耗时随机 100~500ms），**谁先回用谁**；其余忽略。

**数据流（盒子图）：**

```
gw1: CF<String> ──(随机延迟)──→ "gw-1: ack-ok"
gw2: CF<String> ──(随机延迟)──→ "gw-2: ack-ok"
gw3: CF<String> ──(随机延迟)──→ "gw-3: ack-ok"
        ↓ anyOf（第一个完成的赢）↓
   CF<Object> → 打印（需自行转 String）
```

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Ex4AnyOf {
    public static void main(String[] args) {
        long start = System.nanoTime();

        CompletableFuture<String> gw1 = CompletableFuture.supplyAsync(
                () -> sendVia("gw-1", 100 + (long) (Math.random() * 400)));
        CompletableFuture<String> gw2 = null;   // ← 你来写：同款 gw-2
        CompletableFuture<String> gw3 = null;   // ← 你来写：同款 gw-3

        CompletableFuture<Object> first = CompletableFuture.anyOf(gw1, gw2, gw3)
                .orTimeout(1, TimeUnit.SECONDS);   // 整体兜底
        System.out.println("最先返回: " + first.join());   // anyOf 泛型是 Object
        System.out.println("总耗时 ≈ " + (System.nanoTime() - start) / 1_000_000 + "ms");
    }

    static String sendVia(String gateway, long delayMs) {
        try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return gateway + ": ack-ok";
    }
}
```

> anyOf 返回 CF<Object>：三路类型可能各不相同，编译器只能取公共父类，用时自己转。

**验收：** 输出的网关一定是"延迟最短"的那台；总耗时 ≈ 最小延迟 + ε。
**思考：** ①竞速结束后，落败的两路任务还在跑吗？能被取消吗？（呼应 7.3 思考题）②anyOf 全部失败会怎样？

---

## 练习 5：超时降级——orTimeout + handle 默认值

**场景：** 查设备配置最多等 500ms，超时**不报错**，降级用默认配置 `cfg-default`。

**数据流（盒子图）：**

```
fetchConfig(200ms):  CF<String> ──200ms 完成──→ "cfg-v1-real" ──orTimeout(500) 不触发──→ handle → 真实值
fetchConfig(1000ms): CF<String> ──500ms 超时！──→ 异常 ──handle──→ "cfg-default"
```

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Ex5TimeoutFallback {
    static CompletableFuture<String> fetchConfig(long delayMs) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(delayMs);
            return "cfg-v1-real";
        });
    }

    public static void main(String[] args) {
        // 注意：handle 返回的还是 CF<String>（装着"兜底后的值"），打印前要 join
        String fast = fetchConfig(200)
                .orTimeout(500, TimeUnit.MILLISECONDS)
                .handle((v, e) -> null)          // ← 你来写：e != null ? "cfg-default" : v
                .join();
        System.out.println("快路径: " + fast);

        String slow = null;                      // ← 你来写：同款，delay 改 1000ms
        System.out.println("慢路径: " + slow);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

**验收：** 快路径拿真实值，慢路径拿 `cfg-default`，两轮都不抛异常。
**变体（做完必试）：** 把慢路径换成 `completeOnTimeout("cfg-default", 500, MILLISECONDS)`——输出相同但**没有异常日志**。再让 fetchConfig 故意 `throw new RuntimeException("network down")` 对比：completeOnTimeout 只兜超时，业务异常照样抛（需补 exceptionally）。结论：**要不要"感知超时发生了"是 orTimeout+handle 与 completeOnTimeout 的分水岭**。
**思考：** handle 与 exceptionally 的区别？（handle 拿到"值或异常"两种情况、必须返回值能改写正常结果；exceptionally 只在异常时介入）——这是面试高频。

---

## 练习 6：延迟重试——delayedExecutor

**场景：** 指令下发 30% 失败；失败后**延迟 200ms** 自动重试一次；两次都失败才算失败。

**数据流（盒子图）：**

```
first: CF<Boolean> ──成功(true)──→ completedFuture(true) 直接完成
         │失败(false)
         └→ supplyAsync(重试, delayedExecutor(200ms)) ─→ CF<Boolean>（延迟 200ms 后才起跑）
```

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture.delayedExecutor;
import java.util.concurrent.TimeUnit;

public class Ex6DelayedRetry {
    static boolean sendOnce(String cmd) {
        sleep(100);
        return Math.random() > 0.3;
    }

    public static void main(String[] args) {
        // TODO:
        //  CompletableFuture<Boolean> first = supplyAsync(() -> sendOnce("sync"))
        //  first.thenCompose(ok -> ok
        //      ? CompletableFuture.completedFuture(true)
        //      : CompletableFuture.supplyAsync(() -> sendOnce("sync-retry"),
        //              delayedExecutor(200, TimeUnit.MILLISECONDS)))   // ★ 延迟重试的关键
        //  打印最终成功/失败与总耗时（成功一次 ≈100ms；重试过 ≈ 400ms）
    }
}
```

**验收：** 多跑几次：有时 ~100ms 成功；有时 ~400ms（第一次失败 + 200ms 延迟 + 重试）；耗时能反推出走了哪条路径。
**思考：** ①`completedFuture(true)` 在这里起什么作用？（把"已就绪的值"包成 CF，统一类型）②这版异步重试与 7.3 要写的"lambda 里 send || send"同步重试，各有什么优劣？

---

## 练习 7（观察题）：回调到底在哪个线程执行？

**场景：** 不写业务，只打印每步的线程名。**先填预测表，再运行对照。**

```java
package com.learn.concurrent.exercises.cf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ex7WhichThread {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "io-pool"); t.setDaemon(true); return t;
        });

        tag("① supplyAsync(pool)", CompletableFuture.supplyAsync(() -> {
            show(); sleep(200); return 1;                    // 任务本体
        }, pool).thenApply(v -> { show(); return v + 1; })); // 回调 A：紧跟着注册

        CompletableFuture<Integer> done = CompletableFuture.supplyAsync(() -> {
            sleep(300); return 1;
        }, pool);
        done.join();                                          // 先等它完成
        done.thenApply(v -> { show(); return v + 1; });       // 回调 B：完成后才注册

        tag("② thenApplyAsync(不带池)", CompletableFuture.supplyAsync(() -> 1, pool)
                .thenApplyAsync(v -> { show(); return v + 1; }));   // 回调 C

        pool.shutdown();
    }

    static void show() { System.out.println("    跑在: " + Thread.currentThread().getName()); }
    static void tag(String s, Object ignored) { System.out.println(s); }
    static void sleep(long ms) { try { Thread.sleep(ms);} catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
```

**先预测再运行（把你的预测写在这里，运行后核对）：**

| 回调 | 我的预测 | 实际 |
|---|---|---|
| ① supplyAsync 任务本体 | | |
| ① 回调 A（紧跟注册） | | |
| ① 回调 B（完成后注册） | | |
| ② 回调 C（thenApplyAsync 不带池） | | |

**预期答案（做完再看）：** ①本体 io-pool；A io-pool（完成者执行）；B main（已完成→调用者执行）；C ForkJoinPool 的 commonPool-worker。
**思考：** 生产启示——为什么"回调链里夹一段慢逻辑"可能拖慢整个 io-pool？（非异步回调跑在完成者线程上）

---

## 综合大题：设备影子批量刷新 pipeline

**场景：** 20 台设备。流程：①并发查在线状态（离线 30% 概率，直接标记跳过）；②在线的下发 sync 指令（100~400ms，单台 1s 超时降级）；③失败的进入延迟重试队列（延迟 200ms 重试一次，练习 6 复用）；④聚合输出：在线数 / 首次成功 / 重试成功 / 最终失败。**全程不阻塞 main（最后一个 join 收尾）**，线程池显式。

**要求：** 只给需求不给骨架——自己设计方法签名。硬性约束：

- 每台设备的流程是一条完整 CF 链（thenCompose 串联"查状态→下发→重试"）；
- 全部设备 `allOf` 聚合后再统一打印统计（统计可用 AtomicInteger）；
- 单台任何异常都必须被链内消化（handle/exceptionally），不允许打栈到控制台。

**验收：** ①输出四行统计且 20 = 在线 + 离线；②总耗时明显小于"20 台串行"的理论值；③控制台无异常栈；④连跑 5 次统计数字波动但等式守恒。

---

## 加分题（无骨架，检验综合能力）

**A. 异步缓存（Caffeine AsyncCache 思想）：** 用 `ConcurrentHashMap<String, CompletableFuture<Config>>` 实现 `getAsync(key, loader)`：同 key 并发请求只触发一次 loader（提示：computeIfAbsent 返回同一个 CF，后来者直接 join 它）；loader 失败要把该 CF 从 map 里移除（否则"永久缓存了一次失败"）。与 7.5 同步版对照。

**B. 容错版 allOf 工具：** 写泛型方法 `<T> CompletableFuture<List<T>> allOfSafely(List<CompletableFuture<T>> cfs)`——全部完成（无论成败）后返回各 future 的结果列表（失败的用 null 占位）。提示：allOf 后逐个 `join()` 会有问题吗？（异常已用 handle 兜底的话不会——想清楚为什么。）

---

## 自查要点（全部做完后对照）

1. thenApply/thenCompose/thenCombine 三兄弟能脱口而出"map/flatMap/zip"吗？
2. exceptionally vs handle vs whenComplete 三者分工？（异常兜底可改值 / 值和异常都能改 / 只旁观不改）
3. 非异步回调"由完成者线程执行、已完成则由调用者执行"这条规则，练习 7 里哪两行是证据？
4. anyOf 竞速后落败任务是否继续跑、能否取消？
5. delayedExecutor 延迟重试 vs lambda 内同步重试的取舍？
6. 为什么所有练习的 supplyAsync 都显式传了池（或不传池但任务极快）？——回看 m4 笔记"commonPool 灾难组合"。

完成后把你的答案/统计输出发我，我来批改。
