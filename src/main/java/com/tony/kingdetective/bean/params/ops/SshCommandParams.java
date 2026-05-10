package com.tony.kingdetective.bean.params.ops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SshCommandParams {
    private SshCredentialParams credential;
    @NotBlank(message = "Command cannot be blank")
    private String command;
    private Integer timeoutSeconds = 30;
}
