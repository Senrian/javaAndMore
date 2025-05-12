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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建A股主板和创业板股票代码CSV文件
 * 使用离线数据，避免爬虫抓取可能存在的限制
 */
public class StockCodeDownloader {

    // 主要股票市场类型
    private static final String SH_MAIN = "上证主板";
    private static final String SZ_MAIN = "深证主板";
    private static final String GEM = "创业板";

    public static void main(String[] args) {
        System.out.println("开始生成A股股票代码CSV文件...");
        
        try {
            // 创建输出目录
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdir();
            }
            
            // 创建输出文件
            String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
            File outputFile = new File(dataDir, "stock_codes_" + dateStr + ".csv");
            System.out.println("数据将保存到: " + outputFile.getAbsolutePath());
            
            // 准备股票数据
            List<StockInfo> stockList = new ArrayList<>();
            
            // 添加上海主板股票
            stockList.addAll(createShanghaiMainBoardStocks());
            System.out.println("已添加上证主板股票: " + stockList.size() + "只");
            
            // 添加深圳主板股票
            List<StockInfo> szMainStocks = createShenzhenMainBoardStocks();
            stockList.addAll(szMainStocks);
            System.out.println("已添加深证主板股票: " + szMainStocks.size() + "只");
            
            // 添加创业板股票
            List<StockInfo> gemStocks = createGEMStocks();
            stockList.addAll(gemStocks);
            System.out.println("已添加创业板股票: " + gemStocks.size() + "只");
            
            // 保存到CSV文件
            saveToCSV(stockList, outputFile);
            
            System.out.println("生成完成，共包含 " + stockList.size() + " 只股票");
            System.out.println("文件已保存到: " + outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("生成股票代码文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建上海证券交易所主板股票列表
     */
    public static List<StockInfo> createShanghaiMainBoardStocks() {
        List<StockInfo> stocks = new ArrayList<>();
        
        // 上海主板主要股票（60开头）
        Map<String, String> shMainStocks = new HashMap<>();
        // 大盘蓝筹股

        
        // 转换为股票信息对象列表
        for (Map.Entry<String, String> entry : shMainStocks.entrySet()) {
            stocks.add(new StockInfo(entry.getKey(), entry.getValue(), SH_MAIN));
        }
        
        return stocks;
    }
    
    /**
     * 创建深圳证券交易所主板股票列表
     */
    public static List<StockInfo> createShenzhenMainBoardStocks() {
        List<StockInfo> stocks = new ArrayList<>();
        
        // 深圳主板主要股票（00开头）
        Map<String, String> szMainStocks = new HashMap<>();
        szMainStocks.put("000001", "平安银行");
        szMainStocks.put("000002", "万科A");
        szMainStocks.put("000063", "中兴通讯");
        szMainStocks.put("000066", "中国长城");
        szMainStocks.put("000069", "华侨城A");
        szMainStocks.put("000100", "TCL科技");
        szMainStocks.put("000157", "中联重科");
        szMainStocks.put("000333", "美的集团");
        szMainStocks.put("000338", "潍柴动力");
        szMainStocks.put("000402", "金融街");
        szMainStocks.put("000413", "东旭光电");
        szMainStocks.put("000423", "东阿阿胶");
        szMainStocks.put("000425", "徐工机械");
        szMainStocks.put("000538", "云南白药");
        szMainStocks.put("000568", "泸州老窖");
        szMainStocks.put("000596", "古井贡酒");
        szMainStocks.put("000625", "长安汽车");
        szMainStocks.put("000651", "格力电器");
        szMainStocks.put("000661", "长春高新");
        szMainStocks.put("000703", "恒逸石化");
        szMainStocks.put("000708", "中信特钢");
        szMainStocks.put("000725", "京东方A");
        szMainStocks.put("000768", "中航西飞");
        szMainStocks.put("000776", "广发证券");
        szMainStocks.put("000786", "北新建材");
        szMainStocks.put("000858", "五粮液");
        szMainStocks.put("000876", "新希望");
        szMainStocks.put("000895", "双汇发展");
        szMainStocks.put("000938", "紫光股份");
        szMainStocks.put("000961", "中南建设");
        szMainStocks.put("000963", "华东医药");
        szMainStocks.put("002001", "新和成");
        szMainStocks.put("002007", "华兰生物");
        szMainStocks.put("002024", "苏宁易购");
        szMainStocks.put("002027", "分众传媒");
        szMainStocks.put("002032", "苏泊尔");
        szMainStocks.put("002050", "三花智控");
        szMainStocks.put("002142", "宁波银行");
        szMainStocks.put("002230", "科大讯飞");
        szMainStocks.put("002236", "大华股份");
        szMainStocks.put("002241", "歌尔股份");
        szMainStocks.put("002271", "东方雨虹");
        szMainStocks.put("002304", "洋河股份");
        szMainStocks.put("002352", "顺丰控股");
        szMainStocks.put("002410", "广联达");
        szMainStocks.put("002415", "海康威视");
        szMainStocks.put("002475", "立讯精密");
        szMainStocks.put("002594", "比亚迪");
        szMainStocks.put("002714", "牧原股份");
        
        // 转换为股票信息对象列表
        for (Map.Entry<String, String> entry : szMainStocks.entrySet()) {
            stocks.add(new StockInfo(entry.getKey(), entry.getValue(), SZ_MAIN));
        }
        
        return stocks;
    }
    
    /**
     * 创建创业板股票列表
     */
    public static List<StockInfo> createGEMStocks() {
        List<StockInfo> stocks = new ArrayList<>();
        
        // 创业板主要股票（300开头）
        Map<String, String> gemStocks = new HashMap<>();
        gemStocks.put("300003", "乐普医疗");
        gemStocks.put("300014", "亿纬锂能");
        gemStocks.put("300015", "爱尔眼科");
        gemStocks.put("300033", "同花顺");
        gemStocks.put("300059", "东方财富");
        gemStocks.put("300070", "碧水源");
        gemStocks.put("300122", "智飞生物");
        gemStocks.put("300124", "汇川技术");
        gemStocks.put("300133", "华策影视");
        gemStocks.put("300142", "沃森生物");
        gemStocks.put("300144", "宋城演艺");
        gemStocks.put("300146", "汤臣倍健");
        gemStocks.put("300212", "易华录");
        gemStocks.put("300223", "北京君正");
        gemStocks.put("300226", "上海钢联");
        gemStocks.put("300271", "华宇软件");
        gemStocks.put("300274", "阳光电源");
        gemStocks.put("300285", "国瓷材料");
        gemStocks.put("300316", "晶盛机电");
        gemStocks.put("300347", "泰格医药");
        gemStocks.put("300357", "我武生物");
        gemStocks.put("300363", "博腾股份");
        gemStocks.put("300408", "三环集团");
        gemStocks.put("300413", "芒果超媒");
        gemStocks.put("300433", "蓝思科技");
        gemStocks.put("300450", "先导智能");
        gemStocks.put("300454", "深信服");
        gemStocks.put("300496", "中科创达");
        gemStocks.put("300498", "温氏股份");
        gemStocks.put("300529", "健帆生物");
        gemStocks.put("300558", "贝达药业");
        gemStocks.put("300595", "欧普康视");
        gemStocks.put("300601", "康泰生物");
        gemStocks.put("300616", "尚品宅配");
        gemStocks.put("300628", "亿联网络");
        gemStocks.put("300633", "开立医疗");
        gemStocks.put("300661", "圣邦股份");
        gemStocks.put("300677", "英科医疗");
        gemStocks.put("300679", "电连技术");
        gemStocks.put("300699", "光威复材");
        gemStocks.put("300750", "宁德时代");
        gemStocks.put("300759", "康龙化成");
        gemStocks.put("300760", "迈瑞医疗");
        gemStocks.put("300782", "卓胜微");
        
        // 转换为股票信息对象列表
        for (Map.Entry<String, String> entry : gemStocks.entrySet()) {
            stocks.add(new StockInfo(entry.getKey(), entry.getValue(), GEM));
        }
        
        return stocks;
    }
    
    /**
     * 保存股票代码到CSV文件
     */
    private static void saveToCSV(List<StockInfo> stocks, File outputFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            // 写入CSV标题
            writer.println("代码,名称,市场");
            
            // 写入股票数据
            for (StockInfo stock : stocks) {
                writer.println(stock.code + "," + stock.name + "," + stock.market);
            }
        }
    }
    
    /**
     * 股票信息类
     */
    public static class StockInfo {
        final String code;
        final String name;
        final String market;
        
        StockInfo(String code, String name, String market) {
            this.code = code;
            this.name = name;
            this.market = market;
        }
    }
} 