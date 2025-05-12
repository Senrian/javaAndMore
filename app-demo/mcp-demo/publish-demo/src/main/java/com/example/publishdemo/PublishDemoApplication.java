package com.example.publishdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 小红书内容生成与发布应用程序
 * 
 * 该应用程序使用Spring AI和MCP工具：
 * 1. 调用DeepSeek AI生成文案
 * 2. 使用Fetch MCP工具抓取网页内容和图片
 * 3. 调用小红书MCP工具发布内容
 * 
 * @author example
 */
@SpringBootApplication
@EnableAsync
public class PublishDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublishDemoApplication.class, args);
    }
} 