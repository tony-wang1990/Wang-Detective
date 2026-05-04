package com.tony.kingdetective.bean.params.oci.vcn;

import com.tony.kingdetective.bean.params.BasicPageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.oci.vcn
 * @className: VcnPageParams
 * @author: Tony Wang
 * @date: 2025/3/3 21:02
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VcnPageParams extends BasicPageParams {

    @NotBlank(message = "配置id不能为空")
    private String ociCfgId;
    private boolean cleanReLaunch;
}
