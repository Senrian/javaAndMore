package com.example.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 账户实体类
 */
@TableName("account")
public class Account implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 账户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 总额度
     */
    private BigDecimal total;
    
    /**
     * 已用额度
     */
    private BigDecimal used;
    
    /**
     * 剩余额度
     */
    private BigDecimal residue;
    
    public Account() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public BigDecimal getUsed() {
        return used;
    }
    
    public void setUsed(BigDecimal used) {
        this.used = used;
    }
    
    public BigDecimal getResidue() {
        return residue;
    }
    
    public void setResidue(BigDecimal residue) {
        this.residue = residue;
    }
} 