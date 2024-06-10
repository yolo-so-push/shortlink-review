package com.guolihong.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guolihong.shortlink.admin.dao.entity.GroupDO;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     * @param requestParam
     */
    void addGroup(ShortLinkGroupSaveReqDTO requestParam);
}
