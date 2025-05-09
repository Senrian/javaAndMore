package com.example.mcpdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * MCP演示应用主启动类
 */
@SpringBootApplication
@MapperScan("com.example.mcpdemo.mapper")
public class McpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpDemoApplication.class, args);
    }
} 