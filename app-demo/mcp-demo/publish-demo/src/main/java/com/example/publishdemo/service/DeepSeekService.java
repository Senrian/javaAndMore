package com.example.publishdemo.service;

import com.example.publishdemo.model.ContentTheme;

/**
 * DeepSeek服务接口
 * 用于调用DeepSeek API生成文案
 */
public interface DeepSeekService {

    /**
     * 根据主题生成文案标题
     *
     * @param theme 内容主题
     * @return 生成的标题
     */
    String generateTitle(ContentTheme theme);

    /**
     * 根据主题生成文案内容
     *
     * @param theme 内容主题
     * @param titleHint 标题提示，可为空
     * @return 生成的内容
     */
    String generateContent(ContentTheme theme, String titleHint);
    
    /**
     * 生成图片搜索关键词
     *
     * @param theme 内容主题
     * @param content 已生成的内容
     * @return 图片搜索关键词数组
     */
    String[] generateImageKeywords(ContentTheme theme, String content);
} 