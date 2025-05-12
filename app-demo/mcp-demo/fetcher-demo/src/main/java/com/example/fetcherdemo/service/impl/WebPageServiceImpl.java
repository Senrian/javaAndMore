package com.example.fetcherdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fetcherdemo.entity.WebPage;
import com.example.fetcherdemo.mapper.WebPageMapper;
import com.example.fetcherdemo.model.dto.FetchRequest;
import com.example.fetcherdemo.model.dto.FetchResponse;
import com.example.fetcherdemo.service.WebPageService;
import com.example.fetcherdemo.service.mcp.McpFetchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * WebPage服务实现类
 * 
 * @author example
 * @date 2023-06-01
 */
@Service
@Slf4j
public class WebPageServiceImpl extends ServiceImpl<WebPageMapper, WebPage> implements WebPageService {

    private final McpFetchService mcpFetchService;

    public WebPageServiceImpl(McpFetchService mcpFetchService) {
        this.mcpFetchService = mcpFetchService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebPage fetchAndSave(String url) {
        log.info("开始抓取并保存URL: {}", url);
        
        // 检查URL是否已存在
        WebPage existingPage = getByUrl(url);
        if (existingPage != null) {
            log.info("URL已存在，返回已有数据: {}", url);
            return existingPage;
        }
        
        // 创建新的WebPage对象
        WebPage webPage = new WebPage();
        webPage.setUrl(url);
        webPage.setStatus(0); // 设置为未抓取状态
        save(webPage); // 先保存，获取ID
        
        try {
            // 调用MCP服务抓取URL
            FetchResponse response = mcpFetchService.fetchUrl(url);
            
            // 更新WebPage对象
            if (response.isSuccess()) {
                webPage.setTitle(response.getTitle());
                webPage.setContent(response.getContent());
                webPage.setHtmlContent(response.getHtml());
                webPage.setStatus(1); // 抓取成功
            } else {
                webPage.setStatus(2); // 抓取失败
                webPage.setErrorMsg(response.getErrorMessage());
            }
            
            // 更新到数据库
            updateById(webPage);
            log.info("抓取并保存URL完成: {}, 状态: {}", url, webPage.getStatus());
            
            return webPage;
        } catch (Exception e) {
            log.error("抓取并保存URL异常: {}, 异常: {}", url, e.getMessage(), e);
            
            // 更新失败状态
            webPage.setStatus(2); // 抓取失败
            webPage.setErrorMsg("系统异常: " + e.getMessage());
            updateById(webPage);
            
            return webPage;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebPage fetchWithRuleAndSave(FetchRequest request) {
        log.info("开始使用规则抓取并保存URL: {}, 规则: {}", request.getUrl(), request.getExtractRule());
        
        // 检查URL是否已存在
        WebPage existingPage = getByUrl(request.getUrl());
        if (existingPage != null) {
            log.info("URL已存在，返回已有数据: {}", request.getUrl());
            return existingPage;
        }
        
        // 创建新的WebPage对象
        WebPage webPage = new WebPage();
        webPage.setUrl(request.getUrl());
        webPage.setStatus(0); // 设置为未抓取状态
        save(webPage); // 先保存，获取ID
        
        try {
            // 调用MCP服务抓取URL
            FetchResponse response = mcpFetchService.fetchWithExtraction(request);
            
            // 更新WebPage对象
            if (response.isSuccess()) {
                webPage.setTitle(response.getTitle());
                webPage.setContent(response.getContent());
                webPage.setHtmlContent(response.getHtml());
                webPage.setStatus(1); // 抓取成功
            } else {
                webPage.setStatus(2); // 抓取失败
                webPage.setErrorMsg(response.getErrorMessage());
            }
            
            // 更新到数据库
            updateById(webPage);
            log.info("使用规则抓取并保存URL完成: {}, 状态: {}", request.getUrl(), webPage.getStatus());
            
            return webPage;
        } catch (Exception e) {
            log.error("使用规则抓取并保存URL异常: {}, 异常: {}", request.getUrl(), e.getMessage(), e);
            
            // 更新失败状态
            webPage.setStatus(2); // 抓取失败
            webPage.setErrorMsg("系统异常: " + e.getMessage());
            updateById(webPage);
            
            return webPage;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WebPage> batchFetchAndSave(List<String> urls) {
        log.info("开始批量抓取并保存URL, 数量: {}", urls.size());
        List<WebPage> results = new ArrayList<>();
        
        for (String url : urls) {
            try {
                WebPage webPage = fetchAndSave(url);
                results.add(webPage);
            } catch (Exception e) {
                log.error("批量抓取URL异常: {}, 异常: {}", url, e.getMessage(), e);
                
                // 创建失败记录
                WebPage failedPage = new WebPage();
                failedPage.setUrl(url);
                failedPage.setStatus(2); // 抓取失败
                failedPage.setErrorMsg("系统异常: " + e.getMessage());
                save(failedPage);
                
                results.add(failedPage);
            }
        }
        
        log.info("批量抓取并保存URL完成, 成功数量: {}", 
                results.stream().filter(p -> p.getStatus() == 1).count());
        
        return results;
    }

    @Override
    @Async
    public CompletableFuture<List<WebPage>> batchFetchAndSaveAsync(List<String> urls) {
        log.info("开始异步批量抓取并保存URL, 数量: {}", urls.size());
        
        // 创建多个异步任务
        List<CompletableFuture<WebPage>> futures = urls.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> fetchAndSave(url)))
                .collect(Collectors.toList());
        
        // 等待所有任务完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    @Override
    public Page<WebPage> pageByStatus(Integer status, Page<WebPage> page) {
        LambdaQueryWrapper<WebPage> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(WebPage::getStatus, status);
        }
        queryWrapper.orderByDesc(WebPage::getCreateTime);
        
        return page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebPage refetch(Long id) {
        log.info("开始重新抓取网页, ID: {}", id);
        
        // 查询网页
        WebPage webPage = getById(id);
        if (webPage == null) {
            log.error("网页不存在, ID: {}", id);
            throw new RuntimeException("网页不存在");
        }
        
        // 重置状态
        webPage.setStatus(0);
        webPage.setErrorMsg(null);
        updateById(webPage);
        
        try {
            // 调用MCP服务重新抓取
            FetchResponse response = mcpFetchService.fetchUrl(webPage.getUrl());
            
            // 更新WebPage对象
            if (response.isSuccess()) {
                webPage.setTitle(response.getTitle());
                webPage.setContent(response.getContent());
                webPage.setHtmlContent(response.getHtml());
                webPage.setStatus(1); // 抓取成功
            } else {
                webPage.setStatus(2); // 抓取失败
                webPage.setErrorMsg(response.getErrorMessage());
            }
            
            // 更新到数据库
            updateById(webPage);
            log.info("重新抓取网页完成, ID: {}, URL: {}, 状态: {}", id, webPage.getUrl(), webPage.getStatus());
            
            return webPage;
        } catch (Exception e) {
            log.error("重新抓取网页异常, ID: {}, URL: {}, 异常: {}", id, webPage.getUrl(), e.getMessage(), e);
            
            // 更新失败状态
            webPage.setStatus(2); // 抓取失败
            webPage.setErrorMsg("系统异常: " + e.getMessage());
            updateById(webPage);
            
            return webPage;
        }
    }

    /**
     * 根据URL查询网页
     * 
     * @param url URL
     * @return 网页对象，如果不存在则返回null
     */
    private WebPage getByUrl(String url) {
        LambdaQueryWrapper<WebPage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WebPage::getUrl, url);
        return getOne(queryWrapper);
    }
} 