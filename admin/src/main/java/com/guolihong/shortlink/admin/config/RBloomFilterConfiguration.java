package com.guolihong.shortlink.admin.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "rBloomFilterConfigurationByAdmin")
public class RBloomFilterConfiguration {
    /**
     * 用户注册布隆过滤器
     * @param redissonClient
     * @return
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient){
        RBloomFilter<String> userRBloomFilterConfiguration = redissonClient.getBloomFilter("userRBloomFilterConfiguration");
        //用于指定布隆过滤器中的大小，以及误判率
        userRBloomFilterConfiguration.tryInit(100000000L,0.001);
        return userRBloomFilterConfiguration;
    }
}
