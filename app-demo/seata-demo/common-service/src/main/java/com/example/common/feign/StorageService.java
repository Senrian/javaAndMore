package com.example.common.feign;

import com.example.common.dto.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 库存服务Feign客户端
 */
@FeignClient(value = "storage-service")
public interface StorageService {
    
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 数量
     * @return 通用响应对象
     */
    @PostMapping("/storage/decrease")
    CommonResult<Void> decrease(@RequestParam("productId") Long productId, @RequestParam("count") Integer count);
} 