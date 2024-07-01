package com.guolihong.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.guolihong.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

/**
 * 后管回收站service层
 */
public interface RecycleBinService {


    Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
