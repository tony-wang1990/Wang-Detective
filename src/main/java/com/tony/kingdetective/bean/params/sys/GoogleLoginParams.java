package com.tony.kingdetective.bean.params.sys;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.sys
 * @className: GoogleLoginParams
 * @author: Tony Wang
 * @date: 2026/01/02
 */
@Data
public class GoogleLoginParams {

    @NotBlank(message = "Google credential不能为空")
    private String credential;
}
