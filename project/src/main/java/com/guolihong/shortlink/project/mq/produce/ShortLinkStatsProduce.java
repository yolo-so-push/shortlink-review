package com.guolihong.shortlink.project.mq.produce;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.guolihong.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

@Component
@RequiredArgsConstructor
public class ShortLinkStatsProduce {
    private final StringRedisTemplate stringRedisTemplate;

    public void send(Map<String,String> message){
        stringRedisTemplate.opsForStream().add(SHORT_LINK_STATS_STREAM_TOPIC_KEY,message);
    }
}
