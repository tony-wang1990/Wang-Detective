package com.tony.kingdetective.bean.params.oci.securityrule;

import com.tony.kingdetective.bean.params.BasicPageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.oci.securityrule
 * @className: GetSecurityRuleListPageParams
 * @author: Tony Wang
 * @date: 2025/3/1 16:08
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetSecurityRuleListPageParams extends BasicPageParams {

    @NotBlank(message = "api配置id不能为空")
    private String ociCfgId;
    @NotBlank(message = "vcnId不能为空")
    private String vcnId;
    private Integer type;
    private boolean cleanReLaunch;
}
