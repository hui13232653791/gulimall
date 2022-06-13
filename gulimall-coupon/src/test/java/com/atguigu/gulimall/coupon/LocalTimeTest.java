package com.atguigu.gulimall.coupon;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class LocalTimeTest {

    @Test
    public void test() {
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalDateTime now1 = LocalDateTime.now();
        LocalTime now2 = LocalTime.now();

        LocalTime max = LocalTime.MAX;
        LocalTime min = LocalTime.MIN;

        LocalDateTime start = LocalDateTime.of(now, min);
        LocalDateTime end = LocalDateTime.of(plus, max);

        System.out.println(now);
        System.out.println(now1);
        System.out.println(now2);
        System.out.println(plus);

        System.out.println(start);
        System.out.println(end);

    }


}
