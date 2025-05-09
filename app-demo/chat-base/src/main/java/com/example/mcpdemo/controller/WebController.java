package com.example.mcpdemo.controller;

import com.example.mcpdemo.config.McpConfig;
import com.example.mcpdemo.entity.McpSession;
import com.example.mcpdemo.service.McpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Web页面控制器
 */
@Controller
@Slf4j
public class WebController {

    @Autowired
    private McpService mcpService;
    
    @Autowired
    private McpConfig mcpConfig;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        log.debug("Accessing index page");
        
        List<McpSession> sessions = mcpService.listSessions("anonymous");
        model.addAttribute("sessions", sessions);
        model.addAttribute("models", mcpConfig.getModels());
        model.addAttribute("defaultModel", mcpConfig.getDefaultModel());
        
        return "index";
    }
    
    /**
     * 聊天页面
     */
    @GetMapping("/chat")
    public String newChat(Model model) {
        log.debug("Accessing new chat page");
        
        model.addAttribute("models", mcpConfig.getModels());
        model.addAttribute("defaultModel", mcpConfig.getDefaultModel());
        
        return "chat";
    }
    
    /**
     * 已有会话聊天页面
     */
    @GetMapping("/chat/{sessionId}")
    public String existingChat(@PathVariable String sessionId, Model model) {
        log.debug("Accessing existing chat page for session: {}", sessionId);
        
        McpSession mcpSession = mcpService.getSession(sessionId);
        if (mcpSession == null) {
            return "redirect:/";
        }
        
        model.addAttribute("mcpSession", mcpSession);
        model.addAttribute("models", mcpConfig.getModels());
        model.addAttribute("messages", mcpService.getMessages(sessionId));
        
        return "chat";
    }
    
    /**
     * 关于MCP页面
     */
    @GetMapping("/about")
    public String about() {
        log.debug("Accessing about page");
        return "about";
    }
    
    /**
     * 演示页面
     */
    @GetMapping("/demo")
    public String demo(Model model) {
        log.debug("Accessing demo page");
        
        model.addAttribute("models", mcpConfig.getModels());
        model.addAttribute("defaultModel", mcpConfig.getDefaultModel());
        
        return "demo";
    }
} 