package com.example.storage.service.impl;

import com.example.common.entity.Product;
import com.example.common.exception.BusinessException;
import com.example.storage.mapper.StorageMapper;
import com.example.storage.service.StorageService;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务实现类
 */
@Service
public class StorageServiceImpl implements StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageServiceImpl.class);
    
    private final StorageMapper storageMapper;
    
    public StorageServiceImpl(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }
    
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrease(Long productId, Integer count) {
        logger.info("------->storage-service开始扣减库存, XID: {}", RootContext.getXID());
        
        // 查询商品是否存在
        Product product = storageMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(String.format("商品不存在，商品ID: %s", productId));
        }
        
        // 检查库存是否充足
        if (product.getStock() < count) {
            throw new BusinessException(String.format("商品库存不足，商品ID: %s, 当前库存: %s, 需要库存: %s", 
                    productId, product.getStock(), count));
        }
        
        logger.info("------->扣减商品库存，商品ID: {}, 当前库存: {}, 扣减数量: {}, XID: {}", 
                productId, product.getStock(), count, RootContext.getXID());
        
        // 扣减库存
        int result = storageMapper.decrease(productId, count);
        if (result <= 0) {
            throw new BusinessException(String.format("扣减库存失败，商品ID: %s, 当前库存: %s, 扣减数量: %s", 
                    productId, product.getStock(), count));
        }
        
        logger.info("------->扣减库存成功，商品ID: {}, 扣减后库存: {}, XID: {}", 
                productId, product.getStock() - count, RootContext.getXID());
    }
} 