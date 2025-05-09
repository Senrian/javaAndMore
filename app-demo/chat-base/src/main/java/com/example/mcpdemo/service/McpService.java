package com.example.mcpdemo.service;

import com.example.mcpdemo.dto.McpMessageDto;
import com.example.mcpdemo.dto.McpRequest;
import com.example.mcpdemo.dto.McpResponse;
import com.example.mcpdemo.entity.McpSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * MCP服务接口
 */
public interface McpService {

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param model 模型名称
     * @param systemPrompt 系统提示词
     * @return 会话对象
     */
    McpSession createSession(String userId, String model, String systemPrompt);

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话对象
     */
    McpSession getSession(String sessionId);

    /**
     * 获取用户的会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<McpSession> listSessions(String userId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<McpMessageDto> getMessages(String sessionId);

    /**
     * 处理普通请求
     *
     * @param request MCP请求
     * @return MCP响应
     */
    McpResponse chat(McpRequest request);

    /**
     * 处理流式请求
     *
     * @param request MCP请求
     * @return SSE事件发送器
     */
    SseEmitter chatStream(McpRequest request);

    /**
     * 清理过期会话
     */
    void cleanupExpiredSessions();
} 