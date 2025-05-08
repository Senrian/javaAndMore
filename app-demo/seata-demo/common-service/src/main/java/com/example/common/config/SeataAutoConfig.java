package com.example.common.config;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * Seata配置类，用于确保全局事务ID能够在服务之间正确传播
 */
@Configuration
public class SeataAutoConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(SeataAutoConfig.class);
    
    /**
     * 添加XID传播拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SeataHandlerInterceptor()).addPathPatterns("/**");
    }
    
    /**
     * Seata拦截器，用于处理XID的传播
     */
    static class SeataHandlerInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String xid = RootContext.getXID();
            String rpcXid = request.getHeader(RootContext.KEY_XID);
            
            // 输出所有请求头，帮助调试
            logger.debug("【Seata拦截器】收到请求，请求头列表：");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                logger.debug("【Seata拦截器】请求头: {} = {}", headerName, request.getHeader(headerName));
            }
            
            if (xid == null && rpcXid != null) {
                logger.info("【Seata拦截器】从请求中获取到XID: {}, 绑定到当前线程", rpcXid);
                RootContext.bind(rpcXid);
            } else if (xid != null) {
                logger.info("【Seata拦截器】当前线程已经绑定XID: {}", xid);
            } else {
                logger.info("【Seata拦截器】没有找到XID，可能不在全局事务中");
            }
            
            return true;
        }
        
        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                   Object handler, Exception ex) {
            String rpcXid = request.getHeader(RootContext.KEY_XID);
            if (rpcXid != null) {
                String unbindXid = RootContext.unbind();
                if (unbindXid != null) {
                    logger.info("【Seata拦截器】请求结束，解绑XID: {}", unbindXid);
                    if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                        logger.warn("【Seata拦截器】解绑的XID: {} 与请求中的XID: {} 不一致", unbindXid, rpcXid);
                    }
                }
            }
        }
    }
} 