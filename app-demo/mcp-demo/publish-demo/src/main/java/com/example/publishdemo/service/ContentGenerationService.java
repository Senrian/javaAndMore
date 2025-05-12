package com.example.publishdemo.service;

import com.example.publishdemo.model.ContentTheme;
import com.example.publishdemo.model.XhsNote;

import java.util.List;

/**
 * 内容生成服务接口
 * 整合AI文案生成、网页抓取和图片获取功能
 */
public interface ContentGenerationService {

    /**
     * 获取所有可用的内容主题
     *
     * @return 内容主题列表
     */
    List<ContentTheme> getAvailableThemes();

    /**
     * 根据主题名称获取主题
     *
     * @param themeName 主题名称
     * @return 内容主题
     */
    ContentTheme getThemeByName(String themeName);
    
    /**
     * 添加新的内容主题
     *
     * @param theme 内容主题
     * @return 添加后的主题
     */
    ContentTheme addTheme(ContentTheme theme);
    
    /**
     * 根据主题生成完整的小红书笔记
     *
     * @param themeName 主题名称
     * @return 生成的小红书笔记
     */
    XhsNote generateNote(String themeName);
    
    /**
     * 生成并发布小红书笔记
     *
     * @param themeName 主题名称
     * @return 发布结果
     */
    XhsNote generateAndPublishNote(String themeName);
} 