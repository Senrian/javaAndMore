package com.example.storage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 库存服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.example.storage.mapper")
@ComponentScan({"com.example.storage", "com.example.common.config"})
public class StorageServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(StorageServiceApplication.class, args);
    }
} 