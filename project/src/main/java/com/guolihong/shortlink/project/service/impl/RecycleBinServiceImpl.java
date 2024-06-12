package com.guolihong.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guolihong.shortlink.project.dao.entity.ShortLinkDO;
import com.guolihong.shortlink.project.dao.mapper.ShortLinkMapper;
import com.guolihong.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.guolihong.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.guolihong.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper,ShortLinkDO> implements RecycleBinService {
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 短链接移入回收站
     * @param requestParam
     */
    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO linkDO = ShortLinkDO.builder()
                .enableStatus(1)
                .build();
        baseMapper.update(linkDO,queryWrapper);
        stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl()));
    }

    /**
     * 回收站短链接分页查询
     * @param requestParam
     * @return
     */
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        IPage<ShortLinkDO> shortLinkDOIPage = baseMapper.recyclePageLink(requestParam);
        return shortLinkDOIPage.convert(e->{
            ShortLinkPageRespDTO shortLinkPageRespDTO=BeanUtil.toBean(e, ShortLinkPageRespDTO.class);
            shortLinkPageRespDTO.setDomain("http://"+e.getDomain());
            return shortLinkPageRespDTO;
        });
    }
}
