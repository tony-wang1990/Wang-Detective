package com.tony.kingdetective.service;

import cn.hutool.http.HttpResponse;
import com.tony.kingdetective.bean.dto.CfDnsRecordDTO;
import com.tony.kingdetective.bean.params.cf.*;

import java.util.List;

/**
 * @ClassName ICfService
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-19 14:32
 **/
public interface ICfApiService {

    HttpResponse addCfDnsRecords(AddCfDnsRecordsParams params);

    void removeCfDnsRecords(RemoveCfDnsRecordsParams params);

    void removeCfDnsByIdsRecords(RemoveCfDnsByIdsParams params);

    HttpResponse updateCfDnsRecords(UpdateCfDnsRecordsParams params);

    List<CfDnsRecordDTO> getCfDnsRecords(GetCfDnsRecordsParams params);
}
