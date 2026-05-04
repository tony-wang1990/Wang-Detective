package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.oci.instance
 * @className: AutoRescueParams
 * @author: Tony Wang
 * @date: 2025/7/19 14:28
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AutoRescueParams extends GetInstanceCfgInfoParams {

    @NotBlank(message = "实例名称不能为空")
    private String name;

    @NotNull(message = "keepBackupVolume不能为空")
    private Boolean keepBackupVolume = true;
}
