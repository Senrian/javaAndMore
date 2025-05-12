package com.example.publishdemo.service.impl;

import com.example.publishdemo.model.ContentTheme;
import com.example.publishdemo.service.DeepSeekService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek服务实现
 * 调用DeepSeek API生成文案
 */
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekServiceImpl.class);

    @Value("${deepseek.api.url}")
    private String apiUrl;
    
    @Value("${deepseek.api.token}")
    private String apiToken;
    
    @Value("${deepseek.api.model}")
    private String model;

    /**
     * 根据主题生成文案标题
     *
     * @param theme 内容主题
     * @return 生成的标题
     */
    @Override
    public String generateTitle(ContentTheme theme) {
        log.debug("开始为主题 [{}] 生成标题", theme.getName());
        
        String prompt = String.format(
            "请为一篇关于'%s'的小红书笔记生成一个吸引人的标题，要求：\n" +
            "1. 标题要简洁有力，不超过15个字\n" +
            "2. 要有吸引力，能引起用户点击欲望\n" +
            "3. 符合小红书平台风格\n" +
            "4. 不要使用emoji表情符号\n" +
            "5. 直接返回标题，不要有其他解释",
            theme.getName()
        );
        
        String response = callDeepSeekApi(prompt);
        
        // 清理响应，确保只返回标题文本
        String title = response.trim();
        if (title.startsWith("\"") && title.endsWith("\"")) {
            title = title.substring(1, title.length() - 1);
        }
        
        log.info("为主题 [{}] 生成的标题: {}", theme.getName(), title);
        return title;
    }

    /**
     * 根据主题生成文案内容
     *
     * @param theme 内容主题
     * @param titleHint 标题提示，可为空
     * @return 生成的内容
     */
    @Override
    public String generateContent(ContentTheme theme, String titleHint) {
        log.debug("开始为主题 [{}] 生成内容，标题提示: {}", theme.getName(), titleHint);
        
        String promptTemplate = theme.getPromptTemplate();
        String prompt;
        
        if (promptTemplate != null && !promptTemplate.isEmpty()) {
            // 使用自定义提示模板
            prompt = String.format(promptTemplate, titleHint != null ? titleHint : theme.getName());
        } else {
            // 使用默认提示模板
            prompt = String.format(
                "请为一篇小红书笔记撰写正文内容，主题是'%s'%s。要求：\n" +
                "1. 内容真实、有价值、有体验感\n" +
                "2. 语言风格要亲切自然，像朋友间的分享\n" +
                "3. 段落清晰，结构合理\n" +
                "4. 总字数在300-500字之间\n" +
                "5. 不要使用emoji表情符号\n" +
                "6. 直接返回正文内容，不要有其他解释",
                theme.getName(),
                titleHint != null ? "，标题是'" + titleHint + "'" : ""
            );
        }
        
        String content = callDeepSeekApi(prompt);
        
        log.info("为主题 [{}] 生成内容完成，长度: {} 字符", theme.getName(), content.length());
        return content;
    }
    
    /**
     * 生成图片搜索关键词
     *
     * @param theme 内容主题
     * @param content 已生成的内容
     * @return 图片搜索关键词数组
     */
    @Override
    public String[] generateImageKeywords(ContentTheme theme, String content) {
        log.debug("开始为主题 [{}] 生成图片搜索关键词", theme.getName());
        
        String prompt = String.format(
            "基于以下小红书笔记内容，提取3-5个适合作为图片搜索关键词的短语。这些关键词将用于搜索与内容相关的图片。\n" +
            "要求：\n" +
            "1. 关键词要具体且视觉化，便于搜索到相关图片\n" +
            "2. 每个关键词不超过5个字\n" +
            "3. 关键词之间用英文逗号分隔\n" +
            "4. 直接返回关键词列表，不要有其他解释\n\n" +
            "笔记内容：\n%s",
            content
        );
        
        String response = callDeepSeekApi(prompt);
        
        // 清理响应，分割关键词
        String[] keywords = response.trim().split(",");
        
        // 清理每个关键词
        for (int i = 0; i < keywords.length; i++) {
            keywords[i] = keywords[i].trim();
        }
        
        log.info("为主题 [{}] 生成的图片关键词: {}", theme.getName(), String.join(", ", keywords));
        return keywords;
    }
    
    /**
     * 调用DeepSeek API
     *
     * @param prompt 提示词
     * @return API响应内容
     */
    private String callDeepSeekApi(String prompt) {
        try {
            log.debug("调用DeepSeek API，提示词长度: {} 字符", prompt.length());
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);
            
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("max_tokens", 512);
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.7);
            
            HttpResponse<String> response = Unirest.post(apiUrl)
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .asString();
            
            if (response.getStatus() != 200) {
                log.error("DeepSeek API调用失败，状态码: {}, 响应: {}", response.getStatus(), response.getBody());
                throw new RuntimeException("DeepSeek API调用失败: " + response.getStatus());
            }
            
            JSONObject responseJson = new JSONObject(response.getBody());
            String content = responseJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
            
            log.debug("DeepSeek API调用成功，响应长度: {} 字符", content.length());
            return content;
            
        } catch (Exception e) {
            log.error("调用DeepSeek API时发生错误", e);
            throw new RuntimeException("调用DeepSeek API时发生错误: " + e.getMessage(), e);
        }
    }
} 