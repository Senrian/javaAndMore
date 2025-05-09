package com.example.mcpdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP消息实体
 */
@TableName("mcp_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpMessage {
    
    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 会话ID
     */
    @TableField(value = "session_id")
    private String sessionId;
    
    /**
     * 角色：system, user, assistant
     */
    @TableField
    private String role;
    
    /**
     * 消息内容
     */
    @TableField
    private String content;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;
    
    /**
     * 序号（消息顺序）
     */
    @TableField
    private Integer sequence;
    
    /**
     * 使用的模型
     */
    @TableField
    private String model;
} 