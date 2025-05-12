package com.example.publishdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用程序启动测试
 */
@SpringBootTest
@ActiveProfiles("test") // 使用测试配置文件
class PublishDemoApplicationTests {

    @Test
    void contextLoads() {
        // 测试应用程序上下文是否成功加载
    }

} 