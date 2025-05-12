package com.example.fetcherdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fetcherdemo.entity.WebPage;
import org.apache.ibatis.annotations.Mapper;

/**
 * WebPage Mapper接口
 * 
 * @author example
 * @date 2023-06-01
 */
@Mapper
public interface WebPageMapper extends BaseMapper<WebPage> {
} 