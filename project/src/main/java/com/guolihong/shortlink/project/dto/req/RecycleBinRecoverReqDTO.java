package com.guolihong.shortlink.project.dto.req;

import lombok.Data;

/**
 * 回收站短链接恢复请求参数
 */
@Data
public class RecycleBinRecoverReqDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 全部短链接
     */
    private String fullShortUrl;
}
