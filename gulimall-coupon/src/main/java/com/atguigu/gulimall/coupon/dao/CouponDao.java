package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author wangyh
 * @email 1942590224@qq.com
 * @date 2022-04-17 13:01:03
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
