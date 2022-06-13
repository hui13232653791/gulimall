package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     *      /product/skuinfo/info/{skuId}
     *      /api/product/skuinfo/info/{skuId}
     *      1、所有请求过网关
     *          @FeignClient("gulimall-gateway")
     *          /api/product/skuinfo/info/{skuId}
     *      2、直接让后台指定服务处理
     *          @FeignClient("gulimall-product")
     *           /product/skuinfo/info/{skuId}
     *
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

}
