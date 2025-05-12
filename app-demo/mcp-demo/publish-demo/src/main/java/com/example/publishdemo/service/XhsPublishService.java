package com.example.publishdemo.service;

import com.example.publishdemo.model.XhsNote;

/**
 * 小红书发布服务接口
 * 用于发布内容到小红书
 */
public interface XhsPublishService {

    /**
     * 发布图文笔记到小红书
     *
     * @param note 小红书笔记内容
     * @return 发布结果
     */
    XhsNote publishImageNote(XhsNote note);

    /**
     * 检查内容是否符合小红书发布规范
     * 例如：检查是否含有emoji表情等
     *
     * @param content 待检查的内容
     * @return 处理后的内容
     */
    String sanitizeContent(String content);
    
    /**
     * 准备发布图片
     * 将图片URL列表转换为可发布的格式
     *
     * @param imageUrls 图片URL列表
     * @return 处理后的图片URL列表
     */
    String[] prepareImages(String[] imageUrls);
} 