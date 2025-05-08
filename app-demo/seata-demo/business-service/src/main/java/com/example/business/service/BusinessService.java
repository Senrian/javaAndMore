package com.example.business.service;

import com.example.common.dto.OrderDTO;

/**
 * 业务服务接口
 */
public interface BusinessService {
    
    /**
     * 下单
     * @param orderDTO 订单DTO
     * @return 订单ID
     */
    Long placeOrder(OrderDTO orderDTO);

    /**
     * 异常下单，用于测试分布式事务回滚
     * @param orderDTO 订单DTO
     * @return 订单ID
     */
    Long placeOrderWithException(OrderDTO orderDTO);
} 