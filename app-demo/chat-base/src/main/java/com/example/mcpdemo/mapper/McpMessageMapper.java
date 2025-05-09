package com.example.mcpdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mcpdemo.entity.McpMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * McpMessage的MyBatis Plus Mapper接口
 */
@Mapper
public interface McpMessageMapper extends BaseMapper<McpMessage> {
    
    /**
     * 根据会话ID查询消息列表，按序号升序排列
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Select("SELECT * FROM mcp_message WHERE session_id = #{sessionId} ORDER BY sequence ASC")
    List<McpMessage> findBySessionIdOrderBySequenceAsc(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID查询消息列表，按序号降序排列
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Select("SELECT * FROM mcp_message WHERE session_id = #{sessionId} ORDER BY sequence DESC")
    List<McpMessage> findBySessionIdOrderBySequenceDesc(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID查询消息数量
     * @param sessionId 会话ID
     * @return 消息数量
     */
    @Select("SELECT COUNT(*) FROM mcp_message WHERE session_id = #{sessionId}")
    long countBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 获取会话最后一条消息
     * @param sessionId 会话ID
     * @return 最后一条消息
     */
    @Select("SELECT * FROM mcp_message WHERE session_id = #{sessionId} ORDER BY sequence DESC LIMIT 1")
    McpMessage findFirstBySessionIdOrderBySequenceDesc(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID和角色查询消息
     * @param sessionId 会话ID
     * @param role 角色
     * @return 消息列表
     */
    @Select("SELECT * FROM mcp_message WHERE session_id = #{sessionId} AND role = #{role} ORDER BY sequence ASC")
    List<McpMessage> findBySessionIdAndRoleOrderBySequenceAsc(@Param("sessionId") String sessionId, @Param("role") String role);
    
    /**
     * 删除会话的所有消息
     * @param sessionId 会话ID
     */
    @Delete("DELETE FROM mcp_message WHERE session_id = #{sessionId}")
    void deleteBySessionId(@Param("sessionId") String sessionId);
} 