-- 网页信息表
CREATE TABLE IF NOT EXISTS `web_page` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `url` VARCHAR(500) NOT NULL COMMENT '网页URL',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '网页标题',
    `content` LONGTEXT DEFAULT NULL COMMENT '网页内容',
    `html_content` LONGTEXT DEFAULT NULL COMMENT '网页HTML',
    `status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '抓取状态：0-未抓取，1-抓取成功，2-抓取失败',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_url` (`url`(255)),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网页信息表';

-- 任务记录表
CREATE TABLE IF NOT EXISTS `fetch_task` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型：SIMPLE-简单抓取，BATCH-批量抓取，SCHEDULED-定时抓取',
    `urls` TEXT NOT NULL COMMENT '抓取URL列表，多个URL用英文逗号分隔',
    `status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '任务状态：0-待执行，1-执行中，2-已完成，3-失败',
    `total_count` INT(11) NOT NULL DEFAULT '0' COMMENT '总URL数量',
    `success_count` INT(11) NOT NULL DEFAULT '0' COMMENT '成功数量',
    `fail_count` INT(11) NOT NULL DEFAULT '0' COMMENT '失败数量',
    `cron_expression` VARCHAR(100) DEFAULT NULL COMMENT 'Cron表达式，用于定时任务',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_task_type` (`task_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抓取任务表'; 