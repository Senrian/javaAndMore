package com.example.order.service.impl;

import com.example.common.dto.OrderDTO;
import com.example.common.entity.Order;
import com.example.common.exception.BusinessException;
import com.example.common.feign.AccountService;
import com.example.common.feign.StorageService;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    
    private final OrderMapper orderMapper;
    private final StorageService storageService;
    private final AccountService accountService;
    
    public OrderServiceImpl(OrderMapper orderMapper, StorageService storageService, AccountService accountService) {
        this.orderMapper = orderMapper;
        this.storageService = storageService;
        this.accountService = accountService;
    }
    
    /**
     * 创建订单
     * @param orderDTO 订单DTO
     * @return 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Long create(OrderDTO orderDTO) {
        String xid = RootContext.getXID();
        logger.info("------->开始创建订单，当前XID: {}", xid);
        
        // 检查XID是否存在，确认事务上下文是否正确传播
        if (xid == null || xid.isEmpty()) {
            logger.error("------->警告: 当前XID为空，可能没有接收到全局事务上下文");
        } else {
            logger.info("------->成功接收到全局事务上下文，XID: {}", xid);
        }
        
        // 1. 创建订单
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setProductId(orderDTO.getProductId());
        order.setCount(orderDTO.getCount());
        order.setMoney(orderDTO.getMoney());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        
        orderMapper.insert(order);
        Long orderId = order.getId();
        logger.info("------->订单创建成功，订单ID: {}", orderId);
        
        // 2. 扣减库存
        logger.info("------->订单服务开始调用库存服务，进行库存扣减");
        storageService.decrease(orderDTO.getProductId(), orderDTO.getCount());
        logger.info("------->订单服务调用库存服务扣减库存结束");
        
        // 3. 扣减账户余额
        logger.info("------->订单服务开始调用账户服务，进行余额扣减");
        accountService.decrease(orderDTO.getUserId(), orderDTO.getMoney());
        logger.info("------->订单服务调用账户服务扣减余额结束");
        
        // 4. 修改订单状态
        logger.info("------->修改订单状态");
        order.setStatus(1);
        orderMapper.updateById(order);
        logger.info("------->订单状态修改完成");
        
        logger.info("------->下单结束，当前XID: {}", RootContext.getXID());
        return orderId;
    }
} 