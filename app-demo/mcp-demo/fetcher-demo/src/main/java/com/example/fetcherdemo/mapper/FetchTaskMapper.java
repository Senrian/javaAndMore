package com.example.fetcherdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fetcherdemo.entity.FetchTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * FetchTask Mapper接口
 * 
 * @author example
 * @date 2023-06-01
 */
@Mapper
public interface FetchTaskMapper extends BaseMapper<FetchTask> {
} 