package com.example.mcpdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    
    /**
     * 会话ID，如果不提供则创建新会话
     */
    private String sessionId;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 消息列表
     */
    private List<McpMessageDto> messages;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 温度参数
     */
    private Float temperature;
    
    /**
     * 最大回复令牌数
     */
    private Integer maxTokens;
    
    /**
     * 是否流式返回
     */
    private Boolean stream;
    
    /**
     * 插件列表
     */
    private List<String> tools;
    
    /**
     * 自定义参数
     */
    private Map<String, Object> parameters;
} 