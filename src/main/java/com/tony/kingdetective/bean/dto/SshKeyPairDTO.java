package com.tony.kingdetective.bean.dto;

import lombok.Data;

/**
 * @ClassName SshKeyPair
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-06-03 18:05
 **/
@Data
public class SshKeyPairDTO {
    private final String publicKey; // PEM format
    private final String privateKey; // PEM format
    private final String publicKeyOpenSSH; // OpenSSH format
}
