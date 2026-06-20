package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.IdListParams;
import com.tony.kingdetective.bean.params.cf.*;
import com.tony.kingdetective.bean.response.cf.GetCfCfgSelRsp;
import com.tony.kingdetective.bean.response.cf.ListCfCfgPageRsp;
import com.tony.kingdetective.bean.response.cf.ListCfDnsRecordRsp;
import com.tony.kingdetective.service.ICfCfgService;
import com.tony.kingdetective.service.OperationAuditSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
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
    @Resource
    private OperationAuditSupport audit;

    @PostMapping("/listCfg")
    public ResponseData<Page<ListCfCfgPageRsp>> listCfg(@Validated @RequestBody ListCfCfgParams params) {
        return ResponseData.successData(cfCfgService.listCfg(params));
    }

    @PostMapping("/add")
    public ResponseData<Void> addCfCfg(@Validated @RequestBody AddCfCfgParams params) {
        audit.run("CLOUDFLARE_CONFIG_ADD", "cloudflare", "configuration created", () -> cfCfgService.addCfCfg(params));
        return ResponseData.successData();
    }

    @PostMapping("/removeBatch")
    public ResponseData<Void> removeCfCfg(@Validated @RequestBody IdListParams params) {
        audit.run("CLOUDFLARE_CONFIG_DELETE", String.join(",", params.getIdList()), "configuration deleted", () -> cfCfgService.removeCfCfg(params));
        return ResponseData.successData();
    }

    @PostMapping("/update")
    public ResponseData<Void> updateCfCfg(@Validated @RequestBody UpdateCfCfgParams params) {
        audit.run("CLOUDFLARE_CONFIG_UPDATE", "cloudflare", "configuration updated", () -> cfCfgService.updateCfCfg(params));
        return ResponseData.successData();
    }

    @PostMapping("/addCfDnsRecord")
    public ResponseData<Void> addCfDnsRecord(@Validated @RequestBody OciAddCfDnsRecordsParams params) {
        audit.run("CLOUDFLARE_DNS_ADD", "dns-record", "DNS record created", () -> cfCfgService.addCfDnsRecord(params));
        return ResponseData.successData();
    }

    @PostMapping("/removeCfDnsRecord")
    public ResponseData<Void> removeCfDnsRecord(@Validated @RequestBody OciRemoveCfDnsRecordsParams params) {
        audit.run("CLOUDFLARE_DNS_DELETE", "dns-record", "DNS record deleted", () -> cfCfgService.removeCfDnsRecord(params));
        return ResponseData.successData();
    }

    @PostMapping("/updateCfDnsRecord")
    public ResponseData<Void> updateCfDnsRecord(@Validated @RequestBody OciUpdateCfDnsRecordsParams params) {
        audit.run("CLOUDFLARE_DNS_UPDATE", "dns-record", "DNS record updated", () -> cfCfgService.updateCfDnsRecord(params));
        return ResponseData.successData();
    }

    @PostMapping("/listCfDnsRecord")
    public ResponseData<Page<ListCfDnsRecordRsp>> listCfDnsRecord(@Validated @RequestBody ListCfDnsRecordsParams params) {
        return ResponseData.successData(cfCfgService.listCfDnsRecord(params));
    }

    @PostMapping("/getCfCfgSel")
    public ResponseData<GetCfCfgSelRsp> getCfCfgSel() {
        return ResponseData.successData(cfCfgService.getCfCfgSel());
    }
}
