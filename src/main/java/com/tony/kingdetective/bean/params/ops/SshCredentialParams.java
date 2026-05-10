package com.tony.kingdetective.bean.params.ops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SshCredentialParams {
    @NotBlank(message = "SSH host cannot be blank")
    private String host;
    private Integer port = 22;
    @NotBlank(message = "SSH username cannot be blank")
    private String username;
    private String password;
    private String privateKey;
    private String passphrase;
    private Integer connectTimeoutSeconds = 10;
}
