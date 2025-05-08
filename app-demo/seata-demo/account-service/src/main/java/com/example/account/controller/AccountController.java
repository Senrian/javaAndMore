package com.example.account.controller;

import com.example.account.service.AccountService;
import com.example.common.dto.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 账户控制器
 */
@RestController
@RequestMapping("/account")
public class AccountController {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    
    private final AccountService accountService;
    
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    /**
     * 扣减余额
     * @param userId 用户ID
     * @param money 金额
     * @return 通用响应
     */
    @PostMapping("/decrease")
    public CommonResult<String> decrease(@RequestParam("userId") Long userId, @RequestParam("money") BigDecimal money) {
        logger.info("收到扣减余额请求, 用户ID: {}, 金额: {}", userId, money);
        accountService.decrease(userId, money);
        return CommonResult.success("扣减余额成功");
    }
} 