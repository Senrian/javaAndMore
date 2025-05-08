package com.example.account.service.impl;

import com.example.account.mapper.AccountMapper;
import com.example.account.service.AccountService;
import com.example.common.entity.Account;
import com.example.common.exception.BusinessException;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 账户服务实现类
 */
@Service
public class AccountServiceImpl implements AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    
    private final AccountMapper accountMapper;
    
    public AccountServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }
    
    /**
     * 扣减余额
     * @param userId 用户ID
     * @param money 金额
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrease(Long userId, BigDecimal money) {
        logger.info("------->account-service开始扣减余额, XID: {}", RootContext.getXID());
        
        // 查询账户是否存在
        Account account = accountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException(String.format("账户不存在，用户ID: %s", userId));
        }
        
        // 检查余额是否充足
        if (account.getResidue().compareTo(money) < 0) {
            throw new BusinessException(String.format("账户余额不足，用户ID: %s, 当前余额: %s, 需要金额: %s", 
                    userId, account.getResidue(), money));
        }
        
        logger.info("------->扣减账户余额，用户ID: {}, 当前余额: {}, 扣减金额: {}, XID: {}", 
                userId, account.getResidue(), money, RootContext.getXID());
        
        // 模拟超时异常，全局事务回滚
        // 测试时可以取消注释下面的代码
        /*
        if (money.compareTo(new BigDecimal("100")) >= 0) {
            try {
                logger.info("------->模拟超时异常，休眠10秒");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        */
        
        // 扣减余额
        int result = accountMapper.decrease(userId, money);
        if (result <= 0) {
            throw new BusinessException(String.format("扣减余额失败，用户ID: %s, 当前余额: %s, 扣减金额: %s", 
                    userId, account.getResidue(), money));
        }
        
        logger.info("------->扣减余额成功，用户ID: {}, 扣减后余额: {}, XID: {}", 
                userId, account.getResidue().subtract(money), RootContext.getXID());
    }
} 