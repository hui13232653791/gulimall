package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author wangyh
 * @email 1942590224@qq.com
 * @date 2022-04-17 13:34:40
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
