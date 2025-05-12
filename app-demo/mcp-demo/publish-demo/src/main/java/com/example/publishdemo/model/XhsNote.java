package com.example.publishdemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 小红书笔记模型类
 * 用于封装生成和发布的笔记内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XhsNote {
    /**
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记正文内容
     */
    private String content;
    
    /**
     * 笔记配图URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 笔记所属主题
     */
    private ContentTheme theme;
    
    /**
     * 发布状态
     */
    private PublishStatus status;
    
    /**
     * 发布结果消息
     */
    private String resultMessage;
    
    // 手动添加getter和setter方法
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public ContentTheme getTheme() {
        return theme;
    }
    
    public void setTheme(ContentTheme theme) {
        this.theme = theme;
    }
    
    public PublishStatus getStatus() {
        return status;
    }
    
    public void setStatus(PublishStatus status) {
        this.status = status;
    }
    
    public String getResultMessage() {
        return resultMessage;
    }
    
    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
    
    /**
     * 发布状态枚举
     */
    public enum PublishStatus {
        /**
         * 待发布
         */
        PENDING,
        
        /**
         * 发布成功
         */
        SUCCESS,
        
        /**
         * 发布失败
         */
        FAILED
    }
} 