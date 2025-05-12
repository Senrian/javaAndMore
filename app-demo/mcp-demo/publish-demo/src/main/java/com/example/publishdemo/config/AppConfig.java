package com.example.publishdemo.config;

import com.example.publishdemo.client.ManagedMcpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * 应用配置类
 * 定义系统所需的各种Bean
 */
@Configuration
@EnableAsync
public class AppConfig {

    @Value("${spring.ai.mcp.servers.fetch.url}")
    private String fetchServerUrl;

    @Value("${spring.ai.mcp.servers.xhs.url}")
    private String xhsServerUrl;

    /**
     * 创建RestTemplate实例
     * 用于HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 创建Fetch MCP客户端
     * 用于网页内容抓取
     */
    @Bean(name = "fetchMcpClient")
    public ManagedMcpClient fetchClient(RestTemplate restTemplate) {
        return new ManagedMcpClient(fetchServerUrl, restTemplate);
    }

    /**
     * 创建小红书MCP客户端
     * 用于发布内容到小红书
     */
    @Bean(name = "xhsMcpClient")
    public ManagedMcpClient xhsClient(RestTemplate restTemplate) {
        return new ManagedMcpClient(xhsServerUrl, restTemplate);
    }
}