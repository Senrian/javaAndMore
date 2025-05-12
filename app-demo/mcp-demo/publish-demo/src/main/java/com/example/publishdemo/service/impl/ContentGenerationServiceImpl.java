package com.example.publishdemo.service.impl;

import com.example.publishdemo.model.ContentTheme;
import com.example.publishdemo.model.XhsNote;
import com.example.publishdemo.model.XhsNote.PublishStatus;
import com.example.publishdemo.service.ContentGenerationService;
import com.example.publishdemo.service.DeepSeekService;
import com.example.publishdemo.service.FetchService;
import com.example.publishdemo.service.XhsPublishService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内容生成服务实现
 * 整合AI文案生成、网页抓取和图片获取功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationServiceImpl implements ContentGenerationService {

    // 手动添加日志对象
    private static final Logger log = LoggerFactory.getLogger(ContentGenerationServiceImpl.class);

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private FetchService fetchService;
    
    @Autowired
    private XhsPublishService xhsPublishService;
    
    @Value("${content.default-themes:美食探店}")
    private List<String> defaultThemeNames;
    
    @Value("${content.max-images-per-note:9}")
    private int maxImagesPerNote;
    
    // 内存中存储主题配置
    private final Map<String, ContentTheme> themes = new ConcurrentHashMap<>();

    /**
     * 初始化默认主题
     */
    @PostConstruct
    public void init() {
        log.info("初始化默认主题配置: {}", defaultThemeNames);
        
        // 为每个默认主题创建配置
        for (String themeName : defaultThemeNames) {
            ContentTheme theme = createDefaultTheme(themeName);
            themes.put(themeName, theme);
        }
    }
    
    /**
     * 获取所有可用的内容主题
     *
     * @return 内容主题列表
     */
    @Override
    public List<ContentTheme> getAvailableThemes() {
        return new ArrayList<>(themes.values());
    }

    /**
     * 根据主题名称获取主题
     *
     * @param themeName 主题名称
     * @return 内容主题
     */
    @Override
    public ContentTheme getThemeByName(String themeName) {
        return themes.get(themeName);
    }
    
    /**
     * 添加新的内容主题
     *
     * @param theme 内容主题
     * @return 添加后的主题
     */
    @Override
    public ContentTheme addTheme(ContentTheme theme) {
        if (theme.getName() == null || theme.getName().isEmpty()) {
            throw new IllegalArgumentException("主题名称不能为空");
        }
        
        themes.put(theme.getName(), theme);
        log.info("添加新主题: {}", theme.getName());
        
        return theme;
    }
    
    /**
     * 根据主题生成完整的小红书笔记
     *
     * @param themeName 主题名称
     * @return 生成的小红书笔记
     */
    @Override
    public XhsNote generateNote(String themeName) {
        log.info("开始为主题 [{}] 生成笔记", themeName);
        
        ContentTheme theme = themes.get(themeName);
        if (theme == null) {
            log.error("找不到主题: {}", themeName);
            throw new IllegalArgumentException("找不到主题: " + themeName);
        }
        
        // 1. 生成标题
        String title = deepSeekService.generateTitle(theme);
        
        // 2. 生成内容
        String content = deepSeekService.generateContent(theme, title);
        
        // 3. 生成图片关键词并获取图片
        String[] imageKeywords = deepSeekService.generateImageKeywords(theme, content);
        List<String> imageUrls = new ArrayList<>();
        
        // 为每个关键词获取图片
        for (String keyword : imageKeywords) {
            // 每个关键词获取1-2张图片
            int imagesToFetch = Math.min(2, maxImagesPerNote - imageUrls.size());
            if (imagesToFetch <= 0) {
                break;
            }
            
            List<String> images = fetchService.fetchImages(keyword, imagesToFetch);
            imageUrls.addAll(images);
            
            // 如果已经达到最大图片数量，则停止获取
            if (imageUrls.size() >= maxImagesPerNote) {
                break;
            }
        }
        
        // 4. 创建笔记对象
        XhsNote note = new XhsNote();
        note.setTitle(title);
        note.setContent(content);
        note.setImageUrls(imageUrls);
        note.setTheme(theme);
        note.setStatus(XhsNote.PublishStatus.PENDING);
        
        log.info("笔记生成完成: {}, 内容长度: {} 字符, 图片数量: {}", 
                note.getTitle(), note.getContent().length(), note.getImageUrls().size());
        
        return note;
    }
    
    /**
     * 生成并发布小红书笔记
     *
     * @param themeName 主题名称
     * @return 发布结果
     */
    @Override
    @Async
    public XhsNote generateAndPublishNote(String themeName) {
        log.info("开始为主题 [{}] 生成并发布笔记", themeName);
        
        // 1. 生成笔记
        XhsNote note = generateNote(themeName);
        
        // 2. 发布笔记
        return xhsPublishService.publishImageNote(note);
    }
    
    /**
     * 创建默认主题配置
     *
     * @param themeName 主题名称
     * @return 主题配置
     */
    private ContentTheme createDefaultTheme(String themeName) {
        ContentTheme theme = new ContentTheme();
        theme.setName(themeName);
        
        // 根据不同主题设置不同的配置
        switch (themeName) {
            case "美食探店":
                theme.setDescription("分享美食探店体验、美食推荐等内容");
                theme.setSearchKeywords(new String[]{"美食推荐", "探店", "美食打卡", "美食攻略"});
                theme.setImageKeywords(new String[]{"美食", "餐厅", "美食拍照", "网红美食"});
                theme.setStyle("轻松愉快");
                theme.setPromptTemplate("请以小红书博主的口吻，撰写一篇关于美食探店的笔记，主题是'%s'。要求：\n" +
                        "1. 内容要真实、有体验感，包括环境描述、菜品推荐、价格评价等\n" +
                        "2. 语言风格要亲切自然，像朋友间的分享\n" +
                        "3. 段落清晰，结构合理\n" +
                        "4. 总字数在300-500字之间\n" +
                        "5. 不要使用emoji表情符号");
                break;
                
            case "旅行日记":
                theme.setDescription("分享旅行经历、景点推荐、旅行攻略等内容");
                theme.setSearchKeywords(new String[]{"旅行", "旅游攻略", "景点推荐", "旅行日记"});
                theme.setImageKeywords(new String[]{"旅行风景", "景点", "旅行照片", "旅游胜地"});
                theme.setStyle("文艺清新");
                theme.setPromptTemplate("请以小红书博主的口吻，撰写一篇关于旅行的笔记，主题是'%s'。要求：\n" +
                        "1. 内容要有真实的旅行体验，包括景点描述、交通信息、住宿推荐等\n" +
                        "2. 语言风格要生动有趣，充满感染力\n" +
                        "3. 段落清晰，结构合理\n" +
                        "4. 总字数在300-500字之间\n" +
                        "5. 不要使用emoji表情符号");
                break;
                
            case "生活方式":
                theme.setDescription("分享生活技巧、居家布置、生活方式等内容");
                theme.setSearchKeywords(new String[]{"生活方式", "生活技巧", "居家", "日常生活"});
                theme.setImageKeywords(new String[]{"生活场景", "居家布置", "生活细节", "日常"});
                theme.setStyle("实用温馨");
                theme.setPromptTemplate("请以小红书博主的口吻，撰写一篇关于生活方式的笔记，主题是'%s'。要求：\n" +
                        "1. 内容要实用、有价值，能给读者带来启发\n" +
                        "2. 语言风格要亲切自然，像朋友间的分享\n" +
                        "3. 段落清晰，结构合理\n" +
                        "4. 总字数在300-500字之间\n" +
                        "5. 不要使用emoji表情符号");
                break;
                
            case "时尚穿搭":
                theme.setDescription("分享穿搭技巧、时尚单品推荐等内容");
                theme.setSearchKeywords(new String[]{"时尚穿搭", "搭配技巧", "时尚单品", "穿搭推荐"});
                theme.setImageKeywords(new String[]{"时尚穿搭", "搭配", "街拍", "时尚单品"});
                theme.setStyle("时尚专业");
                theme.setPromptTemplate("请以小红书博主的口吻，撰写一篇关于时尚穿搭的笔记，主题是'%s'。要求：\n" +
                        "1. 内容要有专业的穿搭建议，包括单品推荐、搭配技巧等\n" +
                        "2. 语言风格要时尚专业，同时亲切易懂\n" +
                        "3. 段落清晰，结构合理\n" +
                        "4. 总字数在300-500字之间\n" +
                        "5. 不要使用emoji表情符号");
                break;
                
            case "数码科技":
                theme.setDescription("分享数码产品评测、科技新闻等内容");
                theme.setSearchKeywords(new String[]{"数码产品", "科技评测", "数码推荐", "科技新闻"});
                theme.setImageKeywords(new String[]{"数码产品", "科技", "电子设备", "数码评测"});
                theme.setStyle("专业客观");
                theme.setPromptTemplate("请以小红书博主的口吻，撰写一篇关于数码科技的笔记，主题是'%s'。要求：\n" +
                        "1. 内容要有专业的产品评测或科技观点，包括功能介绍、使用体验等\n" +
                        "2. 语言风格要专业但不晦涩，通俗易懂\n" +
                        "3. 段落清晰，结构合理\n" +
                        "4. 总字数在300-500字之间\n" +
                        "5. 不要使用emoji表情符号");
                break;
                
            default:
                // 默认配置
                theme.setDescription("通用主题");
                theme.setSearchKeywords(new String[]{themeName});
                theme.setImageKeywords(new String[]{themeName});
                theme.setStyle("自然流畅");
                theme.setPromptTemplate("请以小红书博主的口吻，撰写一篇关于" + themeName + "的笔记。要求：\n" +
                        "1. 内容要真实、有体验感\n" +
                        "2. 语言风格要亲切自然\n" +
                        "3. 段落清晰，结构合理\n" +
                        "4. 总字数在300-500字之间\n" +
                        "5. 不要使用emoji表情符号");
                break;
        }
        
        return theme;
    }
} 