package com.tony.kingdetective.bean.response.ops;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SshCommandRsp {
    private String host;
    private String name;
    private Integer exitStatus;
    private Boolean timedOut;
    private Long durationMillis;
    private String stdout;
    private String stderr;
}
