package com.example.publishdemo.controller;

import com.example.publishdemo.client.ManagedMcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具测试控制器
 * 用于测试MCP工具的基本功能
 */
@RestController
@RequestMapping("/api/test")
public class McpTestController {

    private static final Logger log = LoggerFactory.getLogger(McpTestController.class);

    @Autowired
    @Qualifier("fetchMcpClient")
    private ManagedMcpClient fetchMcpClient;
    
    @Autowired
    @Qualifier("xhsMcpClient")
    private ManagedMcpClient xhsMcpClient;
    
    // ... existing code ...
}