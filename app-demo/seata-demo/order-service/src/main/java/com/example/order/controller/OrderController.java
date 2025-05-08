package com.example.order.controller;

import com.example.common.dto.CommonResult;
import com.example.common.dto.OrderDTO;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * 创建订单
     * @param orderDTO 订单DTO
     * @return 通用响应
     */
    @PostMapping("/create")
    public CommonResult<Long> create(@RequestBody OrderDTO orderDTO) {
        logger.info("收到创建订单请求: {}", orderDTO);
        Long orderId = orderService.create(orderDTO);
        return CommonResult.success(orderId);
    }
} 