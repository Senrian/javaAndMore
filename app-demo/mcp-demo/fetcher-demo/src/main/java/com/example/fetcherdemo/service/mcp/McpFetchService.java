package com.example.fetcherdemo.service.mcp;

import com.example.fetcherdemo.model.dto.FetchRequest;
import com.example.fetcherdemo.model.dto.FetchResponse;

import java.util.concurrent.CompletableFuture;

/**
 * MCP抓取服务接口
 * 
 * 定义与MCP Fetch服务交互的方法
 * 
 * @author example
 * @date 2023-06-01
 */
public interface McpFetchService {
    
    /**
     * 抓取单个URL的内容
     * 
     * @param url 要抓取的URL
     * @return 抓取结果
     */
    FetchResponse fetchUrl(String url);
    
    /**
     * 异步抓取单个URL的内容
     * 
     * @param url 要抓取的URL
     * @return 抓取结果的CompletableFuture
     */
    CompletableFuture<FetchResponse> fetchUrlAsync(String url);
    
    /**
     * 抓取URL并提取特定内容
     * 
     * @param request 抓取请求，包含URL和提取规则
     * @return 抓取结果
     */
    FetchResponse fetchWithExtraction(FetchRequest request);
    
    /**
     * 检查MCP服务是否可用
     * 
     * @return 如果服务可用，则返回true
     */
    boolean isServiceAvailable();
} 