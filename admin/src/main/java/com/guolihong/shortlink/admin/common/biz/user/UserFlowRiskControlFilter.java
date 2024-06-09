package com.guolihong.shortlink.admin.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.guolihong.shortlink.admin.common.convention.exception.ClientException;
import com.guolihong.shortlink.admin.common.convention.result.Results;
import com.guolihong.shortlink.admin.config.UserFlowRiskControlConfiguration;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static com.guolihong.shortlink.admin.common.convention.errorcode.BaseErrorCode.FLOW_LIMIT_ERROR;


@RequiredArgsConstructor
@Slf4j
public class UserFlowRiskControlFilter implements Filter {
    private final StringRedisTemplate stringRedisTemplate;
    private final String USER_FLOW_RISK_CONTROL_SCRIPT="lua/user_flow_risk_control.lua";
    private final UserFlowRiskControlConfiguration userFlowRiskControlConfiguration;
    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_FLOW_RISK_CONTROL_SCRIPT)));
        String username = Optional.ofNullable(UserContext.getUsername()).orElse("other");
        Long result;
        try {
            result=stringRedisTemplate.execute(redisScript, Lists.newArrayList(username),userFlowRiskControlConfiguration.getTimeWindow());
        }catch (Throwable ex){
            log.error("执行用户请求流量限制lua脚本出错",ex);
            returnJson((HttpServletResponse)response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        if (result==0||result> userFlowRiskControlConfiguration.getMaxAccessCount()){
            returnJson((HttpServletResponse)response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
            return;
        }
        chain.doFilter(request,response);
    }

    private void returnJson(HttpServletResponse response, String toJSONString) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=utf-8");
        try(PrintWriter writer=response.getWriter()){
            writer.println(toJSONString);
        }
    }
}
