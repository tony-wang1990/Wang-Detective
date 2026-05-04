package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.oci.securityrule.AddEgressSecurityRuleParams;
import com.tony.kingdetective.bean.params.oci.securityrule.AddIngressSecurityRuleParams;
import com.tony.kingdetective.bean.params.oci.securityrule.GetSecurityRuleListPageParams;
import com.tony.kingdetective.bean.params.oci.securityrule.RemoveSecurityRuleParams;
import com.tony.kingdetective.bean.response.oci.securityrule.SecurityRuleListRsp;
import com.tony.kingdetective.service.ISecurityRuleService;
import com.tony.kingdetective.utils.CommonUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * <p>
 * SecurityRuleController
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/31 15:48
 */
@RestController
@RequestMapping(path = "/api/securityRule")
public class SecurityRuleController {

    @Resource
    private ISecurityRuleService securityRuleService;

    @RequestMapping("/page")
    public ResponseData<Page<SecurityRuleListRsp.SecurityRuleInfo>> page(@Validated @RequestBody GetSecurityRuleListPageParams params) {
        return ResponseData.successData(securityRuleService.page(params));
    }

    @RequestMapping("/addIngress")
    public ResponseData<Void> addIngress(@Validated @RequestBody AddIngressSecurityRuleParams params){
        securityRuleService.addIngress(params);
        return ResponseData.successData();
    }

    @RequestMapping("/addEgress")
    public ResponseData<Void> addEgress(@Validated @RequestBody AddEgressSecurityRuleParams params){
        securityRuleService.addEgress(params);
        return ResponseData.successData();
    }

    @RequestMapping("/remove")
    public ResponseData<Void> remove(@Validated @RequestBody RemoveSecurityRuleParams params){
        securityRuleService.remove(params);
        return ResponseData.successData();
    }
}
