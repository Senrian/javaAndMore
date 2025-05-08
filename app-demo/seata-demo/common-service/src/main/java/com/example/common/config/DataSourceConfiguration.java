package com.example.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * 使用Seata代理数据源
 */
@Configuration
public class DataSourceConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfiguration.class);

    /**
     * 配置Druid数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DruidDataSource druidDataSource() {
        logger.info("初始化Druid数据源");
        return new DruidDataSource();
    }

    /**
     * 配置Seata数据源代理
     * 替代自动代理配置，确保undo_log正确创建和使用
     */
    @Primary
    @Bean
    public DataSource dataSource(DruidDataSource druidDataSource) {
        logger.info("初始化Seata数据源代理，URL: {}", druidDataSource.getUrl());
        // 确保Druid初始化
        try {
            druidDataSource.init();
            logger.info("Druid数据源初始化成功");
        } catch (Exception e) {
            logger.error("Druid数据源初始化失败", e);
        }
        
        // AT模式的数据源代理
        DataSourceProxy dataSourceProxy = new DataSourceProxy(druidDataSource);
        logger.info("Seata AT模式数据源代理创建成功: {}", dataSourceProxy);
        return dataSourceProxy;
    }
    
    /**
     * 配置JdbcTemplate，使用Seata代理的数据源
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        logger.info("初始化JdbcTemplate，使用Seata代理数据源");
        return new JdbcTemplate(dataSource);
    }
} 