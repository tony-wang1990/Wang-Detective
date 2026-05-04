package com.tony.kingdetective.service;

import com.tony.kingdetective.bean.params.oci.tenant.*;
import com.tony.kingdetective.bean.response.oci.tenant.TenantInfoRsp;

/**
 * @ClassName ITenantService
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-12 16:02
 **/
public interface ITenantService {
    TenantInfoRsp tenantInfo(GetTenantInfoParams params);

    void deleteMfaDevice(UpdateUserBasicParams params);

    void deleteApiKey(UpdateUserBasicParams params);

    void resetPassword(UpdateUserBasicParams params);

    void updateUserInfo(UpdateUserInfoParams params);

    void deleteUser(UpdateUserBasicParams params);

    void updatePwdExpirationPolicy(UpdatePwdExpirationPolicyParams params);
}
