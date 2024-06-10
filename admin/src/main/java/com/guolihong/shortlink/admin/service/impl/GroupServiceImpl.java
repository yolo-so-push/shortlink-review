package com.guolihong.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guolihong.shortlink.admin.common.biz.user.UserContext;
import com.guolihong.shortlink.admin.common.constant.RedisCacheConstant;
import com.guolihong.shortlink.admin.common.convention.exception.ClientException;
import com.guolihong.shortlink.admin.dao.entity.GroupDO;
import com.guolihong.shortlink.admin.dao.mapper.GroupMapper;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.guolihong.shortlink.admin.service.GroupService;
import com.guolihong.shortlink.admin.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
    private final RedissonClient redissonClient;

    @Value("${short-link.group.max-num}")
    private Long maxGroup;

    /**
     * 新增短链接分组
     * @param requestParam
     */
    @Override
    public void addGroup(ShortLinkGroupSaveReqDTO requestParam) {
        saveGroup(UserContext.getUsername(),requestParam.getName());
    }
    //使用加锁的方式新增短链接，避免并发安全问题
    private void saveGroup(String username, String name) {
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_GROUP_CREATE_KEY + username);
        lock.tryLock();
        try {
            LambdaQueryWrapper<GroupDO> eq = Wrappers.lambdaQuery(GroupDO.class).eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            Long groupCount = baseMapper.selectCount(eq);
            if(groupCount>=maxGroup){
                throw new ClientException(String.format("已到最大分组数%d",maxGroup));
            }
            String gid= RandomGenerator.generateRandom();
            while (!hasGroup(username,gid)){
                gid=RandomGenerator.generateRandom();
            }
            GroupDO groupDO = GroupDO.builder()
                    .gid(gid)
                    .name(username)
                    .name(name)
                    .build();
            baseMapper.insert(groupDO);
        }finally {
            lock.unlock();
        }
    }
    //判断当前分组gid是否存在
    private boolean hasGroup(String username, String gid) {
        LambdaQueryWrapper<GroupDO> eq = Wrappers.lambdaQuery(GroupDO.class).eq(GroupDO::getUsername, username)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = baseMapper.selectOne(eq);
        return groupDO==null;
    }
}
