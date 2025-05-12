package com.example.fetcherdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 网页实体类
 * 
 * 用于存储抓取的网页信息
 * 
 * @author example
 * @date 2023-06-01
 */
@Data
@Accessors(chain = true)
@TableName("web_page")
public class WebPage {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 网页URL
     */
    private String url;
    
    /**
     * 网页标题
     */
    private String title;
    
    /**
     * 网页内容
     */
    private String content;
    
    /**
     * 网页HTML
     */
    @TableField("html_content")
    private String htmlContent;
    
    /**
     * 抓取状态：0-未抓取，1-抓取成功，2-抓取失败
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;
    
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
} 