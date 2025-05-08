package com.example.common.feign;

import com.example.common.dto.CommonResult;
import com.example.common.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 订单服务Feign客户端
 */
@FeignClient(value = "order-service")
public interface OrderService {
    
    /**
     * 创建订单
     * @param orderDTO 订单信息
     * @return 通用响应对象
     */
    @PostMapping("/order/create")
    CommonResult<Long> create(@RequestBody OrderDTO orderDTO);
} 