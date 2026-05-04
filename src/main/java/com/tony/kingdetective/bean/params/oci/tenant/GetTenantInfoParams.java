package com.tony.kingdetective.bean.params.oci.tenant;

import lombok.Data;

/**
 * @ClassName GetTenantInfoParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-12 15:57
 **/
@Data
public class GetTenantInfoParams {

    private String ociCfgId;
    private String region;
    private boolean cleanReLaunch;
}
