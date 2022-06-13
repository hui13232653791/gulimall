package com.atguigu.gulimall.product.fallback;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {

    @Override
    public R getSkuSeckilInfo(Long skuId) {
        log.info("秒杀熔断方法调用。。。getSkuSeckilInfo");
        return R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(),BizCodeEnum.TO_MANY_REQUEST.getMsg());
    }
}
