# 小红书内容生成与发布系统

这是一个使用Spring Boot AI和MCP工具的示例项目，用于自动生成小红书内容并发布。

## 功能特点

- 使用DeepSeek AI生成高质量文案
- 使用MCP Fetch工具抓取网页内容和图片
- 使用MCP小红书工具发布内容
- 支持多种主题的内容生成
- 提供Web界面进行操作

## 技术栈

- Spring Boot 3.2.4
- Spring AI 0.8.1（MCP支持）
- Unirest HTTP客户端
- Bootstrap 5前端框架

## 系统架构

系统由以下几个主要模块组成：

1. **DeepSeek服务**：调用DeepSeek AI API生成文案
2. **Fetch服务**：使用MCP工具抓取网页内容和图片
3. **小红书发布服务**：使用MCP工具发布内容到小红书
4. **内容生成服务**：整合上述服务，提供完整的内容生成和发布流程
5. **Web界面**：提供用户友好的操作界面

## 快速开始

### 前提条件

- JDK 17+
- Maven 3.6+
- DeepSeek API密钥
- MCP工具配置

### 构建与运行

1. 克隆项目
```bash
git clone https://github.com/yourusername/publish-demo.git
cd publish-demo
```

2. 构建项目
```bash
mvn clean package
```

3. 运行项目
```bash
java -jar target/publish-demo-0.0.1-SNAPSHOT.jar
```

4. 访问Web界面
```
http://localhost:8080
```

## 配置说明

主要配置在`application.yml`文件中：

```yaml
spring:
  ai:
    mcp:
      servers:
        fetch:
          url: https://www.modelscope.cn/mcp/servers/@modelcontextprotocol/fetch
        xhs:
          url: https://www.modelscope.cn/mcp/servers/@XGenerationLab/xhs_mcp_server

deepseek:
  api:
    url: https://api.ap.siliconflow.com/v1/chat/completions
    token: your-api-token
    model: deepseek-ai/DeepSeek-V3

content:
  default-themes:
    - 美食探店
    - 旅行日记
    - 生活方式
    - 时尚穿搭
    - 数码科技
```

## API接口

系统提供以下REST API接口：

- `GET /api/content/themes` - 获取所有可用主题
- `GET /api/content/themes/{themeName}` - 获取指定主题
- `POST /api/content/themes` - 添加新主题
- `GET /api/content/generate/{themeName}` - 生成指定主题的内容
- `POST /api/content/publish/{themeName}` - 发布指定主题的内容

## 注意事项

- 小红书发布内容不能包含emoji表情
- 图片数量有限制，默认最多9张
- DeepSeek API调用需要有效的API密钥

## 许可证

MIT 