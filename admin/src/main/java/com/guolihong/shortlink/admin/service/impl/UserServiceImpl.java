package com.guolihong.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guolihong.shortlink.admin.common.biz.user.UserContext;
import com.guolihong.shortlink.admin.common.convention.exception.ClientException;
import com.guolihong.shortlink.admin.common.convention.exception.ServiceException;
import com.guolihong.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.guolihong.shortlink.admin.dao.entity.UserDO;
import com.guolihong.shortlink.admin.dao.mapper.UserMapper;
import com.guolihong.shortlink.admin.dto.req.UserLoginReqDTO;
import com.guolihong.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.guolihong.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.guolihong.shortlink.admin.dto.resp.UserActualRespDTO;
import com.guolihong.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.guolihong.shortlink.admin.dto.resp.UserRespDTO;
import com.guolihong.shortlink.admin.service.GroupService;
import com.guolihong.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.guolihong.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.guolihong.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

/**
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    //用户名布隆过滤器
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    //redisson客户端
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;
    /**
     * 根据用户名查询用户是否存在
     * @param username 用户名
     * @return 返回参数
     */
    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> lambdaQueryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(lambdaQueryWrapper);
        if (userDO==null){
            throw new ServiceException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO,result);
        return result;
    }

    /**
     * 根据用户名查询用户未脱敏信息
     * @param username
     * @return
     */
    @Override
    public UserActualRespDTO getActualByUsername(String username) {
        LambdaQueryWrapper<UserDO> lambdaQueryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(lambdaQueryWrapper);
        if (userDO==null){
            throw new ServiceException(UserErrorCodeEnum.USER_NULL);
        }
        UserActualRespDTO result = BeanUtil.toBean(userDO, UserActualRespDTO.class);
        return result;
    }

    /**
     * 查询用户是否存在
     * @param username 用户名
     * @return true 不存在，false存在
     */
    @Override
    public Boolean hasUsername(String username) {
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    /**
     * 用户注册
     * @param requestParam
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterReqDTO requestParam) {
        if (!hasUsername(requestParam.getUsername())){
            //用户名已存在
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        if (!lock.tryLock()) {
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        try {
//            UserDO userDO=new UserDO();
//            BeanUtils.copyProperties(requestParam,userDO); spring提供的
            UserDO userDO = BeanUtil.toBean(requestParam, UserDO.class);
            boolean save = save(userDO);
            if (!save){
                throw new ServiceException(UserErrorCodeEnum.USER_SAVE_ERROR);
            }
            //将用户名保存到布隆过滤器中
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
            //为用户创建默认分组
            groupService.saveGroup(userDO.getUsername(),"默认分组");
        }catch (DuplicateKeyException e){
            log.error("用户注册异常："+e);
            throw new ServiceException(UserErrorCodeEnum.USER_EXIST);
        }finally {
            lock.unlock();
        }
    }

    /**
     * 修改用户信息
     * @param requestParam
     */
    @Override
    public void updateUser(UserUpdateReqDTO requestParam) {
        //需要校验修改的是否是当前登录用户的信息，根据登录凭证
        if (!Objects.equals(requestParam.getUsername(), UserContext.getUsername())){
            throw new ClientException("当前登录用户修改异常");
        }
        UserDO userDO = BeanUtil.toBean(requestParam, UserDO.class);
        LambdaUpdateWrapper<UserDO> eq = Wrappers.lambdaUpdate(UserDO.class).eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam,UserDO.class),eq);
    }

    /**
     * 用户登录功能
     * @param requestParam
     * @return
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> eq = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(eq);
        if (userDO==null){
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + requestParam.getUsername());
        if (CollectionUtils.isNotEmpty(entries)){
            String token = entries.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY+requestParam.getUsername(),token,JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN_KEY+requestParam.getUsername(),30L,TimeUnit.MINUTES);
        return new UserLoginRespDTO(token);
    }

    /**
     * 检查用户是否登录
     * @param username
     * @param token
     * @return
     */
    @Override
    public Boolean checkLogin(String username, String token) {
       return stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token)!= null;
    }

    @Override
    public void logout(String username, String token) {
        if (!checkLogin(username,token)){
            throw new ClientException("用户token不存在或用户未登录");
        }
        stringRedisTemplate.delete(USER_LOGIN_KEY+username);
    }

}
