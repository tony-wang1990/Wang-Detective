package com.tony.kingdetective.bean.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * SysUser
 * </p >
 *
 * @author yohann
 * @since 2024/11/7 14:34
 */
@Data
@Builder
public class SysUserDTO {

    private String taskId;
    private OciCfg ociCfg;
    private String username;
    @Builder.Default
    private float ocpus = 1F;
    @Builder.Default
    private float memory = 6F;
    private Long disk;
    @Builder.Default
    private String architecture = "ARM";
    @Builder.Default
    private Long interval = 60L;
    @Builder.Default
    private volatile int createNumbers = 0;
    private String rootPassword;
    @Builder.Default
    private String operationSystem = "Ubuntu";
    private List<CloudInstance> instanceList;
    @Builder.Default
    private boolean joinChannelBroadcast = true;

    @Data
    @Builder
    public static class OciCfg {
        private String id;
        private String tenantId;
        private String userId;
        private String fingerprint;
        private String privateKeyPath;
        private String privateKey; // Added: in-memory private key content
        private String region;
        private String compartmentId;
        private Boolean deleted;
    }

    @Builder
    @Data
    public static class CloudInstance {
        private String region;
        private String name;
        private String ocId;
        private List<String> publicIp;
        private String shape;
        private String subnetId;
//    private String volumeSize;
    }
}
