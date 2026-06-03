package com.tony.kingdetective.bean.params.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAdminCredentialParams {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新登录账号不能为空")
    private String newAccount;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
