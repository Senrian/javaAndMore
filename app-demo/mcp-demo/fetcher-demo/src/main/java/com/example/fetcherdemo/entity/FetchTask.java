package com.example.fetcherdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 抓取任务实体类
 * 
 * 用于存储网页抓取任务信息
 * 
 * @author example
 * @date 2023-06-01
 */
@Data
@Accessors(chain = true)
@TableName("fetch_task")
public class FetchTask {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;
    
    /**
     * 任务类型：SIMPLE-简单抓取，BATCH-批量抓取，SCHEDULED-定时抓取
     */
    @TableField("task_type")
    private String taskType;
    
    /**
     * 抓取URL列表，多个URL用英文逗号分隔
     */
    private String urls;
    
    /**
     * 任务状态：0-待执行，1-执行中，2-已完成，3-失败
     */
    private Integer status;
    
    /**
     * 总URL数量
     */
    @TableField("total_count")
    private Integer totalCount;
    
    /**
     * 成功数量
     */
    @TableField("success_count")
    private Integer successCount;
    
    /**
     * 失败数量
     */
    @TableField("fail_count")
    private Integer failCount;
    
    /**
     * Cron表达式，用于定时任务
     */
    @TableField("cron_expression")
    private String cronExpression;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 任务类型枚举
     */
    public enum TaskType {
        /**
         * 简单抓取
         */
        SIMPLE,
        
        /**
         * 批量抓取
         */
        BATCH,
        
        /**
         * 定时抓取
         */
        SCHEDULED
    }
    
    /**
     * 任务状态枚举
     */
    public enum Status {
        /**
         * 待执行
         */
        PENDING(0),
        
        /**
         * 执行中
         */
        RUNNING(1),
        
        /**
         * 已完成
         */
        COMPLETED(2),
        
        /**
         * 失败
         */
        FAILED(3);
        
        private final int value;
        
        Status(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
} 