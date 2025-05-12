package com.example.publishdemo.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟MCP客户端
 * 用于替代Spring AI MCP的功能
 */
@Slf4j
public class ManagedMcpClient {
    
    private final String serverUrl;
    private final RestTemplate restTemplate;
    
    // 手动添加日志对象
    private static final Logger log = LoggerFactory.getLogger(ManagedMcpClient.class);
    
    public ManagedMcpClient(String serverUrl, RestTemplate restTemplate) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplate;
    }
    
    /**
     * 调用MCP API获取网页内容
     */
    public String fetchContent(String url, int maxLength) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("url", url);
            requestBody.put("max_length", maxLength);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    serverUrl + "/fetch", 
                    requestEntity, 
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                return jsonResponse.optString("content", "");
            }
            return "无法获取内容";
        } catch (Exception e) {
            log.error("获取网页内容失败 : {}", e.getMessage(), e);
            return "获取内容时出错: " + e.getMessage();
        }
    }
    
    /**
     * 发布笔记到小红书
     */
    public String createNote(String title, String content, List<String> images) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", title);
            requestBody.put("content", content);
            requestBody.put("images", images);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    serverUrl + "/create_note", 
                    requestEntity, 
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return "发布失败: " + response.getStatusCode();
        } catch (Exception e) {
            log.error("发布小红书笔记失败: {}", e.getMessage(), e);
            return "发布失败: " + e.getMessage();
        }
    }
} 