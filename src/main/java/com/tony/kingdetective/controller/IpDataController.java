package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.ipdata.*;
import com.tony.kingdetective.bean.response.ipdata.IpDataPageRsp;
import com.tony.kingdetective.service.IIpDataService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.controller
 * @className: IpDataController
 * @author: Tony Wang
 * @date: 2025/8/5 21:53
 */
@RestController
@RequestMapping(path = "/api/ipData")
public class IpDataController {

    @Resource
    private IIpDataService ipDataService;

    @PostMapping("/add")
    public ResponseData<Void> add(@Validated @RequestBody AddIpDataParams params){
        ipDataService.add(params);
        return ResponseData.successData();
    }

    @PostMapping("/update")
    public ResponseData<Void> update(@Validated @RequestBody UpdateIpDataParams params){
        ipDataService.updateIpData(params);
        return ResponseData.successData();
    }

    @PostMapping("/remove")
    public ResponseData<Void> remove(@Validated @RequestBody RemoveIpDataParams params){
        ipDataService.removeIpData(params);
        return ResponseData.successData();
    }

    @PostMapping("/loadOciIpData")
    public ResponseData<Void> loadOciIpData(){
        ipDataService.loadOciIpData();
        return ResponseData.successData();
    }

    @PostMapping("/page")
    public ResponseData<Page<IpDataPageRsp>> page(@Validated @RequestBody PageIpDataParams params){
        return ResponseData.successData(ipDataService.pageIpData(params));
    }
}
