package com.tony.kingdetective.bean.params.ops;

import lombok.Data;

@Data
public class SshSessionCreateParams {
    private SshCredentialParams credential;
    private Integer ttlMinutes = 15;
}
