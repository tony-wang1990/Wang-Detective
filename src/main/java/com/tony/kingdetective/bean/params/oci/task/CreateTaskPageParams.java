package com.tony.kingdetective.bean.params.oci.task;

import lombok.Data;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: CreateTaskPageParams
 * @author: Tony Wang
 * @date: 2024/11/15 21:37
 */
@Data
public class CreateTaskPageParams {

    private String keyword;
    private long currentPage;
    private long pageSize;
    private String architecture;
}
