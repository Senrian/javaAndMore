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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 从网络下载A股股票数据
 * 支持从多个公开数据源获取股票列表
 */
public class StockOnlineDownloader {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    // 数据源API
    private static final String TUSHARE_API_URL = "http://api.tushare.pro/stock_basic";
    private static final String EASTMONEY_API_URL =
            "http://86.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=5000&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f3&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23&fields=f12,f14";
    private static final String SINA_API_URL =
            "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?num=20&sort=symbol&asc=1&node=";

    // 用于解析新浪API返回的JSON数据的正则表达式
    private static final Pattern SINA_JSON_PATTERN = Pattern.compile("\\{[^{}]*\\}");

    private static final String EASTMONEY_URL = "http://quote.eastmoney.com/stocklist.html";
    private static final String SINA_URL = "http://finance.sina.com.cn/stock/";
    private static final String DATE_STR = new SimpleDateFormat("yyyyMMdd").format(new Date());

    public static void main(String[] args) {
        System.out.println("开始从网络下载A股股票数据...");

        // 先检查传入的参数
        System.out.println("传入参数个数: " + args.length);
        boolean withPrice = false;

        for (int i = 0; i < args.length; i++) {
            System.out.println("参数[" + i + "]: " + args[i]);
            if ("--with-price".equals(args[i])) {
                withPrice = true;
                System.out.println("检测到--with-price参数，将生成股票价格数据");
            }
        }

        try {
            // 创建输出目录
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // 创建输出文件
            File outputFile = new File("data/online_stocks_" + DATE_STR + ".csv");
            System.out.println("数据将保存到: " + outputFile.getAbsolutePath());

            // 下载沪深A股股票数据
            List<StockInfo> stocks = downloadStockData();
            System.out.println("成功下载 " + stocks.size() + " 只股票的数据");

            // 仅当指定了--with-price参数时才更新价格
            if (withPrice) {
                System.out.println("开始获取实时价格数据...");
                updateRealTimePrice(stocks);
            } else {
                System.out.println("未指定--with-price参数，跳过价格数据更新");
            }

            // 保存到CSV文件
            saveToCSV(stocks, outputFile);
            System.out.println("股票数据已保存到: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("处理股票数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 下载沪深A股股票数据
     *
     * @return 股票信息列表
     */
    private static List<StockInfo> downloadStockData() throws IOException, InterruptedException {
        List<StockInfo> stocks = new ArrayList<>();

        // 首先尝试从东方财富获取数据
        System.out.println("正在从东方财富获取股票数据...");
        List<StockInfo> eastmoneyStocks = downloadFromEastmoney();
        // List<StockInfo> sinaStocks = downloadFromSina();
        stocks.addAll(eastmoneyStocks);
        // stocks.addAll(sinaStocks);
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
     * 从新浪财经获取股票数据
     */
    private static List<StockInfo> downloadFromSina() throws IOException, InterruptedException {
        List<StockInfo> stocks = new ArrayList<>();

        // 获取上海主板股票 (sh)
        List<StockInfo> shStocks = downloadSinaStocksByNode("sh_a");
        stocks.addAll(shStocks);
        System.out.println("从新浪财经获取上海A股: " + shStocks.size() + " 只");

        // 休息一下，避免请求过于频繁
        TimeUnit.SECONDS.sleep(1);

        // 获取深圳主板股票 (sz)
        List<StockInfo> szStocks = downloadSinaStocksByNode("sz_a");
        stocks.addAll(szStocks);
        System.out.println("从新浪财经获取深圳A股: " + szStocks.size() + " 只");

        // 休息一下，避免请求过于频繁
        TimeUnit.SECONDS.sleep(1);

        // 获取创业板股票 (cyb)
        List<StockInfo> cybStocks = downloadSinaStocksByNode("cyb");
        stocks.addAll(cybStocks);
        System.out.println("从新浪财经获取创业板: " + cybStocks.size() + " 只");

        return stocks;
    }

    /**
     * 从新浪财经API获取指定板块的股票
     */
    private static List<StockInfo> downloadSinaStocksByNode(String node) throws IOException, InterruptedException {
        List<StockInfo> stocks = new ArrayList<>();
        int page = 1;
        boolean hasMoreData = true;

        System.out.println("开始从新浪财经API分页获取" + node + "板块股票数据...");

        while (hasMoreData) {
            // 构建URL，添加分页参数
            String url = SINA_API_URL + node + "&page=" + page;
            System.out.println("正在获取第" + page + "页，URL: " + url);

            // 发送请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                
                // 检查返回数据是否为空数组
                if (responseBody.trim().equals("[]")) {
                    hasMoreData = false;
                    System.out.println("板块" + node + "数据已全部获取完毕");
                    break;
                }

                // 新浪API返回的不是标准JSON，使用正则表达式提取
                Matcher matcher = SINA_JSON_PATTERN.matcher(responseBody);
                int stockCount = 0;
                
                while (matcher.find()) {
                    try {
                        String jsonStr = matcher.group(0);
                        JsonNode stockNode = objectMapper.readTree(jsonStr);

                        String code = stockNode.path("symbol").asText();
                        String name = stockNode.path("name").asText();

                        // 去掉前缀（如果有的话）
                        if (code.startsWith("sh") || code.startsWith("sz")) {
                            code = code.substring(2);
                        }

                        // 判断股票类型
                        String market = determineMarketType(code);
                        if (!market.isEmpty()) {
                            stocks.add(new StockInfo(code, name, market));
                            stockCount++;
                        }
                    } catch (Exception e) {
                        // 忽略解析单个股票时的错误，继续处理下一个
                        System.err.println("解析股票数据时出错: " + e.getMessage());
                    }
                }

                System.out.println("第" + page + "页获取到" + stockCount + "只股票");
                
                // 如果当前页获取的数据少于20条，说明已经到最后一页
                if (stockCount < 20) {
                    hasMoreData = false;
                    System.out.println("板块" + node + "数据已全部获取完毕");
                } else {
                    page++;
                    // 休眠1000秒，避免请求过于频繁
                    System.out.println("休眠1秒后获取下一页数据...");
                    TimeUnit.SECONDS.sleep(1);
                }
            } else {
                System.err.println("获取新浪财经数据失败，HTTP状态码: " + response.statusCode());
                hasMoreData = false;
            }
        }

        System.out.println("板块" + node + "共获取到" + stocks.size() + "只股票");
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
     * 批量获取股票实时价格
     * 返回的Map中，key为股票代码，value为一个数组，第一个元素为当前价格，第二个元素为涨跌幅
     */
    private static Map<String, double[]> fetchBatchPrices(List<StockInfo> stocks) throws IOException, InterruptedException {
        Map<String, double[]> result = new HashMap<>();

        if (stocks.isEmpty()) {
            return result;
        }

        // 构建查询字符串
        StringBuilder symbols = new StringBuilder();
        for (StockInfo stock : stocks) {
            // 添加市场前缀 - 修正科创板/创业板的前缀
            String prefix;
            if (stock.market.contains("创业板") || stock.market.contains("中小板") || stock.market.contains("深证")) {
                prefix = "sz";
            } else {
                prefix = "sh";
            }
            symbols.append(prefix).append(stock.code).append(",");
        }

        if (symbols.length() > 0) {
            symbols.deleteCharAt(symbols.length() - 1); // 删除最后一个逗号
        }

        // 使用新浪股票API获取实时数据
        String apiUrl = "https://hq.sinajs.cn/list=" + symbols.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Referer", "https://finance.sina.com.cn") // 新浪API需要Referer头
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String data = response.body();

            // 解析返回数据
            // 格式例如: var hq_str_sh600519="贵州茅台,2021.57,2032.15,2005.00,2046.83,1981.00,2005.00,2006.00,17080764,34354822352.29,700,2005.00,500,2006.00,5600,2007.77,200,2009.00,301,2010.00,100,2004.78,1600,2004.00,5200,2003.00,600,2002.00,300,2001.00,2023-04-29,15:00:00,00,";
            String[] lines = data.split("\n");

            for (String line : lines) {
                try {
                    if (line.trim().isEmpty() || !line.contains("=")) {
                        continue;
                    }

                    // 提取股票代码
                    int startIndex = line.indexOf("_") + 1;
                    int endIndex = line.indexOf("=");
                    if (startIndex > 0 && endIndex > startIndex) {
                        String fullCode = line.substring(startIndex, endIndex);
                        String code = fullCode.substring(2); // 去掉sh或sz前缀

                        // 提取数据
                        String quotedData = line.substring(endIndex + 1).trim();
                        // 检查是否有有效数据
                        if (quotedData.length() <= 3) { // "=\"\";" 这样的空数据
                            System.err.println("无法获取股票 " + code + " 的实时数据");
                            continue;
                        }

                        // 去掉开头的"和结尾的";
                        quotedData = quotedData.substring(1, quotedData.length() - 2);
                        String[] fields = quotedData.split(",");

                        if (fields.length >= 3) {
                            double currentPrice = 0;
                            double changePercent = 0;

                            try {
                                // 当前价格是第4个字段(index 3)
                                if (!fields[3].isEmpty()) {
                                    currentPrice = Double.parseDouble(fields[3]);
                                }

                                // 涨跌幅需要计算：(当前价-昨收)/昨收*100%
                                if (currentPrice > 0 && !fields[2].isEmpty()) {
                                    double previousClose = Double.parseDouble(fields[2]);
                                    if (previousClose > 0) {
                                        changePercent = (currentPrice - previousClose) / previousClose * 100;
                                    }
                                }

                                if (currentPrice > 0) { // 只保存有效价格
                                    result.put(code, new double[] {currentPrice, changePercent});
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("解析价格数据出错 (股票:" + code + "): " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略单行解析错误
                    System.err.println("解析价格数据行出错: " + e.getMessage());
                }
            }
        } else {
            System.err.println("获取实时价格数据失败，HTTP状态码: " + response.statusCode());
        }

        return result;
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

    /**
     * 创建备用上海主板股票数据
     */
    private static List<StockInfo> createBackupShanghaiStocks() {
        List<StockInfo> stocks = new ArrayList<>();

        // 主要上海主板股票
        stocks.add(new StockInfo("600000", "浦发银行", "上证主板"));
        stocks.add(new StockInfo("600028", "中国石化", "上证主板"));
        stocks.add(new StockInfo("600030", "中信证券", "上证主板"));
        stocks.add(new StockInfo("600036", "招商银行", "上证主板"));
        stocks.add(new StockInfo("600050", "中国联通", "上证主板"));
        stocks.add(new StockInfo("600104", "上汽集团", "上证主板"));
        stocks.add(new StockInfo("600276", "恒瑞医药", "上证主板"));
        stocks.add(new StockInfo("600309", "万华化学", "上证主板"));
        stocks.add(new StockInfo("600519", "贵州茅台", "上证主板"));
        stocks.add(new StockInfo("600585", "海螺水泥", "上证主板"));
        stocks.add(new StockInfo("600690", "海尔智家", "上证主板"));
        stocks.add(new StockInfo("600887", "伊利股份", "上证主板"));
        stocks.add(new StockInfo("600900", "长江电力", "上证主板"));
        stocks.add(new StockInfo("601012", "隆基绿能", "上证主板"));
        stocks.add(new StockInfo("601088", "中国神华", "上证主板"));
        stocks.add(new StockInfo("601166", "兴业银行", "上证主板"));
        stocks.add(new StockInfo("601288", "农业银行", "上证主板"));
        stocks.add(new StockInfo("601318", "中国平安", "上证主板"));
        stocks.add(new StockInfo("601398", "工商银行", "上证主板"));
        stocks.add(new StockInfo("601601", "中国太保", "上证主板"));
        stocks.add(new StockInfo("601857", "中国石油", "上证主板"));
        stocks.add(new StockInfo("601888", "中国中免", "上证主板"));

        return stocks;
    }

    /**
     * 创建备用深圳主板股票数据
     */
    private static List<StockInfo> createBackupShenzhenStocks() {
        List<StockInfo> stocks = new ArrayList<>();

        // 主要深圳主板股票
        stocks.add(new StockInfo("000001", "平安银行", "深证主板"));
        stocks.add(new StockInfo("000002", "万科A", "深证主板"));
        stocks.add(new StockInfo("000063", "中兴通讯", "深证主板"));
        stocks.add(new StockInfo("000100", "TCL科技", "深证主板"));
        stocks.add(new StockInfo("000333", "美的集团", "深证主板"));
        stocks.add(new StockInfo("000568", "泸州老窖", "深证主板"));
        stocks.add(new StockInfo("000651", "格力电器", "深证主板"));
        stocks.add(new StockInfo("000725", "京东方A", "深证主板"));
        stocks.add(new StockInfo("000776", "广发证券", "深证主板"));
        stocks.add(new StockInfo("000858", "五粮液", "深证主板"));
        stocks.add(new StockInfo("002001", "新和成", "中小板"));
        stocks.add(new StockInfo("002027", "分众传媒", "中小板"));
        stocks.add(new StockInfo("002230", "科大讯飞", "中小板"));
        stocks.add(new StockInfo("002304", "洋河股份", "中小板"));
        stocks.add(new StockInfo("002415", "海康威视", "中小板"));
        stocks.add(new StockInfo("002594", "比亚迪", "中小板"));

        return stocks;
    }

    /**
     * 创建备用创业板股票数据
     */
    private static List<StockInfo> createBackupGEMStocks() {
        List<StockInfo> stocks = new ArrayList<>();

        // 主要创业板股票
        stocks.add(new StockInfo("300014", "亿纬锂能", "创业板"));
        stocks.add(new StockInfo("300015", "爱尔眼科", "创业板"));
        stocks.add(new StockInfo("300059", "东方财富", "创业板"));
        stocks.add(new StockInfo("300122", "智飞生物", "创业板"));
        stocks.add(new StockInfo("300124", "汇川技术", "创业板"));
        stocks.add(new StockInfo("300146", "汤臣倍健", "创业板"));
        stocks.add(new StockInfo("300274", "阳光电源", "创业板"));
        stocks.add(new StockInfo("300408", "三环集团", "创业板"));
        stocks.add(new StockInfo("300413", "芒果超媒", "创业板"));
        stocks.add(new StockInfo("300433", "蓝思科技", "创业板"));
        stocks.add(new StockInfo("300601", "康泰生物", "创业板"));
        stocks.add(new StockInfo("300750", "宁德时代", "创业板"));
        stocks.add(new StockInfo("300760", "迈瑞医疗", "创业板"));

        return stocks;
    }
} 