package com.example.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存Mapper接口
 */
@Mapper
public interface StorageMapper extends BaseMapper<Product> {
    
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 数量
     * @return 影响行数
     */
    @Update("UPDATE product SET stock = stock - #{count} WHERE id = #{productId} AND stock >= #{count}")
    int decrease(@Param("productId") Long productId, @Param("count") Integer count);
} 