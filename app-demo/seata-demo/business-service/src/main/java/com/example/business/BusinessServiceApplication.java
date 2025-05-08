package com.example.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 业务服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.example.common.feign"})
@ComponentScan({"com.example.business", "com.example.common.config", "com.example.common.feign.config"})
public class BusinessServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
} 