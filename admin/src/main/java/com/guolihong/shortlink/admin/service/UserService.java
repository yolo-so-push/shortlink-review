package com.guolihong.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guolihong.shortlink.admin.dao.entity.UserDO;
import com.guolihong.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.guolihong.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 返回参数
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户是否存在
     * @param username 用户名
     * @return true 不存在，false存在
     */
    Boolean hasUsername(String username);

    /**
     * 用户注册
     * @param requestParam
     */
    void register(UserRegisterReqDTO requestParam);
}
