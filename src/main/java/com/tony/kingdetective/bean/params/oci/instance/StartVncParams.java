package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @ClassName StartVncParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-06-04 11:02
 **/
@Data
public class StartVncParams {

    @NotBlank(message = "配置ID不能为空")
    private String ociCfgId;
    private String compartmentId;
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;
}
