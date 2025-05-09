package com.example.mcpdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP消息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpMessageDto {
    
    /**
     * 角色：system, user, assistant
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息ID
     */
    private String id;
    
    /**
     * 创建时间戳
     */
    private Long timestamp;
} 