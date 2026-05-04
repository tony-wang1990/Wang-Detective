package com.tony.kingdetective.bean.params;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: IdParams
 * @author: Tony Wang
 * @date: 2024/11/13 23:52
 */
@Data
public class IdParams {

    @NotBlank(message = "id不能为空")
    private String id;
}
