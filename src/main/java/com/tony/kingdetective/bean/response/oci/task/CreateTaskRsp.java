package com.tony.kingdetective.bean.response.oci.task;

import lombok.Data;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.response
 * @className: CreateTaskRsp
 * @author: Tony Wang
 * @date: 2024/11/15 21:42
 */
@Data
public class CreateTaskRsp {

    private String id;

    private String username;

    private String region;

    private String ocpus;

    private String memory;

    private Integer disk;

    private String architecture;

    private Integer interval;

    private Integer createNumbers;

    private String operationSystem;

    private String createTime;

    private String counts;
}
