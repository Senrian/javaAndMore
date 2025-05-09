package com.example.mcpdemo.controller;

import com.example.mcpdemo.config.McpConfig;
import com.example.mcpdemo.dto.McpMessageDto;
import com.example.mcpdemo.dto.McpRequest;
import com.example.mcpdemo.dto.McpResponse;
import com.example.mcpdemo.entity.McpSession;
import com.example.mcpdemo.service.McpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP API控制器
 */
@RestController
@RequestMapping("/api/mcp")
@Slf4j
public class McpController {

    @Autowired
    private McpService mcpService;
    
    @Autowired
    private McpConfig mcpConfig;
    
    /**
     * 获取MCP服务信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        log.debug("Getting MCP service info");
        
        Map<String, Object> info = new HashMap<>();
        info.put("endpoint", mcpConfig.getEndpoint());
        info.put("defaultModel", mcpConfig.getDefaultModel());
        info.put("models", mcpConfig.getModels());
        info.put("maxContextSize", mcpConfig.getMaxContextSize());
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<McpSession> createSession(@RequestBody Map<String, String> request) {
        log.debug("Creating new session");
        
        String userId = request.getOrDefault("userId", "anonymous");
        String model = request.getOrDefault("model", mcpConfig.getDefaultModel());
        String systemPrompt = request.get("systemPrompt");
        
        McpSession session = mcpService.createSession(userId, model, systemPrompt);
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<McpSession> getSession(@PathVariable String sessionId) {
        log.debug("Getting session: {}", sessionId);
        
        McpSession session = mcpService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<McpSession>> listSessions(@RequestParam(defaultValue = "anonymous") String userId) {
        log.debug("Listing sessions for user: {}", userId);
        
        List<McpSession> sessions = mcpService.listSessions(userId);
        
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        log.debug("Deleting session: {}", sessionId);
        
        McpSession session = mcpService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        mcpService.deleteSession(sessionId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取会话的消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<McpMessageDto>> getMessages(@PathVariable String sessionId) {
        log.debug("Getting messages for session: {}", sessionId);
        
        McpSession session = mcpService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<McpMessageDto> messages = mcpService.getMessages(sessionId);
        
        return ResponseEntity.ok(messages);
    }
    
    /**
     * 处理聊天请求
     */
    @PostMapping("/chat")
    public ResponseEntity<McpResponse> chat(@RequestBody McpRequest request) {
        log.debug("Processing chat request");
        
        McpResponse response = mcpService.chat(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 处理流式聊天请求
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody McpRequest request) {
        log.debug("Processing stream chat request");
        
        // 设置流式响应标志
        request.setStream(true);
        
        return mcpService.chatStream(request);
    }
} 