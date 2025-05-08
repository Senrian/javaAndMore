package com.example.account;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 账户服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.example.account.mapper")
@ComponentScan({"com.example.account", "com.example.common.config", "com.example.common.feign.config"})
public class AccountServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
} 