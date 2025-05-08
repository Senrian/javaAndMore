package com.example.storage.service;

/**
 * 库存服务接口
 */
public interface StorageService {
    
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 数量
     */
    void decrease(Long productId, Integer count);
} 