package com.guolihong.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.guolihong.shortlink.project.common.convention.result.Result;
import com.guolihong.shortlink.project.common.convention.result.Results;
import com.guolihong.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.guolihong.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    /**
     * 短链接创建
     * @param requestParam
     * @return TODO sentinal进行限流
     */
    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return Results.success(shortLinkService.createShortLink(requestParam));
    }

    /**
     * 通过分布式锁创建短链接
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/v1/create/by-lock")
    public Result<ShortLinkCreateRespDTO> createShortLinkByLock(@RequestBody ShortLinkCreateReqDTO requestParam){
        return Results.success(shortLinkService.createShortLinkByLock(requestParam));
    }

    /**
     * 短链接分页查询
     * @param requestParam
     * @return
     */
    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> shortLinkPageQuery(ShortLinkPageReqDTO requestParam){
        return Results.success(shortLinkService.pageQuery(requestParam));
    }
}
