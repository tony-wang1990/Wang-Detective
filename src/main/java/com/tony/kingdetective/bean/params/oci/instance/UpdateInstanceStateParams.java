package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: UpdateInstanceStateParams
 * @author: Tony Wang
 * @date: 2024/11/28 21:28
 */
@Data
public class UpdateInstanceStateParams {

    @NotBlank(message = "配置id不能为空")
    private String ociCfgId;

    @NotBlank(message = "实例id不能为空")
    private String instanceId;

    @NotBlank(message = "实例操作不能为空")
    private String action;

}
