package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.IdListParams;
import com.tony.kingdetective.bean.params.cf.*;
import com.tony.kingdetective.bean.response.cf.GetCfCfgSelRsp;
import com.tony.kingdetective.bean.response.cf.ListCfCfgPageRsp;
import com.tony.kingdetective.bean.response.cf.ListCfDnsRecordRsp;
import com.tony.kingdetective.service.ICfCfgService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * @ClassName CloudflareController
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-20 13:43
 **/
@RestController
@RequestMapping(path = "/api/cf")
public class CloudflareController {

    @Resource
    private ICfCfgService cfCfgService;

    @RequestMapping("/listCfg")
    public ResponseData<Page<ListCfCfgPageRsp>> listCfg(@Validated @RequestBody ListCfCfgParams params) {
        return ResponseData.successData(cfCfgService.listCfg(params));
    }

    @RequestMapping("/add")
    public ResponseData<Void> addCfCfg(@Validated @RequestBody AddCfCfgParams params) {
        cfCfgService.addCfCfg(params);
        return ResponseData.successData();
    }

    @RequestMapping("/removeBatch")
    public ResponseData<Void> removeCfCfg(@Validated @RequestBody IdListParams params) {
        cfCfgService.removeCfCfg(params);
        return ResponseData.successData();
    }

    @RequestMapping("/update")
    public ResponseData<Void> updateCfCfg(@Validated @RequestBody UpdateCfCfgParams params) {
        cfCfgService.updateCfCfg(params);
        return ResponseData.successData();
    }

    @RequestMapping("/addCfDnsRecord")
    public ResponseData<Void> addCfDnsRecord(@Validated @RequestBody OciAddCfDnsRecordsParams params) {
        cfCfgService.addCfDnsRecord(params);
        return ResponseData.successData();
    }

    @RequestMapping("/removeCfDnsRecord")
    public ResponseData<Void> removeCfDnsRecord(@Validated @RequestBody OciRemoveCfDnsRecordsParams params) {
        cfCfgService.removeCfDnsRecord(params);
        return ResponseData.successData();
    }

    @RequestMapping("/updateCfDnsRecord")
    public ResponseData<Void> updateCfDnsRecord(@Validated @RequestBody OciUpdateCfDnsRecordsParams params) {
        cfCfgService.updateCfDnsRecord(params);
        return ResponseData.successData();
    }

    @RequestMapping("/listCfDnsRecord")
    public ResponseData<Page<ListCfDnsRecordRsp>> listCfDnsRecord(@Validated @RequestBody ListCfDnsRecordsParams params) {
        return ResponseData.successData(cfCfgService.listCfDnsRecord(params));
    }

    @RequestMapping("/getCfCfgSel")
    public ResponseData<GetCfCfgSelRsp> getCfCfgSel() {
        return ResponseData.successData(cfCfgService.getCfCfgSel());
    }
}
