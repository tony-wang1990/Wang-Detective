package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.oci.tenant.GetTenantInfoParams;
import com.tony.kingdetective.bean.params.oci.tenant.UpdatePwdExpirationPolicyParams;
import com.tony.kingdetective.bean.params.oci.tenant.UpdateUserBasicParams;
import com.tony.kingdetective.bean.params.oci.tenant.UpdateUserInfoParams;
import com.tony.kingdetective.bean.response.oci.tenant.TenantInfoRsp;
import com.tony.kingdetective.service.ITenantService;
import com.tony.kingdetective.service.OperationAuditSupport;
import com.tony.kingdetective.utils.CommonUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * @ClassName TenantController
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-12 15:51
 **/
@RestController
@RequestMapping(path = "/api/tenant")
public class TenantController {

    @Resource
    private ITenantService tenantService;
    @Resource
    private OperationAuditSupport audit;

    @PostMapping("/tenantInfo")
    public ResponseData<TenantInfoRsp> tenantInfo(@Validated @RequestBody GetTenantInfoParams params) {
        return ResponseData.successData(tenantService.tenantInfo(params));
    }

    @PostMapping("/deleteUser")
    public ResponseData<Void> deleteUser(@Validated @RequestBody UpdateUserBasicParams params) {
        audit.run("OCI_TENANT_USER_DELETE", params.getUserId(), "cfgId=" + params.getOciCfgId(), () -> tenantService.deleteUser(params));
        return ResponseData.successData("删除用户成功");
    }

    @PostMapping("/deleteMfaDevice")
    public ResponseData<Void> deleteMfaDevice(@Validated @RequestBody UpdateUserBasicParams params) {
        audit.run("OCI_TENANT_MFA_CLEAR", params.getUserId(), "cfgId=" + params.getOciCfgId(), () -> tenantService.deleteMfaDevice(params));
        return ResponseData.successData("清除 MFA 设备成功");
    }

    @PostMapping("/deleteApiKey")
    public ResponseData<Void> deleteApiKey(@Validated @RequestBody UpdateUserBasicParams params) {
        audit.run("OCI_TENANT_API_KEYS_CLEAR", params.getUserId(), "cfgId=" + params.getOciCfgId(), () -> tenantService.deleteApiKey(params));
        return ResponseData.successData("清除所有 API 成功");
    }

    @PostMapping("/resetPassword")
    public ResponseData<Void> resetPassword(@Validated @RequestBody UpdateUserBasicParams params) {
        audit.run("OCI_TENANT_PASSWORD_RESET", params.getUserId(), "cfgId=" + params.getOciCfgId(), () -> tenantService.resetPassword(params));
        return ResponseData.successData("重置用户密码成功");
    }

    @PostMapping("/updateUserInfo")
    public ResponseData<Void> updateUserInfo(@Validated @RequestBody UpdateUserInfoParams params) {
        audit.run("OCI_TENANT_USER_UPDATE", params.getUserId(), "cfgId=" + params.getOciCfgId(), () -> tenantService.updateUserInfo(params));
        return ResponseData.successData("更新用户信息成功");
    }

    @PostMapping("/updatePwdEx")
    public ResponseData<Void> updatePwdEx(@Validated @RequestBody UpdatePwdExpirationPolicyParams params) {
        audit.run("OCI_TENANT_PASSWORD_POLICY", params.getCfgId(), "expiresAfter=" + params.getPasswordExpiresAfter(), () -> tenantService.updatePwdExpirationPolicy(params));
        return ResponseData.successData("更新密码过期时间成功");
    }

}
