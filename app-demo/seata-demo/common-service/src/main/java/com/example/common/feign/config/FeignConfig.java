package com.example.common.feign.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Feign配置类，用于确保全局事务ID能够在Feign调用中传播
 */
@Configuration
public class FeignConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FeignConfig.class);
    
    /**
     * 创建Feign请求拦截器，传递XID
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new SeataFeignRequestInterceptor();
    }
    
    /**
     * Seata的Feign请求拦截器，用于传递XID
     */
    static class SeataFeignRequestInterceptor implements RequestInterceptor {
        @Override
        public void apply(RequestTemplate template) {
            String xid = RootContext.getXID();
            
            if (StringUtils.hasLength(xid)) {
                logger.info("【Feign拦截器】传递XID: {} 到远程调用请求头", xid);
                template.header(RootContext.KEY_XID, xid);
            } else {
                logger.info("【Feign拦截器】当前线程没有XID，不传递事务上下文");
            }
        }
    }
} 