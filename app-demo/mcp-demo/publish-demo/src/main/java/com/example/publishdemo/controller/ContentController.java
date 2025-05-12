package com.example.publishdemo.controller;

import com.example.publishdemo.model.ContentTheme;
import com.example.publishdemo.model.XhsNote;
import com.example.publishdemo.service.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 内容控制器
 * 提供内容生成和发布的API接口
 */
@RestController
@RequestMapping("/api/content")
@Slf4j
public class ContentController {

    // 手动添加日志对象
    private static final Logger log = LoggerFactory.getLogger(ContentController.class);

    private final ContentGenerationService contentGenerationService;
    
    public ContentController(ContentGenerationService contentGenerationService) {
        this.contentGenerationService = contentGenerationService;
    }

    /**
     * 获取所有可用主题
     *
     * @return 主题列表
     */
    @GetMapping("/themes")
    public ResponseEntity<List<ContentTheme>> getThemes() {
        log.debug("获取所有可用主题");
        List<ContentTheme> themes = contentGenerationService.getAvailableThemes();
        return ResponseEntity.ok(themes);
    }

    /**
     * 获取指定主题
     *
     * @param themeName 主题名称
     * @return 主题信息
     */
    @GetMapping("/themes/{themeName}")
    public ResponseEntity<ContentTheme> getTheme(@PathVariable String themeName) {
        log.debug("获取主题: {}", themeName);
        ContentTheme theme = contentGenerationService.getThemeByName(themeName);
        
        if (theme == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(theme);
    }

    /**
     * 添加新主题
     *
     * @param theme 主题信息
     * @return 添加后的主题
     */
    @PostMapping("/themes")
    public ResponseEntity<ContentTheme> addTheme(@RequestBody ContentTheme theme) {
        log.debug("添加新主题: {}", theme.getName());
        ContentTheme addedTheme = contentGenerationService.addTheme(theme);
        return ResponseEntity.ok(addedTheme);
    }

    /**
     * 生成小红书笔记内容
     *
     * @param themeName 主题名称
     * @return 生成的笔记内容
     */
    @GetMapping("/generate/{themeName}")
    public ResponseEntity<XhsNote> generateNote(@PathVariable String themeName) {
        log.info("生成主题 [{}] 的笔记内容", themeName);
        
        try {
            XhsNote note = contentGenerationService.generateNote(themeName);
            return ResponseEntity.ok(note);
        } catch (IllegalArgumentException e) {
            log.error("生成笔记失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("生成笔记时发生错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成并发布小红书笔记
     *
     * @param themeName 主题名称
     * @return 操作结果
     */
    @PostMapping("/publish/{themeName}")
    public ResponseEntity<String> publishNote(@PathVariable String themeName) {
        log.info("生成并发布主题 [{}] 的笔记", themeName);
        
        try {
            // 异步生成并发布笔记
            contentGenerationService.generateAndPublishNote(themeName);
            
            return ResponseEntity.ok("笔记生成并发布请求已提交，正在处理中");
        } catch (IllegalArgumentException e) {
            log.error("发布笔记失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body("发布失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("发布笔记时发生错误", e);
            return ResponseEntity.internalServerError().body("发布失败: " + e.getMessage());
        }
    }
} 