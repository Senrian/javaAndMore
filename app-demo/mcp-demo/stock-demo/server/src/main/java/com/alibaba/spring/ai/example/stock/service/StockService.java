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

package com.alibaba.spring.ai.example.stock.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.Serializable;
import java.util.Map;

/**
 * Stock service for retrieving real-time stock information from Eastmoney API.
 * This service provides functionality to fetch stock data including current price,
 * high/low prices, opening price, trading volume, and amount.
 *
 * @author Brian Xiadong
 */
@Service
public class StockService {
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);
    // 使用新浪财经API
    private static final String BASE_URL = "https://hq.sinajs.cn";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final RestClient restClient;

    public StockService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "*/*")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                .defaultHeader("Referer", "https://finance.sina.com.cn")
                .build();
    }

    @JsonSerialize
    public record StockInfo(
            @JsonProperty("code") String code,
            @JsonProperty("name") String name,
            @JsonProperty("currentPrice") Double currentPrice,
            @JsonProperty("highPrice") Double highPrice,
            @JsonProperty("lowPrice") Double lowPrice,
            @JsonProperty("openPrice") Double openPrice,
            @JsonProperty("volume") Double volume,
            @JsonProperty("amount") Double amount
    ) implements Serializable {
    }

    @Tool(name = "getStockInfo", description = "Get real-time stock information for the specified stock code")
    public StockInfo getStockInfo(@ToolParam(description = "股票代码，6位数字") String stockCode) {
        try {
            // 输出参数值，帮助调试
            logger.info("收到股票代码参数: {}", stockCode);
            
            // Validate stock code is not null
            if (stockCode == null) {
                throw new IllegalArgumentException("Stock code cannot be null");
            }
            
            // Validate stock code format
            if (!stockCode.matches("^[0-9]{6}$")) {
                throw new IllegalArgumentException("Stock code must be 6 digits");
            }

            logger.info("Fetching stock information for {}", stockCode);
            
            return fetchRealStockData(stockCode);
        } catch (IllegalArgumentException e) {
            logger.error("Parameter error: {}", e.getMessage());
            throw e;
        }
    }
    
    // 从API获取实际数据
    private StockInfo fetchRealStockData(String stockCode) {
        try {
            // 为股票代码添加市场前缀
            String prefix;
            if (stockCode.startsWith("6") || stockCode.startsWith("5")) {
                prefix = "sh"; // 上证
            } else {
                prefix = "sz"; // 深证
            }
            String symbol = prefix + stockCode;
            
            // 调用新浪API
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/list=" + symbol)
                            .build())
                    .retrieve()
                    .body(String.class);

            logger.info("Raw response: {}", response);

            // 新浪API返回的格式：var hq_str_sh600519="贵州茅台,1811.010,1811.000,1839.000,1849.000,1808.000,1838.990,1839.000,23835138,43665543541.000,200,1838.990,3600,1838.000,3100,1837.000,1200,1836.000,3900,1835.000,100,1839.000,100,1839.990,2600,1840.000,800,1841.000,400,1842.000,100,2023-05-10,15:00:00,00,";
            if (response == null || response.isEmpty() || !response.contains("\"")) {
                throw new IllegalArgumentException("No data found for stock code " + stockCode);
            }
            
            // 从响应中提取股票数据
            int startQuote = response.indexOf("\"");
            int endQuote = response.lastIndexOf("\"");
            
            if (startQuote == -1 || endQuote == -1 || startQuote == endQuote) {
                throw new IllegalArgumentException("Invalid data format for stock code " + stockCode);
            }
            
            String dataStr = response.substring(startQuote + 1, endQuote);
            String[] fields = dataStr.split(",");
            
            if (fields.length < 33) {
                throw new IllegalArgumentException("Insufficient data fields for stock code " + stockCode);
            }
            
            // 解析数据
            // fields[0]: 股票名称
            // fields[1]: 今日开盘价 (Opening Price)
            // fields[2]: 昨日收盘价 (Previous Close)
            // fields[3]: 当前价格 (Current Price)
            // fields[4]: 今日最高价 (Highest Price)
            // fields[5]: 今日最低价 (Lowest Price)
            // fields[8]: 成交量 (Trading Volume) - 手
            // fields[9]: 成交额 (Trading Amount) - 元
            
            String name = fields[0];
            double openPrice = Double.parseDouble(fields[1]);
            double currentPrice = Double.parseDouble(fields[3]);
            double highPrice = Double.parseDouble(fields[4]);
            double lowPrice = Double.parseDouble(fields[5]);
            double volume = Double.parseDouble(fields[8]) / 100.0; // 转换为万手
            double amount = Double.parseDouble(fields[9]) / 100000000.0; // 转换为亿元
            
            logger.info("Parsed data for {}: name={}, price={}", stockCode, name, currentPrice);

            // 返回股票信息对象
            return new StockInfo(
                    stockCode,
                    name,
                    currentPrice,
                    highPrice,
                    lowPrice,
                    openPrice,
                    volume,
                    amount
            );
        } catch (RestClientException e) {
            logger.error("网络请求失败: {}", e.getMessage());
            throw new RuntimeException("Network error - " + e.getMessage());
        } catch (Exception e) {
            logger.error("处理数据失败: {}", e.getMessage());
            throw new RuntimeException("Data processing error - " + e.getMessage());
        }
    }
}

