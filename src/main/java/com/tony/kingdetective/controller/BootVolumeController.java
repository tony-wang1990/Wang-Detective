package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.oci.volume.BootVolumePageParams;
import com.tony.kingdetective.bean.params.oci.volume.TerminateBootVolumeParams;
import com.tony.kingdetective.bean.params.oci.volume.UpdateBootVolumeParams;
import com.tony.kingdetective.bean.response.oci.volume.BootVolumeListPage;
import com.tony.kingdetective.service.IBootVolumeService;
import com.tony.kingdetective.utils.CommonUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * <p>
 * BootVolumeController
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/31 15:46
 */
@RestController
@RequestMapping(path = "/api/bootVolume")
public class BootVolumeController {

    @Resource
    private IBootVolumeService bootVolumeService;

    @PostMapping(path = "/page")
    public ResponseData<Page<BootVolumeListPage.BootVolumeInfo>> userPage(@Validated @RequestBody BootVolumePageParams params) {
        return ResponseData.successData(bootVolumeService.bootVolumeListPage(params), "获取引导卷分页列表成功");
    }

    @PostMapping(path = "/terminate")
    public ResponseData<Void> terminate(@Validated @RequestBody TerminateBootVolumeParams params) {
        bootVolumeService.terminateBootVolume(params);
        return ResponseData.successData("终止引导卷命令下发成功");
    }

    @PostMapping(path = "/update")
    public ResponseData<Void> update(@Validated @RequestBody UpdateBootVolumeParams params) {
        bootVolumeService.update(params);
        return ResponseData.successData("更改引导卷配置成功");
    }
}
