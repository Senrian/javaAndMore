package com.example.business.service.impl;

import com.example.business.service.BusinessService;
import com.example.common.dto.OrderDTO;
import com.example.common.exception.BusinessException;
import com.example.common.feign.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 业务服务实现类
 */
@Service
public class BusinessServiceImpl implements BusinessService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessServiceImpl.class);
    
    private final OrderService orderService;
    
    public BusinessServiceImpl(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * 下单
     * @param orderDTO 订单DTO
     * @return 订单ID
     */
    @Override
    @GlobalTransactional(name = "demo-place-order", rollbackFor = Exception.class)
    public Long placeOrder(OrderDTO orderDTO) {
        logger.info("------->开始下单");
        
        // 远程调用订单服务创建订单
        Long orderId = orderService.create(orderDTO).getData();
        
        logger.info("------->下单成功，订单ID: {}", orderId);
        return orderId;
    }
    
    /**
     * 异常下单，用于测试分布式事务回滚
     * @param orderDTO 订单DTO
     * @return 订单ID
     */
    @Override
    @GlobalTransactional(name = "demo-place-order-with-exception", rollbackFor = Exception.class)
    public Long placeOrderWithException(OrderDTO orderDTO) {
        logger.info("------->开始下单（异常测试）");
        
        // 远程调用订单服务创建订单
        Long orderId = orderService.create(orderDTO).getData();
        
        logger.info("------->下单成功，订单ID: {}，但接下来会抛出异常回滚事务", orderId);
        
        // 抛出异常，触发分布式事务回滚
        throw new BusinessException("测试分布式事务回滚");
    }
} 