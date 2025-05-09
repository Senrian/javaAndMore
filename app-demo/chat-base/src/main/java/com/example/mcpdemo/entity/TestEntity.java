package com.example.mcpdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("test_entity")
@Data
public class TestEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField
    private String name;
} 