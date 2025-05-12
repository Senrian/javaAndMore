package com.example.fetcherdemo.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 抓取响应DTO
 * 
 * @author example
 * @date 2023-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 抓取的URL
     */
    private String url;
    
    /**
     * 网页标题
     */
    private String title;
    
    /**
     * 提取的纯文本内容
     */
    private String content;
    
    /**
     * 原始HTML内容
     */
    private String html;
    
    /**
     * HTTP状态码
     */
    private int statusCode;
    
    /**
     * HTTP响应头
     */
    private Map<String, String> headers;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 抓取耗时（毫秒）
     */
    private long elapsedTime;
    
    /**
     * 创建成功响应
     * 
     * @param url 抓取的URL
     * @param title 网页标题
     * @param content 提取的内容
     * @param html 原始HTML
     * @param statusCode HTTP状态码
     * @param headers HTTP响应头
     * @param elapsedTime 抓取耗时
     * @return 成功的响应对象
     */
    public static FetchResponse success(String url, String title, String content, String html, int statusCode, Map<String, String> headers, long elapsedTime) {
        return FetchResponse.builder()
                .success(true)
                .url(url)
                .title(title)
                .content(content)
                .html(html)
                .statusCode(statusCode)
                .headers(headers)
                .elapsedTime(elapsedTime)
                .build();
    }
    
    /**
     * 创建失败响应
     * 
     * @param url 抓取的URL
     * @param errorMessage 错误信息
     * @return 失败的响应对象
     */
    public static FetchResponse error(String url, String errorMessage) {
        return FetchResponse.builder()
                .success(false)
                .url(url)
                .errorMessage(errorMessage)
                .build();
    }
} 