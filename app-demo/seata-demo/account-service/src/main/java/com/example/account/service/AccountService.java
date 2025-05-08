package com.example.account.service;

import java.math.BigDecimal;

/**
 * 账户服务接口
 */
public interface AccountService {
    
    /**
     * 扣减余额
     * @param userId 用户ID
     * @param money 金额
     */
    void decrease(Long userId, BigDecimal money);
} 