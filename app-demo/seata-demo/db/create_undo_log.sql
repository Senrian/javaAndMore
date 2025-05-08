-- undo_log表创建脚本，使用[your db]:3306/common数据库
-- 注意：该表必须与业务表处于同一个数据库中

DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
    `branch_id` BIGINT NOT NULL COMMENT 'branch transaction id',
    `xid` VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context` VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB NOT NULL COMMENT 'rollback info',
    `log_status` INT(11) NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created` DATETIME(6) NOT NULL COMMENT 'create datetime',
    `log_modified` DATETIME(6) NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table';

ALTER TABLE `undo_log` ADD INDEX `idx_gmt_modified` (`log_modified`);
ALTER TABLE `undo_log` ADD INDEX `idx_status` (`log_status`);

-- 提示信息
SELECT 'undo_log表已创建成功，使用与Seata 1.7.0兼容的表结构' as '执行结果'; 