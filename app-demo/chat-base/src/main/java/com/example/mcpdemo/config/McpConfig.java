package com.example.mcpdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP配置类
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.server")
@Component
@Data
public class McpConfig {
    /**
     * 服务端点
     */
    private String endpoint = "/mcp";
    
    /**
     * 默认模型
     */
    private String defaultModel = "gpt-3.5-turbo";
    
    /**
     * 支持的模型列表
     */
    private List<String> models = new ArrayList<>();
    
    /**
     * 会话超时时间（毫秒）
     */
    private long sessionTimeout = 1800000;
    
    /**
     * 最大上下文长度
     */
    private int maxContextSize = 1000;
    
    /**
     * 最大会话数
     */
    private int maxSessions = 100;
} 