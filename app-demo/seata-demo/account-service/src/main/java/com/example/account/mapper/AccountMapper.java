package com.example.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 账户Mapper接口
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    
    /**
     * 扣减账户余额
     * @param userId 用户ID
     * @param money 金额
     * @return 影响行数
     */
    @Update("UPDATE account SET residue = residue - #{money}, used = used + #{money} " +
            "WHERE user_id = #{userId} AND residue >= #{money}")
    int decrease(@Param("userId") Long userId, @Param("money") BigDecimal money);
} 