package com.guolihong.shortlink.project;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class publishTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Test
    public void send(){
        Map<String,Object> map=new HashMap<>();
        Map<String,String> info=new HashMap<>();
        info.put("name","zhangsan");
        map.put("info",info);
        rabbitTemplate.convertAndSend("short-link.stats",map);
    }

}
