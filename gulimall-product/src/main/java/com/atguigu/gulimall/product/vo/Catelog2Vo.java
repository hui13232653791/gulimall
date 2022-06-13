package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catelog2Vo {
    /**
     * 2级分类Vo
     */

    private String catelog1Id; //1级父分类id

    private List<Catelog3Vo> catelog3List; //3级子分类

    private String id;
    private String name;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catelog3Vo {
        /**
         * 3级分类Vo
         */

        private String catelog2Id; //2级父分类id

        private String id;
        private String name;

    }


}
