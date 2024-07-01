package com.guolihong.shortlink.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.common.convention.result.Results;
import com.guolihong.shortlink.admin.remote.ProjectClient;
import com.guolihong.shortlink.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import com.guolihong.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.guolihong.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.guolihong.shortlink.admin.remote.dto.resp.ShortLinkBatchCreateRespDTO;
import com.guolihong.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "shortLinkControllerByAdmin")
@RequiredArgsConstructor
public class ShortLinkController {
    private final ProjectClient projectClient;

    /**
     * 短链接创建
     * @param requestParam
     * @return TODO sentinal进行限流
     */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return projectClient.createShortLink(requestParam);
    }

    /**
     * 通过分布式锁创建短链接
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/create/by-lock")
    public Result<ShortLinkCreateRespDTO> createShortLinkByLock(@RequestBody ShortLinkCreateReqDTO requestParam){
        return projectClient.createShortLink(requestParam);
    }

    /**
     * 批量创建短链接
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam){
        return projectClient.batchCreateShortLink(requestParam);
    }
    /**
     * 短链接分页查询
     * @param requestParam
     * @return
     */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<Page<ShortLinkPageRespDTO>> shortLinkPageQuery(ShortLinkPageReqDTO requestParam){
        return projectClient.pageShortLink(requestParam.getGid(),requestParam.getOrderTag(),requestParam.getCurrent(),requestParam.getSize());
    }
    /**
     * 修改短链接
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        projectClient.updateShortLink(requestParam);
        return Results.success();
    }


}
