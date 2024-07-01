package com.guolihong.shortlink.project.mq.produce;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.guolihong.shortlink.project.common.constant.MessageConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortLinkStatsProduce {

//    private final StringRedisTemplate stringRedisTemplate;
//
//    public void send(Map<String,String> message){
//        stringRedisTemplate.opsForStream().add(SHORT_LINK_STATS_STREAM_TOPIC_KEY,message);
//    }
    @Autowired
    private  RabbitTemplate rabbitTemplate;
    public void send(Map<String,String> producerMap) {
        String keys = UUID.randomUUID().toString();
        producerMap.put("keys", keys);
        Message<Map<String, String>> build = MessageBuilder
                .withPayload(producerMap)
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .build();
        try {
           rabbitTemplate.convertAndSend("short-link.stats",producerMap);
        } catch (Throwable ex) {
            log.error("[消息访问统计监控] 消息发送失败，消息体：{}", JSON.toJSONString(producerMap), ex);
            // 自定义行为
        }
    }
//    @Bean
//    public MessageConverter jsonMessageConvert(){
//        return new Jackson2JsonMessageConverter();
//    }
}
