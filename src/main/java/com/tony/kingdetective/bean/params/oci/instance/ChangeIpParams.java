package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: ChangeIpParams
 * @author: Tony Wang
 * @date: 2024/11/14 0:03
 */
@Data
public class ChangeIpParams {

    @NotBlank(message = "配置id不能为空")
    private String ociCfgId;

    @NotBlank(message = "实例id不能为空")
    private String instanceId;

    private List<String> cidrList;

    @NotBlank(message = "vnicId不能为空")
    private String vnicId;

    @NotNull(message = "是否更新 Cloudflare DNS 记录不能为空")
    private boolean changeCfDns;
    private String domainPrefix;
    private String selectedDomainCfgId;
    private boolean enableProxy;
    private Integer ttl;
    private String remark;
}
