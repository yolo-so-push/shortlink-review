package com.guolihong.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guolihong.shortlink.admin.common.biz.user.UserContext;
import com.guolihong.shortlink.admin.common.constant.RedisCacheConstant;
import com.guolihong.shortlink.admin.common.convention.exception.ClientException;
import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.dao.entity.GroupDO;
import com.guolihong.shortlink.admin.dao.mapper.GroupMapper;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.guolihong.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.guolihong.shortlink.admin.remote.ProjectClient;
import com.guolihong.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.guolihong.shortlink.admin.service.GroupService;
import com.guolihong.shortlink.admin.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
    private final RedissonClient redissonClient;
    private final ProjectClient projectClient;
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

    /**
     * 查看短链接分组列表
     * @return
     */
    @Override
    public List<ShortLinkGroupRespDTO> groupList() {
        LambdaQueryWrapper<GroupDO> eq = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0)
                .orderByDesc(GroupDO::getSortOrder,GroupDO::getUpdateTime);
        List<GroupDO> groupDOS = baseMapper.selectList(eq);
        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOS = BeanUtil.copyToList(groupDOS, ShortLinkGroupRespDTO.class);
        //这里需要远程调用project服务，查询当前用户的分组下短链接的个数
        List<String> gidList = groupDOS.stream().map(e -> e.getGid()).collect(Collectors.toList());
        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = projectClient.listGroupShortLinkCount(gidList);
        List<ShortLinkGroupCountQueryRespDTO> countList = listResult.getData();
        shortLinkGroupRespDTOS.forEach(e->{
            Optional<ShortLinkGroupCountQueryRespDTO> first=countList.stream().filter(item-> Objects.equals(item.getGid(),e.getGid()))
                    .findFirst();
            first.ifPresent(item->e.setShortLinkCount(first.get().getShortLinkCount()));
        });
        return shortLinkGroupRespDTOS;
    }

    /**
     * 修改短链接分组
     * @param requestParam
     */
    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> eq = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO=new GroupDO();
        groupDO.setName(requestParam.getName());
        baseMapper.update(groupDO,eq);
    }

    /**
     * 删除分组
     * @param gid
     */
    @Override
    public void delete(String gid) {
        LambdaUpdateWrapper<GroupDO> eq = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO=new GroupDO();
        //这里需要将当前分组下的短链接删除
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO,eq);
    }

    /**
     * 修改分组排血
     * @param requestParam
     */
    @Override
    public void sortedGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(g->{
            GroupDO groupDO=GroupDO.builder().sortOrder(g.getSortOrder()).build();
            LambdaUpdateWrapper<GroupDO> eq = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getGid, g.getGid())
                    .eq(GroupDO::getDelFlag, 0)
                    .eq(GroupDO::getUsername, UserContext.getUsername());
            baseMapper.update(groupDO,eq);
        });
    }

    //使用加锁的方式新增短链接，避免并发安全问题
    public void saveGroup(String username, String name) {
        RLock lock = redissonClient.getLock(String.format(RedisCacheConstant.LOCK_GROUP_CREATE_KEY,username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> eq = Wrappers.lambdaQuery(GroupDO.class).eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            Long groupCount = baseMapper.selectCount(eq);
            if(groupCount==maxGroup){
                throw new ClientException(String.format("已到最大分组数%d",maxGroup));
            }
            String gid;
            do {
                gid = RandomGenerator.generateRandom();
            } while (!hasGid(username, gid));
            GroupDO groupDO = GroupDO.builder()
                    .gid(gid)
                    .sortOrder(0)
                    .username(username)
                    .name(name)
                    .build();
            baseMapper.insert(groupDO);
        }finally {
            lock.unlock();
        }
    }
    //判断当前分组gid是否存在
    private boolean hasGid(String username, String gid) {
        LambdaQueryWrapper<GroupDO> eq = Wrappers.lambdaQuery(GroupDO.class)
                //
                .eq(GroupDO::getUsername, Optional.ofNullable(username).orElse(UserContext.getUsername()))
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = baseMapper.selectOne(eq);
        return groupDO==null;
    }
}
