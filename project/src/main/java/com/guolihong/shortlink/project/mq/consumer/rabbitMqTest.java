package com.guolihong.shortlink.project.mq.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Map;

//@Component
public class rabbitMqTest {
    @RabbitListener(queues = "string.queue")
    public void listenMessage(Map msg){
        System.out.println("接收到消息是："+msg.get("info"));
    }
    @RabbitListener(queues = "short-link.stats")
    public void listenMessageShortlink(Map msg){
        System.out.println("接收到消息是："+msg);
    }
}
