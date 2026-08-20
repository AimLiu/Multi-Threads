package com.learn.concurrent.exercises;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mafeifei
 * @email: owntnow@163.com
 * @Date: 2026/8/20 14:54
 * @Description:
 */

public class Ex1TelemetryParse {
    public static void main(String[] args) {
        String[] packets = {"{\"temp\":26.5}", "{\"temp\":31.2}", "###损坏###",
                "{\"temp\":28.0}", "{\"temp\":35.7}"};
        // 每个报文一条链，收集到 List<CompletableFuture<Void>>
        List<CompletableFuture<Void>> chains = new ArrayList<>();
        for (String packet : packets) {
            CompletableFuture<Void> chain =
                    CompletableFuture.supplyAsync(() -> {
                                // 步骤1：模拟网络接收——sleep(50)，返回 packet
                                try {
                                    TimeUnit.MILLISECONDS.sleep(50);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException("interrupted", e);
                                }
                                return packet;                    // ← 你来写
                            })
                            .thenApply(p -> {
                                // 步骤2：解析温度——p 形如 {"temp":26.5}
                                if(p.contains("temp")) {
                                    String currTemp = p.split(":")[1];
                                    return Double.parseDouble(p.replaceAll("[^0-9.]", ""));
                                }
                                // 提示：p.contains("temp") 为 false 就 throw new RuntimeException("bad packet")
                                // 为 true 就 Double.parseDouble(p.replaceAll("[^0-9.]", ""))
                                throw new RuntimeException("bad packet");                      // ← 你来写（返回解析出的温度）
                            })
                            .thenApply(t -> {
                                // 步骤3：判阈值——t > 30 打 "告警:" + t，否则打 "正常:" + t；返回 t（继续传）
                                if (t > 30.0){
                                    System.out.println("当前温度过高：" + t);
                                }else{
                                    System.out.println("当前温度正常：" + t);
                                }
                                return t;                         // ← 你来写
                            })
                            .exceptionally(e -> {
                                // 步骤4：兜底——打 "[丢弃] " + e.getMessage()，返回 null
                                System.out.println("[丢弃] " + e.getMessage());
                                return 0.0;                      // ← 你来写
                            })
                            .thenAccept(t -> {                    // 步骤5：可省——这里其实不需要再消费
                            });
            chains.add(chain);
        }
         // 等全部
        CompletableFuture.allOf(chains.toArray(new CompletableFuture[0])).join();
    }
}
