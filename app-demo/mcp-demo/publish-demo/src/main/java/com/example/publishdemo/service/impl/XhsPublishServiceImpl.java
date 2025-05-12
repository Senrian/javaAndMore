package com.example.publishdemo.service.impl;

import com.example.publishdemo.client.ManagedMcpClient;
import com.example.publishdemo.model.XhsNote;
import com.example.publishdemo.model.XhsNote.PublishStatus;
import com.example.publishdemo.service.XhsPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 小红书发布服务实现
 * 使用MCP工具发布内容到小红书
 */
@Service
public class XhsPublishServiceImpl implements XhsPublishService {

    private static final Logger log = LoggerFactory.getLogger(XhsPublishServiceImpl.class);

    private final ManagedMcpClient xhsMcpClient;
    
    @Value("${content.max-images-per-note:9}")
    private int maxImagesPerNote;

    public XhsPublishServiceImpl(@Qualifier("xhsMcpClient") ManagedMcpClient xhsMcpClient) {
        this.xhsMcpClient = xhsMcpClient;
    }

    /**
     * 发布图文笔记到小红书
     *
     * @param note 小红书笔记内容
     * @return 发布结果
     */
    @Override
    public XhsNote publishImageNote(XhsNote note) {
        log.debug("开始发布小红书笔记: {}", note.getTitle());
        
        try {
            // 确保内容符合规范
            String sanitizedContent = sanitizeContent(note.getContent());
            
            // 准备图片
            List<String> imageUrls = note.getImageUrls();
            if (imageUrls == null) {
                imageUrls = new ArrayList<>();
            }
            
            // 限制图片数量
            if (imageUrls.size() > maxImagesPerNote) {
                log.warn("图片数量超过限制，将截取前 {} 张图片", maxImagesPerNote);
                imageUrls = imageUrls.subList(0, maxImagesPerNote);
            }
            
            // 调用MCP工具发布笔记
            String response = xhsMcpClient.createNote(note.getTitle(), sanitizedContent, imageUrls);
            
            // 处理响应
            if (response != null && !response.contains("失败")) {
                log.info("小红书笔记发布成功: {}", note.getTitle());
                note.setStatus(XhsNote.PublishStatus.SUCCESS);
                note.setResultMessage("发布成功");
            } else {
                log.warn("小红书笔记发布失败: {}", note.getTitle());
                note.setStatus(XhsNote.PublishStatus.FAILED);
                note.setResultMessage("发布失败，" + (response != null ? response : "响应为空"));
            }
        } catch (Exception e) {
            log.error("发布小红书笔记时发生错误: {}", note.getTitle(), e);
            note.setStatus(XhsNote.PublishStatus.FAILED);
            note.setResultMessage("发布失败: " + e.getMessage());
        }
        
        return note;
    }

    /**
     * 检查内容是否符合小红书发布规范
     * 例如：检查是否含有emoji表情等
     *
     * @param content 待检查的内容
     * @return 处理后的内容
     */
    @Override
    public String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }
        
        log.debug("开始清理内容，原始长度: {} 字符", content.length());
        
        // 移除emoji表情
        String sanitized = removeEmojis(content);
        
        // 移除过多的换行符
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");
        
        // 移除HTML标签
        sanitized = sanitized.replaceAll("<[^>]*>", "");
        
        log.debug("内容清理完成，处理后长度: {} 字符", sanitized.length());
        
        return sanitized;
    }
    
    /**
     * 准备发布图片
     * 将图片URL列表转换为可发布的格式
     *
     * @param imageUrls 图片URL列表
     * @return 处理后的图片URL列表
     */
    @Override
    public String[] prepareImages(String[] imageUrls) {
        if (imageUrls == null || imageUrls.length == 0) {
            return new String[0];
        }
        
        List<String> validUrls = new ArrayList<>();
        
        for (String url : imageUrls) {
            if (url != null && !url.isEmpty()) {
                validUrls.add(url);
            }
        }
        
        // 限制图片数量
        if (validUrls.size() > maxImagesPerNote) {
            log.warn("图片数量超过限制，将截取前 {} 张图片", maxImagesPerNote);
            validUrls = validUrls.subList(0, maxImagesPerNote);
        }
        
        return validUrls.toArray(new String[0]);
    }
    
    /**
     * 移除文本中的emoji表情符号
     *
     * @param text 原始文本
     * @return 处理后的文本
     */
    private String removeEmojis(String text) {
        // 匹配emoji表情的正则表达式
        Pattern emojiPattern = Pattern.compile(
            "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
            Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = emojiPattern.matcher(text);
        return matcher.replaceAll("");
    }
} 