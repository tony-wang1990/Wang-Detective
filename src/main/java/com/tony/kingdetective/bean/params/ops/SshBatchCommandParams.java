package com.tony.kingdetective.bean.params.ops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SshBatchCommandParams {
    private List<Target> targets;
    @NotBlank(message = "Command cannot be blank")
    private String command;
    private Integer timeoutSeconds = 30;

    @Data
    public static class Target {
        private String name;
        private SshCredentialParams credential;
    }
}
