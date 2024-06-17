package com.guolihong.shortlink.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.guolihong.shortlink.admin.dao.mapper")
@EnableDiscoveryClient
@EnableFeignClients(value = "com.guolihong.shortlink.admin.remote")
public class ShortLinkAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShortLinkAdminApplication.class,args);
    }
}
