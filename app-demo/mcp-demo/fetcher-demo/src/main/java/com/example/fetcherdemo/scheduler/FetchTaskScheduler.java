package com.example.fetcherdemo.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fetcherdemo.entity.FetchTask;
import com.example.fetcherdemo.service.FetchTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 抓取任务调度器
 * 
 * @author example
 * @date 2023-06-01
 */
@Component
@Slf4j
public class FetchTaskScheduler {
    
    private final FetchTaskService fetchTaskService;
    
    public FetchTaskScheduler(FetchTaskService fetchTaskService) {
        this.fetchTaskService = fetchTaskService;
    }
    
    /**
     * 每5分钟执行一次，检查是否有待执行的任务
     */
    @Scheduled(fixedRate = 300000)
    public void checkPendingTasks() {
        log.info("开始检查待执行的任务");
        
        try {
            // 查询待执行的任务
            LambdaQueryWrapper<FetchTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FetchTask::getStatus, FetchTask.Status.PENDING.getValue());
            queryWrapper.orderByAsc(FetchTask::getCreateTime);
            queryWrapper.last("limit 10"); // 每次最多处理10个任务
            
            List<FetchTask> pendingTasks = fetchTaskService.list(queryWrapper);
            log.info("发现{}个待执行的任务", pendingTasks.size());
            
            // 逐个执行任务
            for (FetchTask task : pendingTasks) {
                try {
                    fetchTaskService.executeTask(task.getId());
                } catch (Exception e) {
                    log.error("执行任务异常, ID: {}, 异常: {}", task.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("检查待执行任务异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每天凌晨1点执行，检查是否有定时任务需要重新调度
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkScheduledTasks() {
        log.info("开始检查定时任务");
        
        try {
            // 查询定时任务
            LambdaQueryWrapper<FetchTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FetchTask::getTaskType, FetchTask.TaskType.SCHEDULED.name());
            queryWrapper.ne(FetchTask::getStatus, FetchTask.Status.RUNNING.getValue()); // 排除正在运行的
            
            List<FetchTask> scheduledTasks = fetchTaskService.list(queryWrapper);
            log.info("发现{}个定时任务", scheduledTasks.size());
            
            // 重置任务状态为待执行
            for (FetchTask task : scheduledTasks) {
                task.setStatus(FetchTask.Status.PENDING.getValue());
                task.setSuccessCount(0);
                task.setFailCount(0);
                fetchTaskService.updateById(task);
                log.info("重置定时任务状态, ID: {}, 名称: {}", task.getId(), task.getTaskName());
            }
        } catch (Exception e) {
            log.error("检查定时任务异常: {}", e.getMessage(), e);
        }
    }
} 