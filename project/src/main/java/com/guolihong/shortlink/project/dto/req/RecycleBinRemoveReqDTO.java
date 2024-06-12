package com.guolihong.shortlink.project.dto.req;

import lombok.Data;

/**
 * 回收站短链接删除请求参数
 */
@Data
public class RecycleBinRemoveReqDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 全部短链接
     */
    private String fullShortUrl;
}
