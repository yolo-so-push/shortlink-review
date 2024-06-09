package com.guolihong.shortlink.admin.config;

import com.guolihong.shortlink.admin.common.biz.user.UserFlowRiskControlFilter;
import com.guolihong.shortlink.admin.common.biz.user.UserTransmitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 过滤器配置类
 */
@Configuration
public class UserConfiguration{
    /**
     * 用户信息传递过滤器配置
     * @return
     */
    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter(){
        FilterRegistrationBean<UserTransmitFilter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(new UserTransmitFilter());
        filterFilterRegistrationBean.setOrder(0);
        filterFilterRegistrationBean.addUrlPatterns("/*");
        return filterFilterRegistrationBean;
    }
    /**
     * 流量风控过滤器注册
     * @param stringRedisTemplate
     * @param userFlowRiskControlConfiguration
     * @return
     */
    @Bean
    //只用short-link.flow-limit.enable为true时该过滤器才会生效
    @ConditionalOnProperty(name = "short-link.flow-limit.enable",havingValue = "true")
    public FilterRegistrationBean<UserFlowRiskControlFilter> globalUserFlowRiskControlFilter(StringRedisTemplate stringRedisTemplate
    ,UserFlowRiskControlConfiguration userFlowRiskControlConfiguration){
        FilterRegistrationBean<UserFlowRiskControlFilter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(new UserFlowRiskControlFilter(stringRedisTemplate,userFlowRiskControlConfiguration));
        filterFilterRegistrationBean.setOrder(10);
        filterFilterRegistrationBean.addUrlPatterns("/*");
        return filterFilterRegistrationBean;
    }
}
