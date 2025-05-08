package com.example.order.service;

import com.example.common.dto.OrderDTO;
import com.example.common.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     * @param orderDTO 订单DTO
     * @return 订单ID
     */
    Long create(OrderDTO orderDTO);
} 