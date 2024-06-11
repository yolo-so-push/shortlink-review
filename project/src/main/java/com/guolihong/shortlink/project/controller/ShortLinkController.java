package com.guolihong.shortlink.project.controller;

import com.guolihong.shortlink.project.common.convention.result.Result;
import com.guolihong.shortlink.project.common.convention.result.Results;
import com.guolihong.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
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
     * @return
     */
    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return Results.success(shortLinkService.createShortLink(requestParam));
    }
}
