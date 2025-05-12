package com.example.publishdemo.service;

import java.util.List;

/**
 * Fetch服务接口
 * 用于抓取网页内容和图片
 */
public interface FetchService {

    /**
     * 抓取网页内容
     *
     * @param url 网页URL
     * @return 网页内容（Markdown格式）
     */
    String fetchWebContent(String url);

    /**
     * 根据关键词搜索并抓取图片
     *
     * @param keyword 搜索关键词
     * @param maxCount 最大图片数量
     * @return 图片URL列表
     */
    List<String> fetchImages(String keyword, int maxCount);
    
    /**
     * 根据URL抓取单张图片
     *
     * @param imageUrl 图片URL
     * @return 处理后的图片URL（可直接用于发布）
     */
    String fetchSingleImage(String imageUrl);
} 