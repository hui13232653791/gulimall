package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestSpuItemAttrGroupVo {

    @Resource
    AttrGroupDao attrGroupDao;
    @Resource
    SkuSaleAttrValueDao saleAttrValueDao;


    @Test
    public void  test01(){
        List<SpuItemAttrGroupVo> spuItemAttrGroupVos = attrGroupDao.getAttrGroupWithAttrsBySpuId(3L, 225L);
        System.out.println(spuItemAttrGroupVos);
    }

    @Test
    public void  test02(){
        List<SkuItemSaleAttrVo> saleAttrBySpuId = saleAttrValueDao.getSaleAttrBySpuId(3L);
        System.out.println(saleAttrBySpuId);
    }


}
