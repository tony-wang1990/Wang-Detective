package com.tony.kingdetective.bean.params.cf;

import lombok.Data;

import java.util.List;

/**
 * @ClassName RemoveCfDnsRecordsParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-19 14:46
 **/
@Data
public class RemoveCfDnsRecordsParams {

    private List<String> proxyDomainList;
    private String zoneId;
    private String apiToken;
}
