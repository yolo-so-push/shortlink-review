package com.guolihong.shortlink.admin.controller;

import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.common.convention.result.Results;
import com.guolihong.shortlink.admin.dto.req.UserLoginReqDTO;
import com.guolihong.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.guolihong.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.guolihong.shortlink.admin.dto.resp.UserActualRespDTO;
import com.guolihong.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.guolihong.shortlink.admin.dto.resp.UserRespDTO;
import com.guolihong.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制层controller
 */
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 根据用户名查询用户信息（无敏感信息）
     * @param username
     * @return
     */
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username){
        return Results.success(userService.getUserByUsername(username));
    }

    /**
     * 根据用户名查询用户真实信息（无脱敏）
     * @param username
     * @return
     */
    @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username){
        return Results.success(userService.getActualByUsername(username));
    }
    /**
     * 查询用户名是否存在
     * @param username
     * @return
     */
    @GetMapping("/api/short-link/admin/v1/user/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username){
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 用户注册
     * @param requestParam 请求参数（用户注册信息）
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam){
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户信息
     * @param requestParam 请求参数
     * @return
     */
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> updateUser(@RequestBody UserUpdateReqDTO requestParam){
        userService.updateUser(requestParam);
        return Results.success();
    }

    /**
     * 用户登录功能
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam){
        return Results.success(userService.login(requestParam));
    }

    /**
     * 检查用户是否登录
     * @param username
     * @param token
     * @return
     */
    @GetMapping("")
    public Result<Boolean> checkLogin(@RequestParam("username") String username,@RequestParam("token") String token){
        return Results.success(userService.checkLogin(username,token));
    }

    /**
     * 用户退出登录
     */
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username,@RequestParam("token") String token){
        userService.logout(username,token);
        return Results.success();
    }
}
