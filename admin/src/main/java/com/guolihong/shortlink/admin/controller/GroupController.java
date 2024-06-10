package com.guolihong.shortlink.admin.controller;

import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.common.convention.result.Results;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.guolihong.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.guolihong.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    /**
     * 新增短链接分组
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/group")
    public Result<Void> addGroup(@RequestBody ShortLinkGroupSaveReqDTO requestParam){
        groupService.addGroup(requestParam);
        return Results.success();
    }

    /**
     * 查看短链接分组列表
     * @return
     */
    @GetMapping("/api/short-link/admin/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> groupList(){
       return Results.success(groupService.groupList());
    }

    /**
     * 修改短链接分组（只能修改名称）
     * @param requestParam
     * @return
     */
    @PutMapping("")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO requestParam){
        groupService.updateGroup(requestParam);
        return Results.success();
    }
}
