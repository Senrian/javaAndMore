package com.example.mcpdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mcpdemo.entity.McpSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * McpSession的MyBatis Plus Mapper接口
 */
@Mapper
public interface McpSessionMapper extends BaseMapper<McpSession> {

    /**
     * 根据用户ID查询会话，按更新时间倒序排序
     * @param userId 用户ID
     * @return 会话列表
     */
    @Select("SELECT * FROM mcp_session WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<McpSession> findByUserIdOrderByUpdateTimeDesc(@Param("userId") String userId);

    /**
     * 根据状态查询会话
     * @param status 状态
     * @return 会话列表
     */
    @Select("SELECT * FROM mcp_session WHERE status = #{status}")
    List<McpSession> findByStatus(@Param("status") String status);

    /**
     * 查询过期会话
     * @param expiryTime 过期时间
     * @return 过期会话列表
     */
    @Select("SELECT * FROM mcp_session WHERE update_time < #{expiryTime}")
    List<McpSession> findExpiredSessions(@Param("expiryTime") Date expiryTime);

    /**
     * 根据用户ID和状态查询会话，按更新时间倒序排序
     * @param userId 用户ID
     * @param status 状态
     * @return 会话列表
     */
    @Select("SELECT * FROM mcp_session WHERE user_id = #{userId} AND status = #{status} ORDER BY update_time DESC")
    List<McpSession> findByUserIdAndStatusOrderByUpdateTimeDesc(@Param("userId") String userId, @Param("status") String status);
} 