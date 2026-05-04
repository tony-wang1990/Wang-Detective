package com.tony.kingdetective.bean.params.oci.vcn;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.oci.vcn
 * @className: RemoveVcnParams
 * @author: Tony Wang
 * @date: 2025/3/3 22:59
 */
@Data
public class RemoveVcnParams {

    @NotEmpty(message = "vcnId列表不能为空")
    private List<String> vcnIds;
    @NotBlank(message = "配置id不能为空")
    private String ociCfgId;
}
