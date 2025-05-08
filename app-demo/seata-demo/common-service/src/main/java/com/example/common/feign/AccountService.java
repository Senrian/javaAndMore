package com.example.common.feign;

import com.example.common.dto.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 账户服务Feign客户端
 */
@FeignClient(value = "account-service")
public interface AccountService {
    
    /**
     * 扣减账户余额
     * @param userId 用户ID
     * @param money 金额
     * @return 通用响应对象
     */
    @PostMapping("/account/decrease")
    CommonResult<Void> decrease(@RequestParam("userId") Long userId, @RequestParam("money") BigDecimal money);
} 