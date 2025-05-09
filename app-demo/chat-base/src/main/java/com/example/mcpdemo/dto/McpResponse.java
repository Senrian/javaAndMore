package com.example.mcpdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResponse {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 响应消息
     */
    private McpMessageDto message;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 是否是流式响应中的最后一条
     */
    private Boolean done;
    
    /**
     * 使用的令牌数
     */
    private Integer usageTokens;
    
    /**
     * 请求ID
     */
    private String requestId;
} 