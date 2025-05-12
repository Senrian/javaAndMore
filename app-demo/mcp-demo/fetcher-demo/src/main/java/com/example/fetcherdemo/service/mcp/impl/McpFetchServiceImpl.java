package com.example.fetcherdemo.service.mcp.impl;

import com.example.fetcherdemo.config.McpConfig;
import com.example.fetcherdemo.model.dto.FetchRequest;
import com.example.fetcherdemo.model.dto.FetchResponse;
import com.example.fetcherdemo.service.mcp.McpFetchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MCP抓取服务实现类
 *
 * @author example
 * @date 2023-06-01
 */
@Service
@Slf4j
public class McpFetchServiceImpl implements McpFetchService {

    private final McpConfig mcpConfig;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public McpFetchServiceImpl(McpConfig mcpConfig, ObjectMapper objectMapper) {
        this.mcpConfig = mcpConfig;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    @Override
    public FetchResponse fetchUrl(String url) {
        log.info("开始抓取URL: {}", url);
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("url", url);
            
            String responseJson = webClient.post()
                    .uri(mcpConfig.getServers().get("fetch").getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
            
            if (responseJson == null) {
                log.error("抓取URL失败，响应为空: {}", url);
                return FetchResponse.error(url, "抓取失败，响应为空");
            }
            
            JsonNode responseNode = objectMapper.readTree(responseJson);
            
            // 解析响应
            String title = responseNode.path("title").asText("");
            String content = responseNode.path("content").asText("");
            String html = responseNode.path("html").asText("");
            int statusCode = responseNode.path("statusCode").asInt(200);
            
            // 构建响应头
            Map<String, String> headers = new HashMap<>();
            JsonNode headersNode = responseNode.path("headers");
            if (headersNode.isObject()) {
                headersNode.fields().forEachRemaining(entry -> 
                    headers.put(entry.getKey(), entry.getValue().asText()));
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("成功抓取URL: {}, 耗时: {}ms", url, elapsedTime);
            
            return FetchResponse.success(url, title, content, html, statusCode, headers, elapsedTime);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("抓取URL异常: {}, 耗时: {}ms, 异常: {}", url, elapsedTime, e.getMessage(), e);
            return FetchResponse.error(url, "抓取异常: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url) {
        log.info("开始异步抓取URL: {}", url);
        return CompletableFuture.supplyAsync(() -> fetchUrl(url));
    }

    @Override
    public FetchResponse fetchWithExtraction(FetchRequest request) {
        log.info("开始抓取URL并提取内容: {}, 提取规则: {}", request.getUrl(), request.getExtractRule());
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("url", request.getUrl());
            requestBody.put("extractContent", request.isExtractContent());
            
            if (request.getExtractRule() != null && !request.getExtractRule().isEmpty()) {
                requestBody.put("extractRule", request.getExtractRule());
                requestBody.put("ruleType", request.getRuleType());
            }
            
            if (request.isKeepHtml()) {
                requestBody.put("keepHtml", true);
            }
            
            if (request.getTimeout() != null) {
                requestBody.put("timeout", request.getTimeout());
            }
            
            // 添加请求头
            if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
                ObjectNode headersNode = objectMapper.createObjectNode();
                request.getHeaders().forEach(headersNode::put);
                requestBody.set("headers", headersNode);
            }
            
            String responseJson = webClient.post()
                    .uri(mcpConfig.getServers().get("fetch").getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
            
            if (responseJson == null) {
                log.error("抓取URL失败，响应为空: {}", request.getUrl());
                return FetchResponse.error(request.getUrl(), "抓取失败，响应为空");
            }
            
            JsonNode responseNode = objectMapper.readTree(responseJson);
            
            // 解析响应
            String title = responseNode.path("title").asText("");
            String content = responseNode.path("content").asText("");
            String html = responseNode.path("html").asText("");
            int statusCode = responseNode.path("statusCode").asInt(200);
            
            // 构建响应头
            Map<String, String> headers = new HashMap<>();
            JsonNode headersNode = responseNode.path("headers");
            if (headersNode.isObject()) {
                headersNode.fields().forEachRemaining(entry -> 
                    headers.put(entry.getKey(), entry.getValue().asText()));
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("成功抓取URL并提取内容: {}, 耗时: {}ms", request.getUrl(), elapsedTime);
            
            return FetchResponse.success(request.getUrl(), title, content, html, statusCode, headers, elapsedTime);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("抓取URL并提取内容异常: {}, 耗时: {}ms, 异常: {}", request.getUrl(), elapsedTime, e.getMessage(), e);
            return FetchResponse.error(request.getUrl(), "抓取异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // 简单的健康检查请求
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("url", "https://www.baidu.com");
            
            String response = webClient.post()
                    .uri(mcpConfig.getServers().get("fetch").getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));
            
            return response != null;
        } catch (Exception e) {
            log.error("MCP服务不可用: {}", e.getMessage());
            return false;
        }
    }
} 