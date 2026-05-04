package com.tony.kingdetective.bean.params.cf;

import lombok.Data;

/**
 * @ClassName UpdateCfCfgParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-20 17:37
 **/
@Data
public class UpdateCfCfgParams {

    private String id;
    private String domain;
    private String zoneId;
    private String apiToken;
}
