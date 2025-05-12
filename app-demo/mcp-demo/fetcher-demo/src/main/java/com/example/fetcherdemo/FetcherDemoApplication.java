package com.example.fetcherdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 网页内容抓取示例应用
 * 
 * 本应用展示了如何使用MCP的Fetch服务进行网页内容抓取，包含多种抓取场景和示例
 * 
 * @author example
 * @date 2023-06-01
 */
@SpringBootApplication
@MapperScan("com.example.fetcherdemo.mapper")
@EnableAsync
public class FetcherDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FetcherDemoApplication.class, args);
    }
} 