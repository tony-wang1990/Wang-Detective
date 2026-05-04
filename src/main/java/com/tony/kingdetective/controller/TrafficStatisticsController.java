package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.dto.ValueLabelDTO;
import com.tony.kingdetective.bean.params.oci.traffic.GetTrafficDataParams;
import com.tony.kingdetective.bean.response.oci.traffic.FetchInstancesRsp;
import com.tony.kingdetective.bean.response.oci.traffic.GetConditionRsp;
import com.tony.kingdetective.bean.response.oci.traffic.GetTrafficDataRsp;
import com.tony.kingdetective.service.ITrafficService;
import com.tony.kingdetective.utils.CommonUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.controller
 * @className: TrafficStatisticsController
 * @author: Tony Wang
 * @date: 2025/3/7 20:35
 */
@RestController
@RequestMapping(path = "/api/traffic")
public class TrafficStatisticsController {

    @Resource
    private ITrafficService trafficService;

    @RequestMapping("data")
    public ResponseData<GetTrafficDataRsp> getData(@Validated @RequestBody GetTrafficDataParams params) {
        return ResponseData.successData(trafficService.getData(params), "获取流量数据成功");
    }

    @RequestMapping("getCondition")
    public ResponseData<GetConditionRsp> getCondition(@RequestParam("ociCfgId") String ociCfgId) {
        return ResponseData.successData(trafficService.getCondition(ociCfgId), "获取查询条件成功");
    }

    @RequestMapping("fetchInstances")
    public ResponseData<FetchInstancesRsp> fetchInstances(@RequestParam("ociCfgId") String ociCfgId,
                                                          @RequestParam("region") String region) {
        return ResponseData.successData(trafficService.fetchInstances(ociCfgId, region), "获取区域实例成功");
    }

    @RequestMapping("fetchVnics")
    public ResponseData<List<ValueLabelDTO>> fetchVnics(@RequestParam("ociCfgId") String ociCfgId,
                                                        @RequestParam("region") String region,
                                                        @RequestParam("instanceId") String instanceId) {
        return ResponseData.successData(trafficService.fetchVnics(ociCfgId, region, instanceId), "获取区域实例成功");
    }

}
