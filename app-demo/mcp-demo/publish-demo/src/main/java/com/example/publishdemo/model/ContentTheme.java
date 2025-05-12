package com.example.publishdemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容主题模型类
 * 用于定义不同的内容主题及其相关属性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTheme {
    /**
     * 主题名称，用作唯一标识
     */
    private String name;
    
    /**
     * 主题描述，用于展示
     */
    private String description;
    
    /**
     * 搜索关键词，用于抓取相关内容
     */
    private String[] searchKeywords;
    
    /**
     * 图片关键词，用于生成或搜索图片
     */
    private String[] imageKeywords;
    
    /**
     * 提示模板，用于生成内容
     */
    private String promptTemplate;
    
    /**
     * 内容风格，用于定义生成内容的风格
     */
    private String style;
    
    // 手动添加getter和setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String[] getSearchKeywords() {
        return searchKeywords;
    }
    
    public void setSearchKeywords(String[] searchKeywords) {
        this.searchKeywords = searchKeywords;
    }
    
    public String[] getImageKeywords() {
        return imageKeywords;
    }
    
    public void setImageKeywords(String[] imageKeywords) {
        this.imageKeywords = imageKeywords;
    }
    
    public String getPromptTemplate() {
        return promptTemplate;
    }
    
    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
} 