package com.example.storage.controller;

import com.example.common.dto.CommonResult;
import com.example.storage.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存控制器
 */
@RestController
@RequestMapping("/storage")
public class StorageController {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageController.class);
    
    private final StorageService storageService;
    
    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 数量
     * @return 通用响应
     */
    @PostMapping("/decrease")
    public CommonResult<Void> decrease(@RequestParam("productId") Long productId, @RequestParam("count") Integer count) {
        logger.info("收到扣减库存请求, 商品ID: {}, 数量: {}", productId, count);
        storageService.decrease(productId, count);
        return CommonResult.success("扣减库存成功").asVoid();
    }
} 