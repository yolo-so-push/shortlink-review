package com.guolihong.shortlink.admin.controller;

import com.guolihong.shortlink.admin.common.convention.result.Result;
import com.guolihong.shortlink.admin.common.convention.result.Results;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.guolihong.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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


}
