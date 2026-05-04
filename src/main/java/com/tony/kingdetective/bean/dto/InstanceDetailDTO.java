package com.tony.kingdetective.bean.dto;

import com.oracle.bmc.core.model.Instance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * InstanceDetailDTO
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/11/7 14:40
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstanceDetailDTO {

    private String taskId;
    @Builder.Default
    private boolean isNoShape = false;
    @Builder.Default
    private boolean isSuccess = false;
    @Builder.Default
    private boolean isOut = false;
    @Builder.Default
    private boolean isNoPubVcn = false;
    @Builder.Default
    private boolean isTooManyReq = false;
    @Builder.Default
    private boolean isDie = false;
    private String publicIp;
    private String image;
    private String shape;
    private String architecture;
    private String username;
    private String region;
    @Builder.Default
    private Float ocpus = 1F;
    @Builder.Default
    private Float memory = 6F;
    @Builder.Default
    private Long disk = 50L;
    private String rootPassword;
    @Builder.Default
    private long createNumbers = 0;
    Instance instance;

}
