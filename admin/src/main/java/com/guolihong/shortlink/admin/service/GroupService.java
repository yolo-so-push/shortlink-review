package com.guolihong.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guolihong.shortlink.admin.dao.entity.GroupDO;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.guolihong.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.guolihong.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;

import java.util.List;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     * @param requestParam
     */
    void addGroup(ShortLinkGroupSaveReqDTO requestParam);

    /**
     * 查看短链接分组列表
     * @return
     */
    List<ShortLinkGroupRespDTO> groupList();

    /**
     * 新增短链接分组，为指定用户
     * @param username
     * @param name
     */
    public void saveGroup(String username, String name);

    /**
     * 修改短链接分组
     * @param requestParam
     */
    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);
}
