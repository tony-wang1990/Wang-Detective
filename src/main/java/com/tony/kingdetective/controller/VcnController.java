package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.oci.vcn.RemoveVcnParams;
import com.tony.kingdetective.bean.params.oci.vcn.VcnPageParams;
import com.tony.kingdetective.bean.response.oci.vcn.VcnPageRsp;
import com.tony.kingdetective.service.IVcnService;
import com.tony.kingdetective.utils.CommonUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.controller
 * @className: VcnController
 * @author: Tony Wang
 * @date: 2025/3/3 20:56
 */
@RestController
@RequestMapping(path = "/api/vcn")
public class VcnController {

    @Resource
    private IVcnService vcnService;

    @RequestMapping(path = "page")
    public ResponseData<Page<VcnPageRsp.VcnInfo>> page(@Validated @RequestBody VcnPageParams params) {
        return ResponseData.successData(vcnService.page(params));
    }

    @RequestMapping(path = "/remove")
    public ResponseData<Void> remove(@Validated @RequestBody RemoveVcnParams params) {
        vcnService.remove(params);
        return ResponseData.successData("删除vcn成功");
    }
}
