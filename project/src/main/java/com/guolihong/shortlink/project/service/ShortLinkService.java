package com.guolihong.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guolihong.shortlink.project.dao.entity.ShortLinkDO;
import com.guolihong.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkPageRespDTO;

public interface ShortLinkService extends IService<ShortLinkDO> {
    /**
     * 创建短链接
     * @param requestParam
     * @return
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 基于分布式锁创建短链接
     * @param requestParam
     * @return
     */
    ShortLinkCreateRespDTO createShortLinkByLock(ShortLinkCreateReqDTO requestParam);

    /**
     * 短链接分页查询
     * @param requestParam
     * @return
     */
    IPage<ShortLinkPageRespDTO> pageQuery(ShortLinkPageReqDTO requestParam);
}
