package com.example.common.exception;

import com.example.common.dto.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     * @param e 业务异常
     * @return 通用响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException e) {
        logger.error("业务异常: {}", e.getMessage(), e);
        return CommonResult.failed(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理未知异常
     * @param e 未知异常
     * @return 通用响应对象
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception e) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return CommonResult.failed("系统异常，请联系管理员");
    }
} 