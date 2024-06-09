package com.guolihong.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * 用户信息过滤器
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String username = servletRequest.getHeader("username");
        if (StrUtil.isNotBlank(username)){
            //用户已经登录
            String userId = servletRequest.getHeader("userId");
            String realName = servletRequest.getHeader("realName");
            UserInfoDTO userInfoDTO = new UserInfoDTO(userId, username, realName);
            UserContext.setUser(userInfoDTO);
        }
        try {
            chain.doFilter(servletRequest,response);
        }finally {
            UserContext.removeUser();
        }
    }
}
