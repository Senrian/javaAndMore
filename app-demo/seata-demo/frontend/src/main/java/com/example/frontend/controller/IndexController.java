package com.example.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器
 */
@Controller
public class IndexController {
    
    /**
     * 重定向到主页
     * @return 重定向路径
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }
} 