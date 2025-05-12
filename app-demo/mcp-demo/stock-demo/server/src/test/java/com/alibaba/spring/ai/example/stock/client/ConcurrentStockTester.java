/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.spring.ai.example.stock.client;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * 并发股票测试工具
 * 用于测试MCP股票服务器在并发请求下的性能和稳定性
 */
public class ConcurrentStockTester {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_THREAD_COUNT = 5;
    private static final int DEFAULT_ITERATIONS = 3;
    
    private static class StockTestResult {
        final String stockCode;
        final long responseTimeMs;
        final boolean success;
        final String errorMessage;
        final Double stockPrice;
        
        StockTestResult(String stockCode, long responseTimeMs, boolean success, 
                Double stockPrice, String errorMessage) {
            this.stockCode = stockCode;
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.stockPrice = stockPrice;
            this.errorMessage = errorMessage;
        }
        
        @Override
        public String toString() {
            return String.format("股票[%s] - %s, 响应时间: %d毫秒%s", 
                    stockCode, 
                    success ? "成功" : "失败", 
                    responseTimeMs,
                    success ? (", 当前价格: " + stockPrice + "元") : (", 错误: " + errorMessage));
        }
    }

    public static void main(String[] args) {
        // 要测试的股票代码列表
        String[] stocksToTest = {
            "600519", // 贵州茅台
            "000001", // 平安银行
            "300750", // 宁德时代
            "688981", // 中芯国际
            "601318", // 中国平安
            "600036", // 招商银行
            "000858", // 五粮液
            "002594", // 比亚迪
            "601899", // 紫金矿业
            "600887"  // 伊利股份
        };
        
        // 解析命令行参数
        int threadCount = DEFAULT_THREAD_COUNT;
        int iterations = DEFAULT_ITERATIONS;
        
        if (args.length >= 1) {
            try {
                threadCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("线程数参数无效，使用默认值: " + DEFAULT_THREAD_COUNT);
            }
        }
        
        if (args.length >= 2) {
            try {
                iterations = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("迭代次数参数无效，使用默认值: " + DEFAULT_ITERATIONS);
            }
        }
        
        System.out.println("股票服务并发测试工具启动");
        System.out.println("线程数: " + threadCount);
        System.out.println("迭代次数: " + iterations);
        System.out.println("测试股票数量: " + stocksToTest.length);
        
        // 初始化MCP客户端
        Instant startTime = Instant.now();
        
        try {
            // 获取当前目录下的JAR文件路径
            String jarPath = getCurrentJarPath();
            System.out.println("使用JAR文件路径: " + jarPath);
            
            var stdioParams = ServerParameters.builder("java")
                    .args("-Dspring.ai.mcp.server.stdio=true",
                            "-Dspring.main.web-application-type=none",
                            "-Dlogging.pattern.console=",
                            "-jar",
                            jarPath)
                    .build();

            var transport = new StdioClientTransport(stdioParams);
            var client = McpClient.sync(transport).build();

            try {
                System.out.println("正在初始化MCP客户端...");
                client.initialize();
                System.out.println("初始化完成！");

                // 列出并显示可用工具
                ListToolsResult toolsList = client.listTools();
                System.out.println("可用工具: " + toolsList);
                System.out.println("============================================");

                // 创建线程池
                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
                
                // 存储所有测试结果
                List<List<StockTestResult>> allIterationResults = new ArrayList<>(iterations);
                
                // 执行多轮测试
                for (int iteration = 0; iteration < iterations; iteration++) {
                    System.out.println("\n开始第 " + (iteration + 1) + " 轮测试");
                    
                    // 记录本轮测试的所有结果
                    List<StockTestResult> iterationResults = Collections.synchronizedList(new ArrayList<>());
                    allIterationResults.add(iterationResults);
                    
                    // 创建一个计数器跟踪完成的请求
                    AtomicInteger completedRequests = new AtomicInteger(0);
                    
                    // 创建所有测试任务
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    
                    // 测试开始时间
                    Instant iterationStart = Instant.now();
                    
                    // 为每个股票创建一个测试任务
                    for (String stockCode : stocksToTest) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            Instant requestStart = Instant.now();
                            try {
                                // 调用股票信息API
                                CallToolResult result = client.callTool(new CallToolRequest("getStockInfo",
                                        Map.of("stockCode", stockCode)));
                                
                                // 计算响应时间
                                long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                                
                                // 从响应中提取股票价格
                                Double stockPrice = extractStockPrice(result);
                                
                                // 记录成功结果
                                iterationResults.add(new StockTestResult(
                                        stockCode, responseTime, true, stockPrice, null));
                                
                            } catch (Exception e) {
                                // 计算响应时间
                                long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                                
                                // 记录失败结果
                                iterationResults.add(new StockTestResult(
                                        stockCode, responseTime, false, null, e.getMessage()));
                            } finally {
                                // 增加完成计数
                                int completed = completedRequests.incrementAndGet();
                                System.out.printf("进度: %d/%d (%.1f%%)\r", 
                                        completed, stocksToTest.length, 
                                        (completed * 100.0 / stocksToTest.length));
                            }
                        }, executorService);
                        
                        futures.add(future);
                    }
                    
                    // 等待所有测试完成
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    
                    // 计算总耗时
                    Duration iterationDuration = Duration.between(iterationStart, Instant.now());
                    
                    // 按股票代码排序结果
                    iterationResults.sort((r1, r2) -> r1.stockCode.compareTo(r2.stockCode));
                    
                    // 显示本轮测试结果
                    System.out.println("\n第 " + (iteration + 1) + " 轮测试结果:");
                    iterationResults.forEach(System.out::println);
                    
                    // 显示统计信息
                    printStatistics(iterationResults, iterationDuration, stocksToTest.length);
                    
                    // 在每轮测试之间稍作暂停，避免连续的高负载
                    if (iteration < iterations - 1) {
                        try {
                            System.out.println("等待2秒后开始下一轮测试...");
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                
                // 显示总体测试汇总
                System.out.println("\n============== 测试汇总 ==============");
                System.out.println("总测试轮数: " + iterations);
                System.out.println("总测试股票数: " + (stocksToTest.length * iterations));
                
                // 计算成功率
                long totalRequests = allIterationResults.stream()
                        .mapToLong(List::size)
                        .sum();
                
                long successfulRequests = allIterationResults.stream()
                        .flatMap(List::stream)
                        .filter(r -> r.success)
                        .count();
                
                System.out.printf("总成功率: %.2f%% (%d/%d)\n", 
                        (successfulRequests * 100.0 / totalRequests),
                        successfulRequests, totalRequests);
                
                // 计算平均响应时间
                DoubleSummaryStatistics responseTimeStats = allIterationResults.stream()
                        .flatMap(List::stream)
                        .mapToDouble(r -> r.responseTimeMs)
                        .summaryStatistics();
                
                System.out.printf("平均响应时间: %.2f毫秒\n", responseTimeStats.getAverage());
                System.out.printf("最短响应时间: %d毫秒\n", (long)responseTimeStats.getMin());
                System.out.printf("最长响应时间: %d毫秒\n", (long)responseTimeStats.getMax());
                
                // 关闭线程池
                executorService.shutdown();
                executorService.awaitTermination(30, TimeUnit.SECONDS);
                
            } finally {
                // 关闭客户端
                client.closeGracefully();
                System.out.println("\n客户端已关闭");
            }
            
        } catch (Exception e) {
            System.err.println("测试过程中遇到错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 计算总测试时间
        Duration totalTestTime = Duration.between(startTime, Instant.now());
        System.out.printf("\n测试完成! 总耗时: %d分%d秒\n", 
                totalTestTime.toMinutes(), 
                totalTestTime.toSecondsPart());
    }
    
    /**
     * 从API响应中提取股票价格
     */
    private static Double extractStockPrice(CallToolResult result) {
        try {
            String text = result.content().get(0).toString();
            JsonNode rootNode = objectMapper.readTree(text);
            return rootNode.get("currentPrice").asDouble();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 打印统计信息
     */
    private static void printStatistics(List<StockTestResult> results, Duration duration, int totalRequests) {
        // 计算成功和失败请求数
        long successCount = results.stream().filter(r -> r.success).count();
        long failureCount = results.size() - successCount;
        
        System.out.println("\n统计信息:");
        System.out.printf("成功率: %.2f%% (%d/%d)\n", 
                (successCount * 100.0 / results.size()), 
                successCount, results.size());
        
        System.out.printf("总耗时: %d毫秒\n", duration.toMillis());
        System.out.printf("平均每请求耗时: %.2f毫秒\n", (duration.toMillis() * 1.0 / totalRequests));
        
        // 计算响应时间统计
        if (!results.isEmpty()) {
            DoubleSummaryStatistics stats = results.stream()
                    .mapToDouble(r -> r.responseTimeMs)
                    .summaryStatistics();
            
            System.out.printf("最短响应时间: %d毫秒\n", (long)stats.getMin());
            System.out.printf("最长响应时间: %d毫秒\n", (long)stats.getMax());
            System.out.printf("平均响应时间: %.2f毫秒\n", stats.getAverage());
        }
    }
    
    /**
     * 获取当前目录下的JAR文件路径
     */
    private static String getCurrentJarPath() {
        try {
            // 优先使用用户目录下的路径
            String userDir = System.getProperty("user.dir");
            System.out.println("当前工作目录: " + userDir);
            
            // 创建可能的JAR路径
            String defaultJarPath = Paths.get(userDir, "target", "starter-stock-server-1.0.0.jar").toString();
            File jarFile = new File(defaultJarPath);
            
            // 检查JAR文件是否存在
            if (jarFile.exists() && jarFile.isFile()) {
                System.out.println("已找到JAR文件: " + defaultJarPath);
                return defaultJarPath;
            } else {
                System.out.println("JAR文件不存在于: " + defaultJarPath);
                
                // 尝试找到备用路径
                String altPath = Paths.get(userDir, "..", "..", "..", "starter-example", "server", "starter-stock-server", "target", "starter-stock-server-1.0.0.jar").normalize().toString();
                File altJarFile = new File(altPath);
                
                if (altJarFile.exists() && altJarFile.isFile()) {
                    System.out.println("找到备用JAR文件: " + altPath);
                    return altPath;
                }
                
                // 显示target目录内容以帮助调试
                File targetDir = new File(Paths.get(userDir, "target").toString());
                if (targetDir.exists() && targetDir.isDirectory()) {
                    System.out.println("target目录内容:");
                    File[] files = targetDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            System.out.println("  - " + file.getName());
                        }
                    }
                }
                
                // 最后才使用硬编码路径
                return "/Users/sixinran/IdeaProjects/spring-ai-alibaba-examples/spring-ai-alibaba-mcp-example/starter-example/server/starter-stock-server/target/starter-stock-server-1.0.0.jar";
            }
        } catch (Exception e) {
            System.err.println("获取JAR路径时出错: " + e.getMessage());
            e.printStackTrace();
            // 使用硬编码路径作为备选
            return "/Users/sixinran/IdeaProjects/spring-ai-alibaba-examples/spring-ai-alibaba-mcp-example/starter-example/server/starter-stock-server/target/starter-stock-server-1.0.0.jar";
        }
    }
} 