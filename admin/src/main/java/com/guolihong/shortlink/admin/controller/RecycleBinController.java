package com.guolihong.shortlink.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.common.convention.result.Results;
import com.guolihong.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.guolihong.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import com.guolihong.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.guolihong.shortlink.admin.remote.ProjectClient;
import com.guolihong.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.guolihong.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回收站管理控制层
 */
@RestController
@RequiredArgsConstructor
public class RecycleBinController {
    private final ProjectClient projectClient;
    /**
     * 保存回收站
     */
    @PostMapping("/api/short-link/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        projectClient.saveRecycleBin(requestParam);
        return Results.success();
    }
    /**
     * 分页查询回收站短链接
     */
    @GetMapping("/api/short-link/v1/recycle-bin/page")
    public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        return projectClient.pageRecycleBinShortLink(requestParam.getGidList(),requestParam.getCurrent(),requestParam.getSize());
    }

    /**
     * 恢复短链接
     */
    @PostMapping("/api/short-link/v1/recycle-bin/recover")
    public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        projectClient.recoverRecycleBin(requestParam);
        return Results.success();
    }
    /**
     * 移除短链接
     */
    @PostMapping("/api/short-link/v1/recycle-bin/remove")
    public Result<Void> removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam) {
        projectClient.removeRecycleBin(requestParam);
        return Results.success();
    }
}
