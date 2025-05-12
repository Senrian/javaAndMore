package com.example.fetcherdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * MCP配置类
 * 
 * 用于从配置文件中读取MCP相关配置
 * 
 * @author example
 * @date 2023-06-01
 */
@Configuration
@ConfigurationProperties(prefix = "mcp")
@Data
public class McpConfig {
    
    /**
     * MCP服务器配置
     */
    private Map<String, McpServerConfig> servers;
    
    /**
     * MCP服务器配置类
     */
    @Data
    public static class McpServerConfig {
        /**
         * 服务类型，如sse
         */
        private String type;
        
        /**
         * 服务URL
         */
        private String url;
    }
} 