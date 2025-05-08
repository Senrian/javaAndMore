# Seata 分布式事务示例项目

本项目是一个使用 Seata 实现分布式事务的示例应用，主要用于演示如何在微服务架构中使用 Seata 保证事务一致性。

## 项目架构

本项目包含以下几个模块：

- **common-service**：公共服务模块，包含实体类、DTO、异常处理等共用代码
- **order-service**：订单服务，负责创建订单
- **storage-service**：库存服务，负责扣减商品库存
- **account-service**：账户服务，负责扣减用户账户余额
- **business-service**：业务服务，作为统一入口调用订单、库存、账户服务
- **frontend**：前端服务，提供用户界面

## 技术栈

- Spring Boot 2.6.x
- Spring Cloud 2021.0.4
- Spring Cloud Alibaba 2021.0.4.0
- Seata 1.7.0
- Nacos 2.2.0
- MyBatis Plus 3.5.2
- MySQL 8.0

## 项目准备

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Nacos 服务器
- Seata 服务器

### 2. 数据库初始化

在 MySQL 中执行 `db/init.sql` 脚本，创建所需的表和初始测试数据。

### 3. Seata Server 配置

确保你已经安装并启动了 Seata 服务器，并配置了对应的 Nacos 注册中心。

## 错误解决：Table 'common.undo_log' doesn't exist

当你遇到 `Table 'common.undo_log' doesn't exist` 错误时，需要在数据库中创建 `undo_log` 表。Seata 的 AT 模式需要这个表来存储事务回滚信息。

### 步骤

1. 连接到你的 MySQL 数据库：
```
mysql -u username -p -h host
```

2. 选择 `common` 数据库：
```
USE common;
```

3. 执行以下 SQL 语句创建 `undo_log` 表：
```sql
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';

ALTER TABLE `undo_log` ADD INDEX `ix_log_created` (`log_created`);
```

4. 验证表是否创建成功：
```
SHOW TABLES;
```

### 注意事项

- 必须在每个参与分布式事务的数据库中创建 `undo_log` 表
- 表结构必须完全一致
- 创建表后重启应用程序

## 启动服务

按照以下顺序启动各个服务：

1. 启动 Nacos 服务器
2. 启动 Seata 服务器

bin/seata-server.sh -h 0.0.0.0 -p 8091

3. 启动 storage-service (库存服务)
4. 启动 account-service (账户服务)
5. 启动 order-service (订单服务)
6. 启动 business-service (业务服务)
7. 启动 frontend (前端服务)

## 测试场景

本项目支持以下测试场景：

### 1. 正常下单

通过前端页面进行正常下单操作，整个流程包括：

- 创建订单
- 扣减库存
- 扣减账户余额
- 修改订单状态

所有操作都在一个分布式事务中完成，任何一步失败都会导致整个事务回滚。

### 2. 异常下单测试

通过前端页面的"异常下单"按钮，模拟业务异常情况。在这种情况下，订单创建后，业务服务会抛出一个异常，导致整个分布式事务回滚，从而验证 Seata 的分布式事务能力。

### 3. 模拟超时

在 account-service 的 AccountServiceImpl 中，有一段被注释的代码可以模拟处理超时的情况。取消注释后，当扣减金额超过 100 时，会休眠 10 秒，触发 Seata 的超时回滚机制。

## 功能演示

1. 访问前端页面: http://localhost:8090
2. 在表单中填写用户ID、商品ID、数量和金额
3. 点击"正常下单"按钮，观察事务执行情况
4. 点击"异常下单"按钮，观察事务回滚情况

## 扩展和定制

本项目提供了基础的分布式事务示例，你可以基于此进行扩展和定制：

1. 添加更多的业务场景
2. 实现更复杂的事务流程
3. 优化异常处理和重试机制
4. 增加日志记录和监控功能
5. 实现多种事务模式（如 TCC、Saga 等）
