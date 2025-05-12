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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * 股票数据导出工具
 * 用于导出股票信息到CSV文件，便于后续分析
 */
public class StockDataExporter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private static class StockData {
        final String code;
        final String name;
        final double currentPrice;
        final double highPrice;
        final double lowPrice;
        final double openPrice;
        final double volume;
        final double amount;
        
        StockData(JsonNode node) {
            this.code = node.get("code").asText();
            this.name = node.get("name").asText();
            this.currentPrice = node.get("currentPrice").asDouble();
            this.highPrice = node.get("highPrice").asDouble();
            this.lowPrice = node.get("lowPrice").asDouble();
            this.openPrice = node.get("openPrice").asDouble();
            this.volume = node.get("volume").asDouble();
            this.amount = node.get("amount").asDouble();
        }
        
        String toCsvLine() {
            return String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%.4f,%.4f",
                    code, name, currentPrice, highPrice, lowPrice, openPrice, volume, amount);
        }
    }
    
    public static void main(String[] args) {
        String outputDir = "data";
        List<String> stockCodes = new ArrayList<>();
        
        // 检查命令行参数
        if (args.length == 0) {
            System.out.println("请提供至少一个股票代码作为参数");
            System.out.println("用法: java StockDataExporter [股票代码1] [股票代码2] ...");
            System.out.println("例如: java StockDataExporter 600519 000001 300750");
            
            // 使用默认股票列表
            stockCodes.addAll(Arrays.asList("600519", "000001", "300750", "688981", "601318"));
            System.out.println("使用默认股票列表: " + String.join(", ", stockCodes));
        } else {
            stockCodes.addAll(Arrays.asList(args));
        }
        
        // 确保输出目录存在
        try {
            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("创建输出目录: " + dirPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("创建目录失败: " + e.getMessage());
            outputDir = "."; // 如果创建失败，使用当前目录
        }
        
        // 创建输出文件名
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String csvFileName = outputDir + File.separator + "stock_data_" + timestamp + ".csv";
        
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

            System.out.println("正在初始化MCP客户端...");
            client.initialize();
            System.out.println("初始化完成！");
            
            System.out.println("开始查询 " + stockCodes.size() + " 只股票数据...");
            
            // 创建CSV文件并写入头部
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
                // 写入CSV文件头
                writer.println("股票代码,股票名称,当前价格,最高价,最低价,开盘价,成交量(万手),成交额(亿元)");
                
                // 存储查询结果
                Map<String, StockData> stockDataMap = new HashMap<>();
                int successCount = 0;
                int failCount = 0;
                
                // 查询每只股票
                for (String stockCode : stockCodes) {
                    System.out.printf("查询股票: %s (%d/%d)\r", 
                            stockCode, (successCount + failCount + 1), stockCodes.size());
                    
                    try {
                        // 调用股票API
                        CallToolResult result = client.callTool(new CallToolRequest("getStockInfo",
                                Map.of("stockCode", stockCode)));
                        
                        // 解析结果
                        String resultText = result.content().get(0).toString();
                        Matcher matcher = JSON_PATTERN.matcher(resultText);
                        
                        if (matcher.find()) {
                            String jsonStr = matcher.group(0);
                            JsonNode stockInfo = objectMapper.readTree(jsonStr);
                            
                            // 创建股票数据对象
                            StockData data = new StockData(stockInfo);
                            stockDataMap.put(stockCode, data);
                            
                            // 写入CSV
                            writer.println(data.toCsvLine());
                            
                            successCount++;
                        } else {
                            System.err.println("解析股票 " + stockCode + " 响应失败");
                            failCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("查询股票 " + stockCode + " 失败: " + e.getMessage());
                        failCount++;
                    }
                    
                    // 添加短暂延迟，避免频繁请求
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // 显示汇总信息
                System.out.println("\n\n数据导出完成！");
                System.out.println("成功查询: " + successCount + " 只股票");
                System.out.println("查询失败: " + failCount + " 只股票");
                System.out.println("CSV文件已保存至: " + new File(csvFileName).getAbsolutePath());
                
                // 简单数据分析
                if (!stockDataMap.isEmpty()) {
                    System.out.println("\n简单数据分析:");
                    
                    // 找出最高价和最低价的股票
                    StockData highestPriceStock = null;
                    StockData lowestPriceStock = null;
                    
                    for (StockData data : stockDataMap.values()) {
                        if (highestPriceStock == null || data.currentPrice > highestPriceStock.currentPrice) {
                            highestPriceStock = data;
                        }
                        
                        if (lowestPriceStock == null || data.currentPrice < lowestPriceStock.currentPrice) {
                            lowestPriceStock = data;
                        }
                    }
                    
                    if (highestPriceStock != null) {
                        System.out.printf("最高价股票: %s (%s), 价格: %.2f元\n", 
                                highestPriceStock.name, highestPriceStock.code, highestPriceStock.currentPrice);
                    }
                    
                    if (lowestPriceStock != null) {
                        System.out.printf("最低价股票: %s (%s), 价格: %.2f元\n", 
                                lowestPriceStock.name, lowestPriceStock.code, lowestPriceStock.currentPrice);
                    }
                }
            }
            
            // 关闭客户端
            client.closeGracefully();
            
        } catch (Exception e) {
            System.err.println("程序执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
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