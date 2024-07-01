package com.guolihong.shortlink.aggregation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "com.guolihong.shortlink.admin",
        "com.guolihong.shortlink.project",
})
@MapperScan(value = {
        "com.guolihong.shortlink.project.dao.mapper",
        "com.guolihong.shortlink.admin.dao.mapper"
})
public class AggregationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AggregationApplication.class,args);
    }
}
