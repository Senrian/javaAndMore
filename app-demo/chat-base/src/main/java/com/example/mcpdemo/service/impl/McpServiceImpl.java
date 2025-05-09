package com.example.mcpdemo.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mcpdemo.config.McpConfig;
import com.example.mcpdemo.dto.McpMessageDto;
import com.example.mcpdemo.dto.McpRequest;
import com.example.mcpdemo.dto.McpResponse;
import com.example.mcpdemo.entity.McpMessage;
import com.example.mcpdemo.entity.McpSession;
import com.example.mcpdemo.mapper.McpMessageMapper;
import com.example.mcpdemo.mapper.McpSessionMapper;
import com.example.mcpdemo.service.McpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP服务实现类
 */
@Service
@Slf4j
public class McpServiceImpl implements McpService {

    @Autowired
    private McpSessionMapper sessionMapper;

    @Autowired
    private McpMessageMapper messageMapper;

    @Autowired
    private McpConfig mcpConfig;

    // 缓存活跃的SSE连接
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public McpSession createSession(String userId, String model, String systemPrompt) {
        log.debug("Creating new session for user: {}, model: {}", userId, model);
        
        if (model == null || model.isEmpty()) {
            model = mcpConfig.getDefaultModel();
        }
        
        String sessionId = UUID.randomUUID().toString();
        Date now = new Date();
        
        McpSession session = McpSession.builder()
                .id(sessionId)
                .userId(userId)
                .model(model)
                .createTime(now)
                .updateTime(now)
                .status("ACTIVE")
                .systemPrompt(systemPrompt)
                .title("新对话")
                .build();
        
        sessionMapper.insert(session);
        
        // 如果有系统提示词，保存为第一条消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            McpMessage systemMessage = McpMessage.builder()
                    .sessionId(sessionId)
                    .role("system")
                    .content(systemPrompt)
                    .createTime(now)
                    .sequence(0)
                    .model(model)
                    .build();
            
            messageMapper.insert(systemMessage);
        }
        
        log.info("Created new session: {}", sessionId);
        return session;
    }

    @Override
    public McpSession getSession(String sessionId) {
        log.debug("Fetching session: {}", sessionId);
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public List<McpSession> listSessions(String userId) {
        log.debug("Listing sessions for user: {}", userId);
        return sessionMapper.findByUserIdAndStatusOrderByUpdateTimeDesc(userId, "ACTIVE");
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        log.debug("Deleting session: {}", sessionId);
        QueryWrapper<McpMessage> messageWrapper = new QueryWrapper<>();
        messageWrapper.eq("session_id", sessionId);
        messageMapper.delete(messageWrapper);
        sessionMapper.deleteById(sessionId);
        log.info("Deleted session: {}", sessionId);
    }

    @Override
    public List<McpMessageDto> getMessages(String sessionId) {
        log.debug("Fetching messages for session: {}", sessionId);
        
        QueryWrapper<McpMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId).orderByAsc("sequence");
        List<McpMessage> messages = messageMapper.selectList(queryWrapper);
        
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public McpResponse chat(McpRequest request) {
        log.debug("Processing chat request: {}", JSON.toJSONString(request));
        
        String sessionId = request.getSessionId();
        McpSession session;
        
        // 创建或获取会话
        if (sessionId == null || sessionId.isEmpty()) {
            session = createSession(
                    "anonymous", 
                    request.getModel() != null ? request.getModel() : mcpConfig.getDefaultModel(),
                    request.getSystemPrompt()
            );
            sessionId = session.getId();
        } else {
            session = getSession(sessionId);
            if (session == null) {
                log.error("Session not found: {}", sessionId);
                return McpResponse.builder()
                        .error("Session not found")
                        .build();
            }
        }
        
        // 保存用户消息
        McpMessage userMessage = saveUserMessage(sessionId, request);
        
        // 模拟模型响应
        String responseContent = generateMockResponse(request);
        
        // 保存助手消息
        McpMessage assistantMessage = saveAssistantMessage(sessionId, responseContent, session.getModel());
        
        // 更新会话
        updateSession(session);
        
        // 构建响应
        return buildResponse(sessionId, assistantMessage, session.getModel());
    }

    @Override
    public SseEmitter chatStream(McpRequest request) {
        log.debug("Processing stream chat request: {}", JSON.toJSONString(request));
        
        String sessionId = request.getSessionId();
        McpSession session;
        
        // 创建或获取会话
        if (sessionId == null || sessionId.isEmpty()) {
            session = createSession(
                    "anonymous", 
                    request.getModel() != null ? request.getModel() : mcpConfig.getDefaultModel(),
                    request.getSystemPrompt()
            );
            sessionId = session.getId();
        } else {
            session = getSession(sessionId);
            if (session == null) {
                log.error("Session not found: {}", sessionId);
                return null;
            }
        }
        
        final String finalSessionId = sessionId;
        
        // 保存用户消息
        McpMessage userMessage = saveUserMessage(finalSessionId, request);
        
        // 创建SSE发送器
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        
        // 存储到活跃连接映射
        activeEmitters.put(finalSessionId, emitter);
        
        // 设置SSE完成、超时和错误时的回调
        emitter.onCompletion(() -> {
            log.debug("SSE completed for session: {}", finalSessionId);
            activeEmitters.remove(finalSessionId);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for session: {}", finalSessionId);
            activeEmitters.remove(finalSessionId);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            log.error("SSE error for session: {}", finalSessionId, ex);
            activeEmitters.remove(finalSessionId);
        });
        
        // 启动后台线程模拟流式响应
        final String model = session.getModel();
        
        new Thread(() -> {
            try {
                // 模拟流式返回
                sendStreamResponse(emitter, finalSessionId, model);
                
                // 更新会话
                updateSession(session);
            } catch (Exception e) {
                log.error("Error in stream response thread", e);
                try {
                    emitter.send(McpResponse.builder()
                            .error("Internal server error")
                            .sessionId(finalSessionId)
                            .done(true)
                            .build());
                    emitter.complete();
                } catch (Exception ex) {
                    log.error("Error sending error response", ex);
                }
            }
        }).start();
        
        return emitter;
    }

    @Override
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Running expired sessions cleanup task");
        
        // 计算超时时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, -(int)mcpConfig.getSessionTimeout());
        Date expiryTime = calendar.getTime();
        
        // 查找过期会话
        List<McpSession> expiredSessions = sessionMapper.findExpiredSessions(expiryTime);
        
        log.info("Found {} expired sessions", expiredSessions.size());
        
        // 删除过期会话及其消息
        for (McpSession session : expiredSessions) {
            QueryWrapper<McpMessage> messageWrapper = new QueryWrapper<>();
            messageWrapper.eq("session_id", session.getId());
            messageMapper.delete(messageWrapper);
            sessionMapper.deleteById(session.getId());
        }
    }

    // 辅助方法 - 保存用户消息
    private McpMessage saveUserMessage(String sessionId, McpRequest request) {
        log.debug("Saving user message for session: {}", sessionId);
        
        McpMessageDto userMessageDto = request.getMessages().get(request.getMessages().size() - 1);
        
        QueryWrapper<McpMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId);
        long messageCount = messageMapper.selectCount(queryWrapper);
        
        McpMessage userMessage = McpMessage.builder()
                .sessionId(sessionId)
                .role("user")
                .content(userMessageDto.getContent())
                .createTime(new Date())
                .sequence((int) messageCount)
                .model(request.getModel())
                .build();
        
        messageMapper.insert(userMessage);
        return userMessage;
    }

    // 辅助方法 - 保存助手消息
    private McpMessage saveAssistantMessage(String sessionId, String content, String model) {
        log.debug("Saving assistant message for session: {}", sessionId);
        
        QueryWrapper<McpMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId);
        long messageCount = messageMapper.selectCount(queryWrapper);
        
        McpMessage assistantMessage = McpMessage.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(content)
                .createTime(new Date())
                .sequence((int) messageCount)
                .model(model)
                .build();
        
        messageMapper.insert(assistantMessage);
        return assistantMessage;
    }

    // 辅助方法 - 更新会话
    private void updateSession(McpSession session) {
        log.debug("Updating session: {}", session.getId());
        
        session.setUpdateTime(new Date());
        
        // 尝试设置会话标题
        if (session.getTitle().equals("新对话")) {
            QueryWrapper<McpMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("session_id", session.getId())
                   .eq("role", "user")
                   .orderByAsc("sequence")
                   .last("LIMIT 1");
            McpMessage lastUserMessage = messageMapper.selectOne(queryWrapper);
            
            if (lastUserMessage != null) {
                String content = lastUserMessage.getContent();
                if (content.length() > 30) {
                    content = content.substring(0, 27) + "...";
                }
                session.setTitle(content);
            }
        }
        
        sessionMapper.updateById(session);
    }

    // 辅助方法 - 构建响应
    private McpResponse buildResponse(String sessionId, McpMessage message, String model) {
        log.debug("Building response for session: {}", sessionId);
        
        McpMessageDto messageDto = convertToDto(message);
        
        return McpResponse.builder()
                .sessionId(sessionId)
                .message(messageDto)
                .model(model)
                .done(true)
                .requestId(UUID.randomUUID().toString())
                .usageTokens(calculateTokens(message.getContent()))
                .build();
    }

    // 辅助方法 - 转换消息实体到DTO
    private McpMessageDto convertToDto(McpMessage message) {
        return McpMessageDto.builder()
                .role(message.getRole())
                .content(message.getContent())
                .id(message.getId().toString())
                .timestamp(message.getCreateTime().getTime())
                .build();
    }

    // 辅助方法 - 模拟计算令牌数
    private int calculateTokens(String text) {
        return text.length() / 4;  // 简单估算
    }

    // 辅助方法 - 模拟生成响应内容
    private String generateMockResponse(McpRequest request) {
        log.debug("Generating mock response");
        
        String userMessage = request.getMessages().get(request.getMessages().size() - 1).getContent();
        
        if (userMessage.contains("你好") || userMessage.contains("Hello") || userMessage.contains("hello")) {
            return "你好！我是 MCP (Model Context Protocol) 服务，很高兴为您服务。有什么可以帮助您的吗？";
        } else if (userMessage.contains("什么是MCP") || userMessage.contains("介绍MCP")) {
            return "MCP (Model Context Protocol) 是一种开放的通信协议，旨在标准化大模型应用开发。它提供了一套统一的接口，使应用开发者可以更轻松地接入不同的大语言模型服务。MCP的核心功能包括上下文管理、会话控制、多模态输入输出支持等。";
        } else if (userMessage.contains("特点") || userMessage.contains("优势")) {
            return "MCP协议的主要特点和优势包括：\n\n1. 统一接口：提供标准化的API接口，降低对接不同模型的成本\n2. 上下文管理：自动处理对话上下文，支持长对话\n3. 多模型兼容：支持同时接入多种大语言模型\n4. 扩展性强：可以通过插件机制扩展功能\n5. 安全可控：提供身份验证和访问控制机制\n6. 开放生态：开源协议，鼓励社区共建";
        } else if (userMessage.contains("使用场景") || userMessage.contains("应用")) {
            return "MCP协议的应用场景非常广泛，包括但不限于：\n\n1. 聊天机器人和智能客服系统\n2. 知识库问答系统\n3. 内容生成和创作辅助工具\n4. 代码辅助和智能编程工具\n5. 多模态应用，如图像理解和生成\n6. 专业领域的决策支持系统\n7. 教育和培训系统";
        } else {
            return "感谢您的提问。作为MCP示例服务，我可以帮助您了解MCP协议的各种功能和应用。您可以询问关于MCP的定义、特点、使用场景、技术实现等方面的问题。如果您想体验更丰富的功能，可以尝试流式响应、多模态输入、工具调用等高级特性。";
        }
    }

    // 辅助方法 - 发送流式响应
    private void sendStreamResponse(SseEmitter emitter, String sessionId, String model) throws Exception {
        log.debug("Sending stream response for session: {}", sessionId);
        
        try {
            String fullResponse = "感谢您使用MCP流式响应功能。Model Context Protocol (MCP) 是一种标准化的大模型应用开发协议，它可以帮助开发者更容易地构建基于大语言模型的应用程序。流式响应是MCP的一个重要特性，它可以提供更好的用户体验，特别是在生成长文本时。";
            
            // 将完整响应分成多个小片段
            List<String> chunks = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            
            // 将全文本按词分割
            String[] words = fullResponse.split("(?<=\\s)");
            for (String word : words) {
                sb.append(word);
                
                // 每积累5-10个字就发送一次
                if (sb.length() >= 5 && Math.random() > 0.7) {
                    chunks.add(sb.toString());
                    sb = new StringBuilder();
                }
            }
            
            // 添加剩余部分
            if (sb.length() > 0) {
                chunks.add(sb.toString());
            }
            
            // 保存完整的助手消息
            McpMessage assistantMessage = saveAssistantMessage(sessionId, fullResponse, model);
            
            // 流式发送小片段
            StringBuilder cumulativeResponse = new StringBuilder();
            
            for (int i = 0; i < chunks.size(); i++) {
                boolean isLast = (i == chunks.size() - 1);
                cumulativeResponse.append(chunks.get(i));
                
                McpMessageDto messageDto = McpMessageDto.builder()
                        .role("assistant")
                        .content(cumulativeResponse.toString())
                        .id(assistantMessage.getId().toString())
                        .timestamp(System.currentTimeMillis())
                        .build();
                
                McpResponse response = McpResponse.builder()
                        .sessionId(sessionId)
                        .message(messageDto)
                        .model(model)
                        .done(isLast)
                        .requestId(UUID.randomUUID().toString())
                        .usageTokens(isLast ? calculateTokens(fullResponse) : null)
                        .build();
                
                try {
                    emitter.send(response);
                    log.debug("Sent chunk {} of {} for session: {}", i+1, chunks.size(), sessionId);
                } catch (IOException e) {
                    log.error("Error sending SSE chunk: {} for session: {}", e.getMessage(), sessionId, e);
                    throw e;
                }
                
                // 模拟延迟
                Thread.sleep(100 + (int)(Math.random() * 150));
            }
            
            log.debug("Successfully completed stream response for session: {}", sessionId);
            emitter.complete();
        } catch (Exception e) {
            log.error("Error in sendStreamResponse for session {}: {}", sessionId, e.getMessage(), e);
            try {
                McpResponse errorResponse = McpResponse.builder()
                        .error("Internal server error: " + e.getMessage())
                        .sessionId(sessionId)
                        .done(true)
                        .build();
                emitter.send(errorResponse);
                emitter.complete();
            } catch (Exception ex) {
                log.error("Error sending error response for session {}: {}", sessionId, ex.getMessage(), ex);
            }
            throw e;
        }
    }
} 