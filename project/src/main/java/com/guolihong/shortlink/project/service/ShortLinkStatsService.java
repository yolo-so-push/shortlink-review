package com.guolihong.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.guolihong.shortlink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkGroupStatsRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

public interface ShortLinkStatsService {
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);

    ShortLinkGroupStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam);

    IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam);

    IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam);
}
