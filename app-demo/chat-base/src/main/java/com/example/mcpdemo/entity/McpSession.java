package com.example.mcpdemo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MCP会话实体
 */
@TableName("mcp_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpSession {
    
    /**
     * 会话ID
     */
    @TableId
    private String id;
    
    /**
     * 会话标题
     */
    @TableField
    private String title;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;
    
    /**
     * 最后更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;
    
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;
    
    /**
     * 使用的模型
     */
    @TableField
    private String model;
    
    /**
     * 状态：ACTIVE, ARCHIVED
     */
    @TableField
    private String status;
    
    /**
     * 系统提示词
     */
    @TableField(value = "system_prompt")
    private String systemPrompt;
} 