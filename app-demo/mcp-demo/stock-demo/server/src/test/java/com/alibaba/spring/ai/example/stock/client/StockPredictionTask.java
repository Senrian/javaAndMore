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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.File;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * 股票涨停预测定时任务
 * 在A股开盘时间内定时执行，利用技术指标预测可能涨停的股票
 */
public class StockPredictionTask {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat VOLUME_FORMAT = new DecimalFormat("0.0000");
    
    // 股票池（默认监控的股票代码列表）
    private static final List<String> DEFAULT_STOCK_CODES = loadDefaultStockCodes();
    
    // 存储每支股票的历史数据
    private static final Map<String, List<StockData>> STOCK_HISTORY_MAP = new HashMap<>();
    
    // 存储每支股票的技术指标
    private static final Map<String, TechnicalIndicators> STOCK_INDICATORS_MAP = new HashMap<>();
    
    // 定时任务执行器
    private static ScheduledExecutorService scheduler;
    
    // MCP客户端
    private static McpSyncClient client;
    
    /**
     * 股票数据类
     */
    private static class StockData {
        final String code;
        final String name;
        final double currentPrice;
        final double highPrice;
        final double lowPrice;
        final double openPrice;
        final double volume;
        final double amount;
        final Date date;
        
        StockData(JsonNode node) {
            this.code = node.get("code").asText();
            this.name = node.get("name").asText();
            this.currentPrice = node.get("currentPrice").asDouble();
            this.highPrice = node.get("highPrice").asDouble();
            this.lowPrice = node.get("lowPrice").asDouble();
            this.openPrice = node.get("openPrice").asDouble();
            this.volume = node.get("volume").asDouble();
            this.amount = node.get("amount").asDouble();
            this.date = new Date();
        }
        
        @Override
        public String toString() {
            return String.format("股票[%s-%s] 价格: %.2f元, 最高: %.2f元, 最低: %.2f元, 成交量: %.2f万手",
                    code, name, currentPrice, highPrice, lowPrice, volume);
        }
    }
    
    /**
     * 技术指标类
     */
    private static class TechnicalIndicators {
        String stockCode;
        String stockName;
        
        // 神奇九转指标
        int tdSequenceUp = 0;   // 上涨序列（当日收盘价高于4天前收盘价）
        int tdSequenceDown = 0; // 下跌序列（当日收盘价低于4天前收盘价）
        
        // 量价关系指标
        double volumeRatio = 0; // 量比（当前成交量/过去5日平均成交量）
        double priceChangeRatio = 0; // 价格变化比率
        
        // MACD指标（仅计算基础值，不进行专业技术分析）
        double macdDiff = 0; // DIFF线值
        
        // 存储最近10日的价格和量能数据
        List<Double> priceTrend = new ArrayList<>();
        List<Double> volumeTrend = new ArrayList<>();
        
        // 涨停可能性评分（0-100）
        int breakoutScore = 0;
        
        @Override
        public String toString() {
            return String.format("股票[%s-%s] 上涨序列: %d, 下跌序列: %d, 量比: %.2f, 涨停评分: %d",
                    stockCode, stockName, tdSequenceUp, tdSequenceDown, volumeRatio, breakoutScore);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("股票涨停预测定时任务启动...");
        
        // 初始化股票池
        List<String> stockCodes;
        
        if (args.length > 0) {
            // 如果有命令行参数，使用命令行参数作为股票代码
            stockCodes = Arrays.asList(args);
            System.out.println("使用命令行指定的 " + stockCodes.size() + " 只股票进行监控");
        } else {
            // 尝试从CSV文件加载股票代码
            stockCodes = loadStockCodesFromCSV();
            if (stockCodes.isEmpty()) {
                // 如果CSV文件不存在或加载失败，使用默认股票列表
                stockCodes = DEFAULT_STOCK_CODES;
                System.out.println("未找到CSV文件或加载失败，使用默认股票列表监控 " + stockCodes.size() + " 只股票");
            } else {
                System.out.println("从CSV文件加载了 " + stockCodes.size() + " 只股票进行监控");
            }
        }
        
        // 初始化历史数据和技术指标
        for (String code : stockCodes) {
            STOCK_HISTORY_MAP.put(code, new ArrayList<>());
            STOCK_INDICATORS_MAP.put(code, new TechnicalIndicators());
            STOCK_INDICATORS_MAP.get(code).stockCode = code;
        }
        
        try {
            // 初始化MCP客户端
            initializeClient();
            
            // 启动定时任务
            startScheduledTasks(stockCodes);
            
            // 阻塞主线程，让定时任务继续执行
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("程序执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            shutdown();
        }
    }
    
    /**
     * 加载默认股票代码列表
     */
    private static List<String> loadDefaultStockCodes() {
        List<String> codes = new ArrayList<>();
        String csvPath = "/Users/sixinran/IdeaProjects/javaAndMore/app-demo/mcp-demo/stock-demo/server/src/test/java/com/alibaba/spring/ai/example/stock/client/stocks_hot.csv";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 1) {
                        codes.add(parts[0].trim());
                    }
                }
            }
            System.out.println("已从CSV文件加载 " + codes.size() + " 只股票代码作为默认股票池");
        } catch (Exception e) {
            System.err.println("加载默认股票池时出错: " + e.getMessage());
            // 出错时返回空列表
            return new ArrayList<>();
        }
        
        return codes;
    }
    
    /**
     * 从CSV文件加载股票代码
     */
    private static List<String> loadStockCodesFromCSV() {
        List<String> codes = new ArrayList<>();
        
        try {
            // 在data目录下查找最新的stock_codes文件
            File dataDir = new File("data");
            if (!dataDir.exists() || !dataDir.isDirectory()) {
                System.out.println("data目录不存在，返回默认股票池");
                return DEFAULT_STOCK_CODES;
            }
            
            // 查找匹配的文件
            File[] files = dataDir.listFiles((dir, name) -> name.startsWith("stock_codes_") && name.endsWith(".csv"));
            if (files == null || files.length == 0) {
                System.out.println("未在data目录找到股票代码文件，返回默认股票池");
                return DEFAULT_STOCK_CODES;
            }
            
            // 按文件名排序，选择最新的文件
            Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName()));
            File latestFile = files[0];
            
            System.out.println("从文件加载股票代码: " + latestFile.getAbsolutePath());
            
            // 读取CSV文件，忽略标题行
            try (BufferedReader reader = new BufferedReader(new FileReader(latestFile))) {
                // 跳过标题行
                String line = reader.readLine();
                
                // 读取股票代码
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 1) {
                        codes.add(parts[0].trim());
                    }
                }
            }
            
            System.out.println("成功加载 " + codes.size() + " 只股票代码");
            
        } catch (Exception e) {
            System.err.println("加载CSV文件时出错: " + e.getMessage());
            // 出错时返回默认股票池
            return DEFAULT_STOCK_CODES;
        }
        
        return codes;
    }
    
    /**
     * 初始化MCP客户端
     */
    private static void initializeClient() throws Exception {
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
        client = McpClient.sync(transport).build();

        System.out.println("正在初始化MCP客户端...");
        System.out.println("初始化完成！");
    }
    
    /**
     * 启动定时任务
     */
    private static void startScheduledTasks(List<String> stockCodes) {
        scheduler = Executors.newScheduledThreadPool(3);
        
        // 定义任务启动时间
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 每天早上9:30和下午13:00检查是否交易日
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkTradingDay();
            } catch (Exception e) {
                System.err.println("检查交易日任务异常: " + e.getMessage());
            }
        }, getSecondsUntilNextHour(9, 30), 24 * 60 * 60, TimeUnit.SECONDS);
        
        // 2. 每5s获取一次股票数据并分析
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isTradingTime()) {
                    for (String code : stockCodes) {
                        fetchAndAnalyzeStock(code);
                        // 避免频繁请求
                        TimeUnit.MILLISECONDS.sleep(3000);
                    }
                    
                    // 输出可能涨停的股票
                    predictBreakoutStocks();
                }
            } catch (Exception e) {
                System.err.println("获取股票数据任务异常: " + e.getMessage());
            }
        }, 1, 20, TimeUnit.SECONDS);
        
        // 3. A股收盘后总结当日预测结果
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isAfterMarketClose()) {
                    summarizeDailyPredictions();
                }
            } catch (Exception e) {
                System.err.println("总结预测结果任务异常: " + e.getMessage());
            }
        }, getSecondsUntilNextHour(15, 5), 24 * 60 * 60, TimeUnit.SECONDS);
    }
    
    /**
     * 获取股票数据并进行分析
     */
    private static void fetchAndAnalyzeStock(String stockCode) {
        try {
            // 调用股票API
            CallToolResult result = client.callTool(new CallToolRequest("getStockInfo",
                    Map.of("arg0", stockCode)));
            
            // 解析结果
            String resultText = result.content().get(0).toString();
            Matcher matcher = JSON_PATTERN.matcher(resultText);
            
            if (matcher.find()) {
                String jsonStr = matcher.group(0);
                JsonNode stockInfo = objectMapper.readTree(jsonStr);
                
                // 创建股票数据对象
                StockData data = new StockData(stockInfo);
                
                // 保存历史数据
                List<StockData> history = STOCK_HISTORY_MAP.get(stockCode);
                history.add(data);
                // 只保留最近10条记录
                if (history.size() > 1000) {
                    history.remove(0);
                }
                
                // 更新技术指标
                updateTechnicalIndicators(stockCode, data);
                
            } else {
                System.err.println("解析股票 " + stockCode + " 响应失败");
            }
        } catch (Exception e) {
            System.err.println("获取股票 " + stockCode + " 数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新技术指标
     */
    private static void updateTechnicalIndicators(String stockCode, StockData data) {
        List<StockData> history = STOCK_HISTORY_MAP.get(stockCode);
        TechnicalIndicators indicators = STOCK_INDICATORS_MAP.get(stockCode);
        indicators.stockName = data.name;
        
        // 更新价格和成交量趋势
        indicators.priceTrend.add(data.currentPrice);
        indicators.volumeTrend.add(data.volume);
        if (indicators.priceTrend.size() > 10) {
            indicators.priceTrend.remove(0);
        }
        if (indicators.volumeTrend.size() > 10) {
            indicators.volumeTrend.remove(0);
        }
        
        // 计算价格变化比率
        if (history.size() >= 2) {
            StockData yesterday = history.get(history.size() - 2);
            indicators.priceChangeRatio = (data.currentPrice - yesterday.currentPrice) / yesterday.currentPrice * 100;
        }
        
        // 计算量比
        if (history.size() >= 5) {
            double avgVolume = history.subList(history.size() - 5, history.size()).stream()
                    .mapToDouble(s -> s.volume)
                    .average()
                    .orElse(0);
            indicators.volumeRatio = avgVolume > 0 ? data.volume / avgVolume : 0;
        }
        
        // 更新神奇九转指标 - 上涨序列
        if (history.size() >= 5) {
            StockData fourDaysAgo = history.get(history.size() - 5);
            if (data.currentPrice > fourDaysAgo.currentPrice) {
                indicators.tdSequenceUp++;
                indicators.tdSequenceDown = 0;
            } else {
                indicators.tdSequenceUp = 0;
            }
            
            // 更新神奇九转指标 - 下跌序列
            if (data.currentPrice < fourDaysAgo.currentPrice) {
                indicators.tdSequenceDown++;
                indicators.tdSequenceUp = 0;
            } else {
                indicators.tdSequenceDown = 0;
            }
        }
        
        // 计算涨停可能性评分
        calculateBreakoutScore(indicators, data);
    }
    
    /**
     * 计算涨停可能性评分（0-100分）
     */
    private static void calculateBreakoutScore(TechnicalIndicators indicators, StockData data) {
        int score = 0;
        
        // 神奇九转指标评分（满分25分）
        if (indicators.tdSequenceDown >= 7 && indicators.tdSequenceDown <= 9) {
            // 下跌序列达到7-9，可能即将反转上涨
            score += 25;
        } else if (indicators.tdSequenceDown >= 5 && indicators.tdSequenceDown < 7) {
            // 下跌序列达到5-6，有一定反转可能
            score += 15;
        } else if (indicators.tdSequenceUp == 1 || indicators.tdSequenceUp == 2) {
            // 刚刚开始上涨序列
            score += 20;
        }
        
        // 量价关系评分（满分30分）
        if (indicators.volumeRatio > 3.0) {
            // 量比大于3，成交量显著放大
            score += 30;
        } else if (indicators.volumeRatio > 2.0) {
            // 量比大于2
            score += 20;
        } else if (indicators.volumeRatio > 1.5) {
            // 量比大于1.5
            score += 10;
        }
        
        // 价格变化趋势评分（满分20分）
        if (indicators.priceChangeRatio > 3.0) {
            // 当日价格上涨超过3%
            score += 20;
        } else if (indicators.priceChangeRatio > 1.5) {
            // 当日价格上涨超过1.5%
            score += 10;
        }
        
        // 收盘价接近前期高点评分（满分15分）
        if (indicators.priceTrend.size() >= 5) {
            double maxPrice = Collections.max(indicators.priceTrend);
            double priceToHighRatio = data.currentPrice / maxPrice;
            if (priceToHighRatio > 0.95) {
                // 当前价格接近前期高点
                score += 15;
            } else if (priceToHighRatio > 0.9) {
                score += 10;
            }
        }
        
        // 形态评分（满分10分）
        // 如果有十字星、上影线、下影线等特殊形态，给予额外分数
        if (Math.abs(data.openPrice - data.currentPrice) < 0.1 * (data.highPrice - data.lowPrice)) {
            // 十字星形态
            score += 5;
        }
        if (data.highPrice - data.currentPrice > 2 * (data.currentPrice - data.openPrice) && 
                data.currentPrice > data.openPrice) {
            // 上影线形态
            score += 5;
        }
        
        // 保存评分结果
        indicators.breakoutScore = Math.min(100, score);
    }
    
    /**
     * 预测可能涨停的股票
     */
    private static void predictBreakoutStocks() {

        // 过滤出评分高于60分的股票
        List<TechnicalIndicators> highScoreStocks = STOCK_INDICATORS_MAP.values().stream()
                .filter(i -> i.breakoutScore >= 45)
                .sorted((a, b) -> b.breakoutScore - a.breakoutScore)
                .collect(Collectors.toList());
        
        if (highScoreStocks.isEmpty()) {
            return;
        }
        
        System.out.println("以下股票可能有涨停机会：");
        for (TechnicalIndicators stock : highScoreStocks) {
            String reason = getBreakoutReason(stock);
            System.out.printf("股票[%s-%s] 涨停评分: %d 原因: %s\n", 
                    stock.stockCode, stock.stockName, stock.breakoutScore, reason);
        }
        System.out.println("*****************************");
    }
    
    /**
     * 获取股票可能涨停的原因描述
     */
    private static String getBreakoutReason(TechnicalIndicators indicators) {
        List<String> reasons = new ArrayList<>();
        
        if (indicators.tdSequenceDown >= 7 && indicators.tdSequenceDown <= 9) {
            reasons.add("神奇九转下跌序列(" + indicators.tdSequenceDown + ")接近完成，可能即将反转上涨");
        } else if (indicators.tdSequenceDown >= 5 && indicators.tdSequenceDown < 7) {
            reasons.add("神奇九转下跌序列(" + indicators.tdSequenceDown + ")正在形成，有反转迹象");
        } else if (indicators.tdSequenceUp == 1 || indicators.tdSequenceUp == 2) {
            reasons.add("神奇九转上涨序列刚刚开始(" + indicators.tdSequenceUp + ")，可能有增长动力");
        }
        
        if (indicators.volumeRatio > 3.0) {
            reasons.add("量比大于3，成交量显著放大，市场热情高");
        } else if (indicators.volumeRatio > 2.0) {
            reasons.add("量比大于2，成交量明显增加");
        }
        
        if (indicators.priceChangeRatio > 3.0) {
            reasons.add("价格上涨超过3%，短期动能强劲");
        }
        
        // 如果没有特定原因，提供一个默认说明
        if (reasons.isEmpty()) {
            reasons.add("综合技术指标评分较高");
        }
        
        return String.join("；", reasons);
    }
    
    /**
     * 总结每日预测结果
     */
    private static void summarizeDailyPredictions() {
        System.out.println("\n===== " + getCurrentTimeStamp() + " 当日预测总结 =====");
        
        int totalPredictions = 0;
        int highScorePredictions = 0;
        
        for (TechnicalIndicators indicators : STOCK_INDICATORS_MAP.values()) {
            if (indicators.breakoutScore > 0) {
                totalPredictions++;
                if (indicators.breakoutScore >= 70) {
                    highScorePredictions++;
                    System.out.printf("高评分股票: [%s-%s] 涨停评分: %d\n", 
                            indicators.stockCode, indicators.stockName, indicators.breakoutScore);
                }
            }
        }
        
        System.out.println("当日总计分析股票: " + STOCK_INDICATORS_MAP.size() + " 只");
        System.out.println("有评分预测的股票: " + totalPredictions + " 只");
        System.out.println("高评分(>=70)股票: " + highScorePredictions + " 只");
        System.out.println("明日请继续关注以上高评分股票的表现！");
        System.out.println("==============================");
    }
    
    /**
     * 检查当天是否为交易日
     */
    private static void checkTradingDay() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        
        // 简单判断周末（实际A股还需要结合节假日判断）
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            System.out.println(today + " 是周末，非交易日");
        } else {
            System.out.println(today + " 是工作日，可能为交易日（未考虑节假日）");
            System.out.println("交易时间: 9:30-11:30, 13:00-15:00");
        }
    }
    
    /**
     * 判断当前是否为交易时间
     */
    private static boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        // 9:30-11:30 或 13:00-15:00
        return (now.isAfter(LocalTime.of(9, 29)) && now.isBefore(LocalTime.of(11, 31))) ||
               (now.isAfter(LocalTime.of(12, 59)) && now.isBefore(LocalTime.of(15, 1)));
    }
    
    /**
     * 判断当前是否为收盘后时间
     */
    private static boolean isAfterMarketClose() {
        LocalTime now = LocalTime.now();
        // 15:00之后
        return now.isAfter(LocalTime.of(15, 0));
    }
    
    /**
     * 获取当前时间戳
     */
    private static String getCurrentTimeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 计算距离下一个指定小时分钟的秒数
     */
    private static long getSecondsUntilNextHour(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute));
        if (now.isAfter(nextTime)) {
            nextTime = nextTime.plusDays(1);
        }
        return java.time.Duration.between(now, nextTime).getSeconds();
    }
    
    /**
     * 计算距离下一个N分钟的秒数
     */
    
    /**
     * 获取当前目录下的JAR文件路径
     */
    private static String getCurrentJarPath() {
        return "/Users/sixinran/IdeaProjects/javaAndMore/app-demo/mcp-demo/stock-demo/server/target/starter-stock-server-1.0.0.jar";
    }
    
    /**
     * 关闭资源
     */
    private static void shutdown() {
        System.out.println("正在关闭资源...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        if (client != null) {
            try {
                client.close();
                System.out.println("MCP客户端已关闭");
            } catch (Exception e) {
                System.err.println("关闭MCP客户端时出错: " + e.getMessage());
            }
        }
        
        System.out.println("所有资源已关闭");
    }
} 