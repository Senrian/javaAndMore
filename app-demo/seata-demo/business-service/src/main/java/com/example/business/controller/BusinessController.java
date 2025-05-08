package com.example.business.controller;

import com.example.business.service.BusinessService;
import com.example.common.dto.CommonResult;
import com.example.common.dto.OrderDTO;
import com.example.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 业务控制器
 */
@RestController
@RequestMapping("/business")
@CrossOrigin(origins = "*")
public class BusinessController {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessController.class);
    
    private final BusinessService businessService;
    
    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }
    
    /**
     * 下单
     * @param orderDTO 订单DTO
     * @return 通用响应
     */
    @PostMapping("/placeOrder")
    public CommonResult<String> placeOrder(@RequestBody OrderDTO orderDTO) {
        logger.info("收到下单请求: {}", orderDTO);
        Long orderId = businessService.placeOrder(orderDTO);
        return CommonResult.success("下单成功");
    }
    
    /**
     * 异常下单，测试回滚
     * @param orderDTO 订单DTO
     * @return 通用响应
     */
    @PostMapping("/placeOrderWithException")
    public CommonResult placeOrderWithException(@RequestBody OrderDTO orderDTO) {
        logger.info("收到异常下单请求: {}", orderDTO);
        try {
            // 这个方法会抛出异常，触发分布式事务回滚
            Long orderId = businessService.placeOrderWithException(orderDTO);
            // 这行代码正常情况下不会执行到
            return CommonResult.success("下单成功");
        } catch (BusinessException e) {
            // 捕获业务异常，确保事务已回滚
            logger.error("下单异常并已回滚: {}", e.getMessage(), e);
            // 返回错误信息给前端
            return CommonResult.failed(e.getCode(), e.getMessage());
        } catch (Exception e) {
            // 捕获其他未知异常
            logger.error("系统异常: {}", e.getMessage(), e);
            return CommonResult.failed("系统异常，请联系管理员");
        }
    }
} 