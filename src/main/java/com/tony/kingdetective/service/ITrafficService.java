package com.tony.kingdetective.service;

import com.tony.kingdetective.bean.dto.ValueLabelDTO;
import com.tony.kingdetective.bean.params.oci.traffic.GetTrafficDataParams;
import com.tony.kingdetective.bean.response.oci.traffic.FetchInstancesRsp;
import com.tony.kingdetective.bean.response.oci.traffic.GetConditionRsp;
import com.tony.kingdetective.bean.response.oci.traffic.GetTrafficDataRsp;

import java.util.List;

public interface ITrafficService {
    GetTrafficDataRsp getData(GetTrafficDataParams params);

    GetConditionRsp getCondition(String ociCfgId);

    FetchInstancesRsp fetchInstances(String ociCfgId, String region);

    List<ValueLabelDTO> fetchVnics(String ociCfgId, String region, String instanceId);

}
