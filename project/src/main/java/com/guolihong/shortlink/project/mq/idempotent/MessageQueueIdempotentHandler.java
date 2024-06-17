package com.guolihong.shortlink.project.mq.idempotent;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MessageQueueIdempotentHandler {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent:";

    /**
     * 判断当前消息是否被消费过，如果消费过则设置失败，否则设置成功
     * @param messageId
     * @return false 消费过，true 未消费
     */
    public boolean isMessageProcessed(String messageId){
        String key=IDEMPOTENT_KEY_PREFIX+messageId;
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key,"0",2, TimeUnit.MINUTES));
    }

    /**
     * 判断消息的消费流程是否执行完毕
     * @param messageId
     * @return
     */
    public boolean isAccomplish(String messageId){
        String key=IDEMPOTENT_KEY_PREFIX+messageId;
        return Objects.equals(stringRedisTemplate.opsForValue().get(key),"1");
    }

    /**
     * 设置消息的消费流程执行完毕标识
     * @param messageId
     */
    public void setAccomplish(String messageId){
        String key=IDEMPOTENT_KEY_PREFIX+messageId;
        stringRedisTemplate.opsForValue().set(key,"1",2,TimeUnit.MINUTES);
    }

    /**
     * 如果消息处理遇到异常情况，删除幂等标识（消费标识）
     * @param messageId
     */
    public void deleteMessageProcessed(String messageId){
        String key=IDEMPOTENT_KEY_PREFIX+messageId;
        stringRedisTemplate.delete(key);
    }
}
