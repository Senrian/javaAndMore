# MCP 示例项目

本项目是 Model Context Protocol (MCP) 的示例实现，展示了如何使用 MCP 协议构建大模型应用。

## 项目简介

MCP (Model Context Protocol) 是一种开放的通信协议，旨在标准化大模型应用开发，提供统一接口，支持多模型接入。本示例项目实现了 MCP 的核心功能，包括：

- 基础对话功能
- 会话管理
- 流式响应
- 多模型支持
- 工具调用（模拟）
- 多模态支持（模拟）

## 技术栈

- Java 11
- Spring Boot 2.7.9
- Spring Data JPA
- MySQL 数据库
- Thymeleaf 模板引擎
- Bootstrap 5
- MCP Kit 1.1.0

## 快速开始

### 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 5.7+

### 配置数据库

项目默认使用以下数据库配置：

```properties
spring.datasource.url=jdbc:mysql://81.70.252.170:3306/common?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=common
spring.datasource.password=4ccymwAhc5Y7GsPN
```

可以在 `application.yml` 中修改为自己的数据库配置。

### 构建与运行

1. 克隆项目

```bash
git clone https://github.com/yourusername/mcp-demo.git
cd mcp-demo
```

2. 编译项目

```bash
mvn clean package
```

3. 运行项目

```bash
java -jar target/mcp-demo-0.0.1-SNAPSHOT.jar
```

或者使用 Maven 运行：

```bash
mvn spring-boot:run
```

4. 访问应用

打开浏览器访问：http://localhost:8080

## 功能演示

### 基础对话

- 访问首页，点击"开始新对话"
- 输入问题，点击发送
- 查看 AI 回复

### 流式响应

- 在聊天页面，确保"流式响应"开关已开启
- 输入问题，点击发送
- 观察打字机效果的实时响应

### 会话管理

- 创建多个对话，在首页可以查看所有会话
- 点击任意会话卡片可以继续对话
- 会话会自动保存上下文历史

### 多模型支持

- 在聊天页面，使用模型选择下拉框切换不同模型
- 支持 GPT-3.5、GPT-4、文心一言等多种模型（模拟）

## 表结构
```aiignore
-- MCP 数据库表结构创建脚本

-- 创建 MCP 会话表
CREATE TABLE IF NOT EXISTS `mcp_session` (
  `id` varchar(255) NOT NULL COMMENT '会话ID',
  `title` varchar(255) DEFAULT '新对话' COMMENT '会话标题',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '最后更新时间',
  `user_id` varchar(255) NOT NULL COMMENT '用户ID',
  `model` varchar(50) DEFAULT NULL COMMENT '使用的模型',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE, ARCHIVED',
  `system_prompt` text DEFAULT NULL COMMENT '系统提示词',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP会话表';

-- 创建 MCP 消息表
CREATE TABLE IF NOT EXISTS `mcp_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` varchar(255) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色：system, user, assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `sequence` int(11) NOT NULL COMMENT '序号（消息顺序）',
  `model` varchar(50) DEFAULT NULL COMMENT '使用的模型',
  PRIMARY KEY (`id`),
  KEY `idx_session_sequence` (`session_id`, `sequence`),
  KEY `idx_session_role` (`session_id`, `role`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MCP消息表';

-- 创建测试实体表
CREATE TABLE IF NOT EXISTS `test_entity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='测试实体表';

-- 插入测试数据
INSERT INTO `test_entity` (`name`) VALUES ('测试数据1');
INSERT INTO `test_entity` (`name`) VALUES ('测试数据2');
```

## API 文档

### 主要接口

- `POST /api/mcp/chat` - 发送聊天请求
- `POST /api/mcp/chat/stream` - 发送流式聊天请求
- `POST /api/mcp/sessions` - 创建新会话
- `GET /api/mcp/sessions/{sessionId}` - 获取会话信息
- `GET /api/mcp/sessions` - 获取会话列表
- `DELETE /api/mcp/sessions/{sessionId}` - 删除会话
- `GET /api/mcp/sessions/{sessionId}/messages` - 获取会话消息历史

详细 API 文档请参考"功能演示"页面的"API文档"选项卡。

## 项目结构

```
mcp-demo/
├── src/main/java/com/example/mcpdemo/
│   ├── config/             # 配置类
│   ├── controller/         # 控制器
│   ├── dto/                # 数据传输对象
│   ├── entity/             # 实体类
│   ├── repository/         # 数据访问层
│   ├── service/            # 服务层
│   └── McpDemoApplication.java  # 主应用类
├── src/main/resources/
│   ├── templates/          # Thymeleaf模板
│   ├── static/             # 静态资源
│   └── application.yml     # 应用配置
└── pom.xml                 # Maven配置
```

## 参考资料

- [MCP 官方文档](https://mcp.so/)
- [Awesome MCP Servers](https://github.com/punkpeye/awesome-mcp-servers)
- [ModelScope MCP](https://www.modelscope.cn/mcp)

## 许可证

本项目采用 MIT 许可证。
