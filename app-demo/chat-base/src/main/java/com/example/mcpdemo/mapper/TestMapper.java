package com.example.mcpdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mcpdemo.entity.TestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestMapper extends BaseMapper<TestEntity> {
    
    /**
     * 根据名称查询
     */
    @Select("SELECT * FROM test_entity WHERE name = #{name} LIMIT 1")
    TestEntity findByName(@Param("name") String name);
} 