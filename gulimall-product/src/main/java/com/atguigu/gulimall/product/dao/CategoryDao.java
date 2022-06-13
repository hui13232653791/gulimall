package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author wangyh
 * @email 1942590224@qq.com
 * @date 2022-04-17 11:45:28
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
