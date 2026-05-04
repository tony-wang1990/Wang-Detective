package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.entity.IpData;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tony.kingdetective.bean.params.ipdata.AddIpDataParams;
import com.tony.kingdetective.bean.params.ipdata.PageIpDataParams;
import com.tony.kingdetective.bean.params.ipdata.RemoveIpDataParams;
import com.tony.kingdetective.bean.params.ipdata.UpdateIpDataParams;
import com.tony.kingdetective.bean.response.ipdata.IpDataPageRsp;

/**
* @author Tony Wang
* @description 针对表【ip_data】的数据库操作Service
* @createDate 2025-08-04 17:28:41
*/
public interface IIpDataService extends IService<IpData> {

    void add(AddIpDataParams params);

    void loadOciIpData();

    void updateIpData(UpdateIpDataParams params);

    void removeIpData(RemoveIpDataParams params);

    Page<IpDataPageRsp> pageIpData(PageIpDataParams params);
}
