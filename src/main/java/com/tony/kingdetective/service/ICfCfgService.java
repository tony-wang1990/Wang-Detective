package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.entity.CfCfg;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tony.kingdetective.bean.params.IdListParams;
import com.tony.kingdetective.bean.params.cf.*;
import com.tony.kingdetective.bean.response.cf.GetCfCfgSelRsp;
import com.tony.kingdetective.bean.response.cf.ListCfCfgPageRsp;
import com.tony.kingdetective.bean.response.cf.ListCfDnsRecordRsp;

/**
* @author Tony Wang
* @description 针对表【cf_cfg】的数据库操作Service
* @createDate 2025-03-19 16:10:18
*/
public interface ICfCfgService extends IService<CfCfg> {

    Page<ListCfCfgPageRsp> listCfg(ListCfCfgParams params);

    void addCfCfg(AddCfCfgParams params);

    void removeCfCfg(IdListParams params);

    void updateCfCfg(UpdateCfCfgParams params);

    void addCfDnsRecord(OciAddCfDnsRecordsParams params);

    void removeCfDnsRecord(OciRemoveCfDnsRecordsParams params);

    void updateCfDnsRecord(OciUpdateCfDnsRecordsParams params);

    Page<ListCfDnsRecordRsp> listCfDnsRecord(ListCfDnsRecordsParams params);

    GetCfCfgSelRsp getCfCfgSel();
}
