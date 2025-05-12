package com.example.publishdemo.service.impl;

import com.example.publishdemo.client.ManagedMcpClient;
import com.example.publishdemo.service.FetchService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetch服务实现
 * 使用MCP Fetch工具抓取网页内容和图片
 */
@Service
public class FetchServiceImpl implements FetchService {

    private static final Logger log = LoggerFactory.getLogger(FetchServiceImpl.class);

    private final ManagedMcpClient fetchMcpClient;
    private final RestTemplate restTemplate;
    
    @Value("${content.generation.max-fetch-pages:5}")
    private int maxFetchPages;

    public FetchServiceImpl(@Qualifier("fetchMcpClient") ManagedMcpClient fetchMcpClient, 
                            RestTemplate restTemplate) {
        this.fetchMcpClient = fetchMcpClient;
        this.restTemplate = restTemplate;
    }

    /**
     * 抓取网页内容
     *
     * @param url 网页URL
     * @return 网页内容（Markdown格式）
     */
    @Override
    public String fetchWebContent(String url) {
        log.debug("开始抓取网页内容: {}", url);
        
        try {
            String content = fetchMcpClient.fetchContent(url, 5000);
            
            if (content != null && !content.isEmpty()) {
                log.info("成功抓取网页内容: {}, 长度: {} 字符", url, content.length());
                return content;
            } else {
                log.warn("抓取网页内容失败，响应为空或无效: {}", url);
                return "";
            }
        } catch (Exception e) {
            log.error("抓取网页内容时发生错误: {}", url, e);
            return "";
        }
    }

    /**
     * 根据关键词搜索并抓取图片
     *
     * @param keyword 搜索关键词
     * @param maxCount 最大图片数量
     * @return 图片URL列表
     */
    @Override
    public List<String> fetchImages(String keyword, int maxCount) {
        log.debug("开始搜索图片，关键词: {}, 最大数量: {}", keyword, maxCount);
        
        List<String> imageUrls = new ArrayList<>();
        
        try {
            // 使用搜索引擎搜索图片
            String searchUrl = "https://www.bing.com/images/search?q=" + keyword + "&form=HDRSC2&first=1";
            
            String htmlContent = fetchMcpClient.fetchContent(searchUrl, 10000);
            
            if (htmlContent != null && !htmlContent.isEmpty()) {
                // 简单解析HTML中的图片URL
                List<String> extractedUrls = extractImageUrls(htmlContent);
                
                // 限制图片数量
                int count = Math.min(extractedUrls.size(), maxCount);
                for (int i = 0; i < count; i++) {
                    imageUrls.add(extractedUrls.get(i));
                }
                
                log.info("成功搜索图片，关键词: {}, 找到: {} 张图片", keyword, imageUrls.size());
            } else {
                log.warn("搜索图片失败，响应为空或无效: {}", keyword);
            }
        } catch (Exception e) {
            log.error("搜索图片时发生错误: {}", keyword, e);
        }
        
        return imageUrls;
    }
    
    /**
     * 根据URL抓取单张图片
     *
     * @param imageUrl 图片URL
     * @return 处理后的图片URL（可直接用于发布）
     */
    @Override
    public String fetchSingleImage(String imageUrl) {
        log.debug("开始抓取单张图片: {}", imageUrl);
        
        try {
            String content = fetchMcpClient.fetchContent(imageUrl, 1000);
            
            if (content != null && !content.isEmpty()) {
                log.info("成功抓取图片: {}", imageUrl);
                return imageUrl; // 返回原始URL，实际应用中可能需要处理图片数据
            } else {
                log.warn("抓取图片失败，响应为空或无效: {}", imageUrl);
                return null;
            }
        } catch (Exception e) {
            log.error("抓取图片时发生错误: {}", imageUrl, e);
            return null;
        }
    }
    
    /**
     * 从HTML内容中提取图片URL
     *
     * @param htmlContent HTML内容
     * @return 图片URL列表
     */
    private List<String> extractImageUrls(String htmlContent) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            // 使用正则表达式匹配图片URL
            Pattern pattern = Pattern.compile("src=[\"'](https?://[^\"']+\\.(jpg|jpeg|png|gif))[\"']");
            Matcher matcher = pattern.matcher(htmlContent);
            
            while (matcher.find() && imageUrls.size() < 20) { // 限制最大数量
                String url = matcher.group(1);
                if (isValidImageUrl(url)) {
                    imageUrls.add(url);
                }
            }
        } catch (Exception e) {
            log.error("解析HTML提取图片URL时发生错误", e);
        }
        
        return imageUrls;
    }
    
    /**
     * 检查URL是否为有效的图片URL
     *
     * @param url URL
     * @return 是否有效
     */
    private boolean isValidImageUrl(String url) {
        return url != null && !url.isEmpty() && 
               (url.endsWith(".jpg") || url.endsWith(".jpeg") || 
                url.endsWith(".png") || url.endsWith(".gif") ||
                url.contains(".jpg?") || url.contains(".jpeg?") || 
                url.contains(".png?") || url.contains(".gif?"));
    }
}