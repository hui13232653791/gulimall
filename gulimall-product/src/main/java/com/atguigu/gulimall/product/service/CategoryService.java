package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author wangyh
 * @email 1942590224@qq.com
 * @date 2022-04-17 11:45:28
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    //查出所有分类以及子分类，以树形结构组装起来
    List<CategoryEntity> listWithTree();

    //删除菜单分类，检查当前删除的菜单，是否被别的地方引用
    void removeMenuByIds(List<Long> asList);

    //找到catelogId完整所属分类路径：[父/子/孙]
    Long[] findCatelogPath(Long catelogId);

    //级联更新所有关联的数据
    void updateCascade(CategoryEntity category);

    //获取所有的1级分类
    List<CategoryEntity> getLevel1Catagories();

    //渲染三级分类菜单
    Map<String, List<Catelog2Vo>> getCatelogJson();

}

