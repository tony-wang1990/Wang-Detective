package com.tony.kingdetective.bean.params.ops;

import lombok.Data;

@Data
public class SftpParams {
    private SshCredentialParams credential;
    private String path = ".";
    private String targetPath;
    private String content;
    private Boolean recursive = false;
}
