package com.guolihong.shortlink.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@MapperScan("com.guolihong.shortlink.project.dao.mapper")
@SpringBootApplication
@EnableDiscoveryClient
public class ShortLinkProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShortLinkProjectApplication.class,args);
    }
}
