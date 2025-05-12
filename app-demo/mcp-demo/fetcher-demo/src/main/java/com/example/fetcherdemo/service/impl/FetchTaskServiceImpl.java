package com.example.fetcherdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fetcherdemo.entity.FetchTask;
import com.example.fetcherdemo.entity.WebPage;
import com.example.fetcherdemo.mapper.FetchTaskMapper;
import com.example.fetcherdemo.service.FetchTaskService;
import com.example.fetcherdemo.service.WebPageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * FetchTask服务实现类
 * 
 * @author example
 * @date 2023-06-01
 */
@Service
@Slf4j
public class FetchTaskServiceImpl extends ServiceImpl<FetchTaskMapper, FetchTask> implements FetchTaskService {

    private final WebPageService webPageService;

    public FetchTaskServiceImpl(WebPageService webPageService) {
        this.webPageService = webPageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FetchTask createSimpleTask(String taskName, String url) {
        log.info("创建简单抓取任务: {}, URL: {}", taskName, url);
        
        FetchTask task = new FetchTask();
        task.setTaskName(taskName);
        task.setTaskType(FetchTask.TaskType.SIMPLE.name());
        task.setUrls(url);
        task.setStatus(FetchTask.Status.PENDING.getValue());
        task.setTotalCount(1);
        task.setSuccessCount(0);
        task.setFailCount(0);
        
        save(task);
        log.info("简单抓取任务创建成功, ID: {}", task.getId());
        
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FetchTask createBatchTask(String taskName, List<String> urls) {
        log.info("创建批量抓取任务: {}, URL数量: {}", taskName, urls.size());
        
        FetchTask task = new FetchTask();
        task.setTaskName(taskName);
        task.setTaskType(FetchTask.TaskType.BATCH.name());
        task.setUrls(String.join(",", urls));
        task.setStatus(FetchTask.Status.PENDING.getValue());
        task.setTotalCount(urls.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        
        save(task);
        log.info("批量抓取任务创建成功, ID: {}", task.getId());
        
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FetchTask createScheduledTask(String taskName, List<String> urls, String cronExpression) {
        log.info("创建定时抓取任务: {}, URL数量: {}, Cron: {}", taskName, urls.size(), cronExpression);
        
        FetchTask task = new FetchTask();
        task.setTaskName(taskName);
        task.setTaskType(FetchTask.TaskType.SCHEDULED.name());
        task.setUrls(String.join(",", urls));
        task.setStatus(FetchTask.Status.PENDING.getValue());
        task.setTotalCount(urls.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setCronExpression(cronExpression);
        
        save(task);
        log.info("定时抓取任务创建成功, ID: {}", task.getId());
        
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeTask(Long taskId) {
        log.info("开始执行任务, ID: {}", taskId);
        
        // 查询任务
        FetchTask task = getById(taskId);
        if (task == null) {
            log.error("任务不存在, ID: {}", taskId);
            return false;
        }
        
        // 检查任务状态
        if (task.getStatus() == FetchTask.Status.RUNNING.getValue()) {
            log.warn("任务正在执行中, ID: {}", taskId);
            return false;
        }
        
        // 更新任务状态为执行中
        task.setStatus(FetchTask.Status.RUNNING.getValue());
        updateById(task);
        
        // 解析URL列表
        List<String> urls = Arrays.asList(task.getUrls().split(","));
        
        // 异步执行任务
        executeTaskAsync(task.getId(), urls);
        
        return true;
    }

    @Async
    protected void executeTaskAsync(Long taskId, List<String> urls) {
        log.info("开始异步执行任务, ID: {}, URL数量: {}", taskId, urls.size());
        
        try {
            // 批量抓取URL
            List<WebPage> results = webPageService.batchFetchAndSave(urls);
            
            // 统计结果
            long successCount = results.stream().filter(p -> p.getStatus() == 1).count();
            long failCount = results.stream().filter(p -> p.getStatus() == 2).count();
            
            // 更新任务状态
            FetchTask task = getById(taskId);
            task.setSuccessCount((int) successCount);
            task.setFailCount((int) failCount);
            task.setStatus(FetchTask.Status.COMPLETED.getValue());
            updateById(task);
            
            log.info("任务执行完成, ID: {}, 成功: {}, 失败: {}", taskId, successCount, failCount);
        } catch (Exception e) {
            log.error("任务执行异常, ID: {}, 异常: {}", taskId, e.getMessage(), e);
            
            // 更新任务状态为失败
            FetchTask task = getById(taskId);
            task.setStatus(FetchTask.Status.FAILED.getValue());
            updateById(task);
        }
    }

    @Override
    public Page<FetchTask> pageTask(String taskType, Integer status, Page<FetchTask> page) {
        LambdaQueryWrapper<FetchTask> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(taskType)) {
            queryWrapper.eq(FetchTask::getTaskType, taskType);
        }
        
        if (status != null) {
            queryWrapper.eq(FetchTask::getStatus, status);
        }
        
        queryWrapper.orderByDesc(FetchTask::getCreateTime);
        
        return page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(Long taskId) {
        log.info("取消任务, ID: {}", taskId);
        
        // 查询任务
        FetchTask task = getById(taskId);
        if (task == null) {
            log.error("任务不存在, ID: {}", taskId);
            return false;
        }
        
        // 只有待执行和执行中的任务可以取消
        if (task.getStatus() != FetchTask.Status.PENDING.getValue() && 
            task.getStatus() != FetchTask.Status.RUNNING.getValue()) {
            log.warn("任务状态不允许取消, ID: {}, 状态: {}", taskId, task.getStatus());
            return false;
        }
        
        // 更新任务状态为失败
        task.setStatus(FetchTask.Status.FAILED.getValue());
        updateById(task);
        
        log.info("任务已取消, ID: {}", taskId);
        return true;
    }
} 