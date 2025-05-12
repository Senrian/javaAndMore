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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.text.DecimalFormat;
import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * MCP server using stdio transport, automatically started by the client.
 * You need to build the server jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */
public class ClientStdio {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat VOLUME_FORMAT = new DecimalFormat("0.0000");

    public static void main(String[] args) {
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
            System.out.println("初始化完成！\n");

            // 列出并显示可用工具
            ListToolsResult toolsList = client.listTools();
            System.out.println("可用工具 = " + toolsList);
            System.out.println("============================================");

            // 基础测试 - 主板股票（上证）
            testStock(client, "600519", "上海股票 - 贵州茅台");

            // 基础测试 - 主板股票（深证）
            testStock(client, "000001", "深圳股票 - 平安银行");

            // 测试创业板股票
            testStock(client, "300750", "创业板股票 - 宁德时代");

            // 测试科创板股票
            testStock(client, "688981", "科创板股票 - 中芯国际");

            // 测试ETF基金
            testStock(client, "510050", "ETF基金 - 50ETF");

            // 错误测试 - 不存在的股票代码
            System.out.println("\n测试不存在的股票代码 (999999):");
            try {
                CallToolResult invalidStockResult = client.callTool(new CallToolRequest("getStockInfo",
                        Map.of("stockCode", "999999")));
                System.out.println("股票信息: " + invalidStockResult);
            } catch (Exception e) {
                System.out.println("预期错误: " + e.getMessage());
            }

            // 错误测试 - 非数字股票代码
            System.out.println("\n测试非数字股票代码 (abc123):");
            try {
                CallToolResult invalidCodeResult = client.callTool(new CallToolRequest("getStockInfo",
                        Map.of("stockCode", "abc123")));
                System.out.println("股票信息: " + invalidCodeResult);
            } catch (Exception e) {
                System.out.println("预期错误: " + e.getMessage());
            }

            // 错误测试 - 位数不足的股票代码
            System.out.println("\n测试位数不足的股票代码 (12345):");
            try {
                CallToolResult insufficientDigitsResult = client.callTool(new CallToolRequest("getStockInfo",
                        Map.of("stockCode", "12345")));
                System.out.println("股票信息: " + insufficientDigitsResult);
            } catch (Exception e) {
                System.out.println("预期错误: " + e.getMessage());
            }

            // 错误测试 - 位数超过的股票代码
            System.out.println("\n测试位数超过的股票代码 (1234567):");
            try {
                CallToolResult excessDigitsResult = client.callTool(new CallToolRequest("getStockInfo",
                        Map.of("stockCode", "1234567")));
                System.out.println("股票信息: " + excessDigitsResult);
            } catch (Exception e) {
                System.out.println("预期错误: " + e.getMessage());
            }

            // 性能测试 - 连续查询多支股票并测量时间
            System.out.println("\n性能测试 - 连续查询多支股票:");
            String[] stocksToTest = {"600519", "000001", "300750", "688981", "601318"};
            Instant start = Instant.now();

            for (String stockCode : stocksToTest) {
                try {
                    CallToolResult result = client.callTool(new CallToolRequest("getStockInfo",
                            Map.of("stockCode", stockCode)));
                    System.out.println("成功获取股票 " + stockCode + " 的信息");
                } catch (Exception e) {
                    System.out.println("获取股票 " + stockCode + " 信息失败: " + e.getMessage());
                }
            }

            Duration elapsedTime = Duration.between(start, Instant.now());
            System.out.println("批量查询 " + stocksToTest.length + " 只股票总耗时: " +
                    elapsedTime.toMillis() + " 毫秒, 平均每只股票: " +
                    (elapsedTime.toMillis() / stocksToTest.length) + " 毫秒");

        } catch (Exception e) {
            System.err.println("测试过程中遇到错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
            System.out.println("\n客户端已关闭");
        }
    }

    /**
     * 测试单个股票代码并以格式化方式显示结果
     */
    private static void testStock(McpSyncClient client, String stockCode, String description) {
        System.out.println("\n测试" + description + " (" + stockCode + "):");
        try {
            Instant start = Instant.now();
            CallToolResult stockResult = client.callTool(new CallToolRequest("getStockInfo",
                    Map.of("stockCode", stockCode)));
            Duration elapsed = Duration.between(start, Instant.now());

            // 从结果字符串中提取JSON部分
            String resultText = stockResult.content().get(0).toString();
            Matcher matcher = JSON_PATTERN.matcher(resultText);

            if (matcher.find()) {
                String jsonStr = matcher.group(0);
                JsonNode stockInfo = objectMapper.readTree(jsonStr);

                // 格式化显示股票信息
                System.out.println("┌─────────────────────────────────────────────┐");
                System.out.printf("│ 股票代码: %-6s   股票名称: %-15s│\n",
                        stockInfo.get("code").asText(),
                        stockInfo.get("name").asText());
                System.out.println("├─────────────────────────────────────────────┤");
                System.out.printf("│ 当前价格: %-8s元                       │\n",
                        PRICE_FORMAT.format(stockInfo.get("currentPrice").asDouble()));
                System.out.printf("│ 今日最高: %-8s元  今日最低: %-8s元 │\n",
                        PRICE_FORMAT.format(stockInfo.get("highPrice").asDouble()),
                        PRICE_FORMAT.format(stockInfo.get("lowPrice").asDouble()));
                System.out.printf("│ 开盘价格: %-8s元                       │\n",
                        PRICE_FORMAT.format(stockInfo.get("openPrice").asDouble()));
                System.out.printf("│ 成交量: %-7s万手   成交额: %-8s亿元 │\n",
                        VOLUME_FORMAT.format(stockInfo.get("volume").asDouble()),
                        VOLUME_FORMAT.format(stockInfo.get("amount").asDouble()));
                System.out.println("└─────────────────────────────────────────────┘");
                System.out.println("查询耗时: " + elapsed.toMillis() + " 毫秒");
            } else {
                System.out.println("股票信息: " + stockResult);
            }
        } catch (Exception e) {
            System.out.println("获取股票信息失败: " + e.getMessage());
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
