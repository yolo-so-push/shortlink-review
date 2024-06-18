package com.guolihong.shortlink.project.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.guolihong.shortlink.project.common.convention.result.Result;
import com.guolihong.shortlink.project.common.convention.result.Results;
import com.guolihong.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.guolihong.shortlink.project.handler.CustomBlockHandler;
import com.guolihong.shortlink.project.service.ShortLinkService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    /**
     * 短链接创建
     * @param requestParam
     * @return sentinal进行限流
     */
    @PostMapping("/api/short-link/v1/create")
    @SentinelResource(value = "create_short_link",blockHandler = "createShortLinkBlockHandlerMethod",blockHandlerClass = CustomBlockHandler.class )
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

    @PostMapping("/api/short-link/v1/create/batch")
    public Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam){
        return Results.success(shortLinkService.batchCreateShortLink(requestParam));
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
    /**
     * 修改短链接
     */
    @PostMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
     * 查询当前用户所有分组下的的短链接个数
     * @param requestParam gid（分组标识）
     * @return 短链接分组中的短链接个数
     */
    @GetMapping("/api/short-link/v1/count")
    public Result<List<ShortLinkGroupCountQueryRespDTO>> queryGroupLinkCount(@RequestParam("requestParam") List<String> requestParam){
        return Results.success(shortLinkService.queryGroupLinkCount(requestParam));
    }

    /**
     * 短链接跳转原始链接
     */
    @GetMapping("/{short-uri}")
    public void restoreUrl(@PathVariable("short-uri") String shortUri, ServletRequest request, ServletResponse response) {
        shortLinkService.restoreUrl(shortUri, request, response);
    }
}
