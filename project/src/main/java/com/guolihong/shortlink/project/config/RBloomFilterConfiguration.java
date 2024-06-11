package com.guolihong.shortlink.project.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("userRBloomFilterConfiguration")
public class RBloomFilterConfiguration {
    /**
     * 短链接生成避免查询数据库布隆过滤器
     * @param redissonClient
     * @return
     */
    @Bean
    public RBloomFilter<String> shortLinkCachePenetrationBloomFilter(RedissonClient redissonClient){
        RBloomFilter<String> userRBloomFilterConfiguration = redissonClient.getBloomFilter("shortLinkRBloomFilterConfiguration");
        //用于指定布隆过滤器中的大小，以及误判率
        userRBloomFilterConfiguration.tryInit(100000000L,0.001);
        return userRBloomFilterConfiguration;
    }
}
