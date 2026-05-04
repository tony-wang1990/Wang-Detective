package com.tony.kingdetective.bean.params.ipdata;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.ipdata
 * @className: AddIpDataParams
 * @author: Tony Wang
 * @date: 2025/8/5 21:55
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddIpDataParams {

    @NotBlank(message = "ip地址不能为空")
    private String ip;
}
