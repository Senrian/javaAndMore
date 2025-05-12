package com.example.fetcherdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fetcherdemo.entity.FetchTask;

import java.util.List;

/**
 * FetchTask服务接口
 * 
 * @author example
 * @date 2023-06-01
 */
public interface FetchTaskService extends IService<FetchTask> {
    
    /**
     * 创建简单抓取任务
     * 
     * @param taskName 任务名称
     * @param url 要抓取的URL
     * @return 创建的任务
     */
    FetchTask createSimpleTask(String taskName, String url);
    
    /**
     * 创建批量抓取任务
     * 
     * @param taskName 任务名称
     * @param urls 要抓取的URL列表
     * @return 创建的任务
     */
    FetchTask createBatchTask(String taskName, List<String> urls);
    
    /**
     * 创建定时抓取任务
     * 
     * @param taskName 任务名称
     * @param urls 要抓取的URL列表
     * @param cronExpression Cron表达式
     * @return 创建的任务
     */
    FetchTask createScheduledTask(String taskName, List<String> urls, String cronExpression);
    
    /**
     * 执行任务
     * 
     * @param taskId 任务ID
     * @return 执行结果
     */
    boolean executeTask(Long taskId);
    
    /**
     * 分页查询任务
     * 
     * @param taskType 任务类型
     * @param status 任务状态
     * @param page 分页参数
     * @return 分页结果
     */
    Page<FetchTask> pageTask(String taskType, Integer status, Page<FetchTask> page);
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(Long taskId);
} 