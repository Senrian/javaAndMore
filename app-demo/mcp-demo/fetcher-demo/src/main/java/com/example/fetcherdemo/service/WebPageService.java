package com.example.fetcherdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fetcherdemo.entity.WebPage;
import com.example.fetcherdemo.model.dto.FetchRequest;
import com.example.fetcherdemo.model.dto.FetchResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * WebPage服务接口
 * 
 * @author example
 * @date 2023-06-01
 */
public interface WebPageService extends IService<WebPage> {
    
    /**
     * 抓取单个URL并保存
     * 
     * @param url 要抓取的URL
     * @return 抓取结果
     */
    WebPage fetchAndSave(String url);
    
    /**
     * 使用自定义规则抓取URL并保存
     * 
     * @param request 抓取请求
     * @return 抓取结果
     */
    WebPage fetchWithRuleAndSave(FetchRequest request);
    
    /**
     * 批量抓取URL
     * 
     * @param urls URL列表
     * @return 抓取结果列表
     */
    List<WebPage> batchFetchAndSave(List<String> urls);
    
    /**
     * 异步批量抓取URL
     * 
     * @param urls URL列表
     * @return 抓取结果的Future
     */
    CompletableFuture<List<WebPage>> batchFetchAndSaveAsync(List<String> urls);
    
    /**
     * 根据状态查询网页
     * 
     * @param status 状态
     * @param page 分页参数
     * @return 分页结果
     */
    Page<WebPage> pageByStatus(Integer status, Page<WebPage> page);
    
    /**
     * 重新抓取指定ID的网页
     * 
     * @param id 网页ID
     * @return 更新后的网页
     */
    WebPage refetch(Long id);
} 