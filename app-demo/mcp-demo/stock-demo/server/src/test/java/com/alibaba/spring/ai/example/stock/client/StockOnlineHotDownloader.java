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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 从网络下载A股股票数据
 * 支持从多个公开数据源获取股票列表
 * 现在只从东方财富获取数据，并将未包含在stocks_hot.csv中的股票添加到该文件末尾
 * 支持定时任务功能，默认每60秒执行一次
 */
public class StockOnlineHotDownloader {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    // 数据源API
    private static final String EASTMONEY_API_URL =
            "http://86.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=5000&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f3&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23&fields=f12,f14";
    
    private static final String DATE_STR = new SimpleDateFormat("yyyyMMdd").format(new Date());
    private static final String HOT_STOCKS_CSV = "Users/sixinran/IdeaProjects/javaAndMore/app-demo/mcp-demo/stock-demo/server/src/test/java/com/alibaba/spring/ai/example/stock/client/stocks_hot.csv";
    
    // 定时任务相关变量
    private static final int DEFAULT_SCHEDULE_SECONDS = 10;
    private static boolean withPrice = false;
    private static boolean runAsScheduleTask = true;
    private static int scheduleSeconds = DEFAULT_SCHEDULE_SECONDS;

    public static void main(String[] args) {
        // 先检查传入的参数
        System.out.println("传入参数个数: " + args.length);
        
        for (int i = 0; i < args.length; i++) {
            System.out.println("参数[" + i + "]: " + args[i]);
            if ("--with-price".equals(args[i])) {
                withPrice = true;
                System.out.println("检测到--with-price参数，将生成股票价格数据");
            } else if ("--schedule".equals(args[i])) {
                runAsScheduleTask = true;
                System.out.println("检测到--schedule参数，将以定时任务模式运行");
                
                // 检查是否有指定间隔时间
                if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
                    scheduleSeconds = Integer.parseInt(args[i + 1]);
                    i++; // 跳过下一个参数，因为已经处理了
                    System.out.println("定时任务间隔设置为 " + scheduleSeconds + " 秒");
                } else {
                    System.out.println("未指定间隔时间，使用默认值 " + DEFAULT_SCHEDULE_SECONDS + " 秒");
                }
            }
        }

        if (runAsScheduleTask) {
            runAsScheduledTask();
        } else {
            runOnce();
        }
    }
    
    /**
     * 以定时任务方式运行，每隔指定时间执行一次
     */
    private static void runAsScheduledTask() {

        // 创建定时任务执行器
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // 先立即执行一次
        System.out.println("首次执行...");
        runOnce();
        
        // 然后每隔指定时间执行一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n执行定时任务... 当前时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                runOnce();
            } catch (Exception e) {
                System.err.println("定时任务执行出错: " + e.getMessage());
                e.printStackTrace();
            }
        }, scheduleSeconds, scheduleSeconds, TimeUnit.SECONDS);
        
        // 添加钩子在程序退出时关闭任务
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("关闭定时任务...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("定时任务未能在5秒内正常关闭，将强制关闭");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }));
        
        System.out.println("定时任务已启动，按Ctrl+C停止程序");
    }
    
    /**
     * 执行一次下载和处理操作
     */
    private static void runOnce() {
        try {
            // 创建输出目录
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // 读取已有的热门股票数据
            Set<String> existingStockCodes = new HashSet<>();
            List<StockInfo> existingStocks = readExistingStocks();
            for (StockInfo stock : existingStocks) {
                existingStockCodes.add(stock.code);
            }
            System.out.println("已从stocks_hot.csv读取 " + existingStocks.size() + " 只股票");

            // 下载东方财富股票数据
            List<StockInfo> newStocks = downloadFromEastmoney();
            System.out.println("从东方财富成功下载 " + newStocks.size() + " 只股票的数据");

            // 筛选出未包含在stocks_hot.csv中的股票
            List<StockInfo> stocksToAppend = new ArrayList<>();
            for (StockInfo stock : newStocks) {
                if (!existingStockCodes.contains(stock.code)) {
                    stocksToAppend.add(stock);
                }
            }
            System.out.println("筛选出 " + stocksToAppend.size() + " 只需要追加的股票");

            // 仅当指定了--with-price参数时才更新价格
            if (withPrice) {
                System.out.println("开始获取实时价格数据...");
                updateRealTimePrice(stocksToAppend);
            } else {
                System.out.println("未指定--with-price参数，跳过价格数据更新");
            }

            // 追加新股票到stocks_hot.csv
            if (!stocksToAppend.isEmpty()) {
                appendToHotStocksCSV(stocksToAppend);
                System.out.println("新股票数据已追加到: " + HOT_STOCKS_CSV);
            } else {
                System.out.println("没有新的股票需要追加");
            }

            // 保存完整数据到CSV文件
            File outputFile = new File("data/online_stocks_" + DATE_STR + ".csv");
            saveToCSV(existingStocks, outputFile);
            System.out.println("完整股票数据已保存到: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("处理股票数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 读取已有的stocks_hot.csv文件
     */
    private static List<StockInfo> readExistingStocks() throws IOException {
        List<StockInfo> stocks = new ArrayList<>();
        File file = new File(HOT_STOCKS_CSV);
        
        if (!file.exists()) {
            System.out.println("stocks_hot.csv不存在，将创建新文件");
            return stocks;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 跳过标题行
                }
                
                if (line.trim().isEmpty()) {
                    continue; // 跳过空行
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    String market = parts[2].trim();
                    
                    StockInfo stock = new StockInfo(code, name, market);
                    
                    // 如果有价格数据，也读取进来
                    if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                        try {
                            stock.currentPrice = Double.parseDouble(parts[3].trim());
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                    
                    // 如果有涨跌幅数据，也读取进来
                    if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                        try {
                            String changeStr = parts[4].trim();
                            if (changeStr.endsWith("%")) {
                                changeStr = changeStr.substring(0, changeStr.length() - 1);
                            }
                            stock.changePercent = Double.parseDouble(changeStr);
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                    
                    stocks.add(stock);
                }
            }
        }
        
        return stocks;
    }

    /**
     * 从东方财富网获取股票数据
     */
    private static List<StockInfo> downloadFromEastmoney() throws IOException, InterruptedException {
        List<StockInfo> stocks = new ArrayList<>();

        // 发送请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EASTMONEY_API_URL))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode dataNode = rootNode.path("data");
            JsonNode stocksNode = dataNode.path("diff");

            if (stocksNode.isArray()) {
                for (JsonNode stockNode : stocksNode) {
                    String code = stockNode.path("f12").asText();
                    String name = stockNode.path("f14").asText();

                    // 判断股票类型
                    String market = determineMarketType(code);
                    if (!market.isEmpty()) {
                        stocks.add(new StockInfo(code, name, market));
                    }
                }
            }
        }

        return stocks;
    }

    /**
     * 根据股票代码判断市场类型
     */
    private static String determineMarketType(String code) {
        if (code == null || code.length() != 6) {
            return "";
        }

        // 上海主板: 60开头
        if (code.startsWith("60")) {
            return "上证主板";
        }
        // 上海科创板: 688开头
        else if (code.startsWith("688")) {
            return "科创板";
        }
        // 深圳主板: 00开头
        else if (code.startsWith("00")) {
            return "深证主板";
        }
        // 深圳中小板: 002开头
        else if (code.startsWith("002")) {
            return "中小板";
        }
        // 创业板: 300开头
        else if (code.startsWith("300")) {
            return "创业板";
        }
        // 北交所: 8开头
        else if (code.startsWith("8")) {
            return "北交所";
        }
        // 其他代码
        return "";
    }

    /**
     * 保存股票数据到CSV文件
     */
    private static void saveToCSV(List<StockInfo> stocks, File outputFile) throws IOException {
        System.out.println("保存股票数据到CSV文件...");
        System.out.println("总共处理 " + stocks.size() + " 只股票");

        int stocksWithPrice = 0;

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            // 写入CSV标题
            writer.println("代码,名称,市场,当前价格,涨跌幅");

            // 写入股票数据
            for (StockInfo stock : stocks) {
                // 再次检查价格数据是否存在
                if (stock.currentPrice > 0) {
                    stocksWithPrice++;
                }

                String priceInfo = stock.currentPrice > 0 ? String.format("%.2f", stock.currentPrice) : "";
                String changeInfo = stock.changePercent != 0 ? String.format("%.2f%%", stock.changePercent) : "";
                writer.println(stock.code + "," + stock.name + "," + stock.market + "," + priceInfo + "," + changeInfo);
            }
        }

        System.out.println("CSV保存完成，其中 " + stocksWithPrice + " 只股票有价格数据");
    }

    /**
     * 将新股票追加到stocks_hot.csv文件
     */
    private static void appendToHotStocksCSV(List<StockInfo> stocks) throws IOException {
        if (stocks.isEmpty()) {
            System.out.println("没有新股票需要追加");
            return;
        }

        System.out.println("正在将 " + stocks.size() + " 只新股票追加到stocks_hot.csv...");
        
        try (FileWriter writer = new FileWriter(HOT_STOCKS_CSV, true)) {
            for (StockInfo stock : stocks) {
                String priceInfo = stock.currentPrice > 0 ? String.format("%.2f", stock.currentPrice) : "";
                String changeInfo = stock.changePercent != 0 ? String.format("%.2f%%", stock.changePercent) : "";
                writer.write(stock.code + "," + stock.name + "," + stock.market + "," + priceInfo + "," + changeInfo + "\n");
            }
        }
        
        System.out.println("追加完成");
    }

    /**
     * 获取实时股票价格
     */
    private static void updateRealTimePrice(List<StockInfo> stocks) {
        if (stocks.isEmpty()) {
            return;
        }

        System.out.println("进入updateRealTimePrice方法，股票数量: " + stocks.size());

        try {
            // 直接使用模拟数据
            System.out.println("开始调用generateRandomPriceData生成随机价格数据...");

            // 检查stocks列表中的第一只股票
            if (!stocks.isEmpty()) {
                StockInfo firstStock = stocks.get(0);
                System.out.println("第一只股票信息 - 代码: " + firstStock.code + ", 名称: " + firstStock.name);
            }

            generateRandomPriceData(stocks);

            // 验证数据
            int stocksWithPrice = 0;
            for (StockInfo stock : stocks) {
                if (stock.currentPrice > 0) {
                    stocksWithPrice++;
                    if (stocksWithPrice <= 5) {  // 只输出前5只有价格的股票，避免输出过多
                        System.out.printf("股票[%d]: %s (%s), 价格: %.2f, 涨跌幅: %.2f%%\n",
                                stocksWithPrice, stock.name, stock.code, stock.currentPrice, stock.changePercent);
                    }
                }
            }
            System.out.println("验证: " + stocksWithPrice + " 只股票有价格数据");
        } catch (Exception e) {
            System.err.println("更新股票价格过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("实时价格数据获取完成");
    }

    /**
     * 生成随机价格数据用于测试和演示
     */
    private static void generateRandomPriceData(List<StockInfo> stocks) {
        System.out.println("正在为" + stocks.size() + "只股票生成随机价格数据...");
        Random random = new Random();
        int count = 0;

        try {
            for (StockInfo stock : stocks) {
                // 基础价格 - 根据股票代码生成一个基础价格 (10-200之间)
                double basePrice = 10 + (Integer.parseInt(stock.code.replaceAll("[^0-9]", "")) % 19) * 10;

                // 添加随机波动 (-5% 到 +5%)
                double fluctuation = (random.nextDouble() * 10 - 5) / 100;
                double finalPrice = basePrice * (1 + fluctuation);

                // 保留两位小数
                finalPrice = Math.round(finalPrice * 100) / 100.0;

                // 设置价格
                stock.currentPrice = finalPrice;

                // 设置涨跌幅 (-3% 到 +3%)
                stock.changePercent = (random.nextDouble() * 6 - 3);
                stock.changePercent = Math.round(stock.changePercent * 100) / 100.0;

                count++;

                // 输出前10只股票的详细信息
                if (count <= 10) {
                    System.out.printf("生成价格数据 - 股票[%d]: %s (%s), 基础价格: %.2f, 最终价格: %.2f, 涨跌幅: %.2f%%\n",
                            count, stock.name, stock.code, basePrice, stock.currentPrice, stock.changePercent);
                }
            }

            System.out.println("成功为" + count + "只股票生成了随机价格数据");
        } catch (Exception e) {
            System.err.println("生成随机价格数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 股票信息类
     */
    private static class StockInfo {
        final String code;
        final String name;
        final String market;
        double currentPrice;
        double changePercent;

        StockInfo(String code, String name, String market) {
            this.code = code;
            this.name = name;
            this.market = market;
            this.currentPrice = 0;
            this.changePercent = 0;
        }
    }
} 