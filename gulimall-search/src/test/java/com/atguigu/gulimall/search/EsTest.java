package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EsTest {

    @Autowired
    private RestHighLevelClient client;

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

    @Data
    class User {
        private String userName;
        private String gender;
        private Integer age;
    }

    @Data
    @ToString
    static class Account {
        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }


    /**
     * ?????????????????????Es
     *
     * @throws IOException
     */
    @Test
    public void indexData() throws IOException {
        // ????????????
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");

        User user = new User();
        user.setUserName("??????");
        user.setAge(20);
        user.setGender("???");
        String jsonString = JSON.toJSONString(user);

        //????????????????????????????????????????????????
        indexRequest.source(jsonString, XContentType.JSON);

        //?????????????????????????????????
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println(index);
    }

    @Test
    public void searchData() throws IOException {
        // 1 ??????????????????
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // ??????????????????
//        sourceBuilder.query();
//        sourceBuilder.from();
//        sourceBuilder.size();
//        sourceBuilder.aggregation();
        sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        // ???????????????????????????:????????????????????????
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);// ????????????
        // ?????????AggregationBuilder
        sourceBuilder.aggregation(ageAgg);
        // ???????????????????????????:????????????
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);

        System.out.println("???????????????\n" + sourceBuilder.toString());

        searchRequest.source(sourceBuilder);

        // 2 ????????????
        SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        // 3 ??????????????????
        System.out.println("???????????????\n" + response.toString());
        // 3.1 ???????????????????????????????????????java bean
        SearchHits hits = response.getHits();
        SearchHit[] searchHits = hits.getHits();
        System.out.println("???????????????????????????????????????java bean???");
        for (SearchHit hit : searchHits) {
            hit.getId();
            hit.getIndex();
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account);
        }
        // 3.2 ??????????????????????????????
        Aggregations aggregations = response.getAggregations();
        Terms ageAgg1 = aggregations.get("ageAgg");
        System.out.println("?????????????????????????????????");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("?????????" + keyAsString);
        }

    }


}
