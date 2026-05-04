package com.tony.kingdetective.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oracle.bmc.core.model.Subnet;
import com.tony.kingdetective.bean.constant.CacheConstant;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.params.oci.vcn.RemoveVcnParams;
import com.tony.kingdetective.bean.params.oci.vcn.VcnPageParams;
import com.tony.kingdetective.bean.response.oci.vcn.VcnPageRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.service.IVcnService;
import com.tony.kingdetective.utils.CommonUtils;
import com.tony.kingdetective.utils.CustomExpiryGuavaCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.service.impl
 * @className: VcnServiceImpl
 * @author: Tony Wang
 * @date: 2025/3/3 20:21
 */
@Service
@Slf4j
public class VcnServiceImpl implements IVcnService {

    @Resource
    private ISysService sysService;
    @Resource
    private CustomExpiryGuavaCache<String, Object> customCache;

    @Override
    public Page<VcnPageRsp.VcnInfo> page(VcnPageParams params) {
        if (params.isCleanReLaunch()) {
            customCache.remove(CacheConstant.PREFIX_VCN_PAGE + params.getOciCfgId());
        }

        @SuppressWarnings("unch ecked")
        List<VcnPageRsp.VcnInfo> vcnList = (List<VcnPageRsp.VcnInfo>) customCache.get(CacheConstant.PREFIX_VCN_PAGE + params.getOciCfgId());
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        if (ObjUtil.isEmpty(vcnList)) {
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                vcnList = fetcher.listVcn().parallelStream().map(vcn -> {
                    VcnPageRsp.VcnInfo vcnInfo = new VcnPageRsp.VcnInfo();
                    vcnInfo.setId(vcn.getId());
                    vcnInfo.setDisplayName(vcn.getDisplayName());
                    vcnInfo.setStatus(vcn.getLifecycleState().getValue());
                    vcnInfo.setVisibility(fetcher.checkVcnIsPublic(vcn));
                    vcnInfo.setCreateTime(DateUtil.format(vcn.getTimeCreated(), CommonUtils.DATETIME_FMT_NORM));
                    return vcnInfo;
                }).collect(Collectors.toList());
            } catch (Exception e) {
                log.error("获取VCN列表失败", e);
                throw new OciException(-1, "获取VCN列表失败");
            }
        }

        customCache.put(CacheConstant.PREFIX_VCN_PAGE + params.getOciCfgId(), vcnList, 10 * 60 * 1000);
        List<VcnPageRsp.VcnInfo> resList = vcnList.parallelStream()
                .filter(x -> CommonUtils.contains(x.getDisplayName(), params.getKeyword(), true))
                .collect(Collectors.toList());
        List<VcnPageRsp.VcnInfo> vcnInfoList = CommonUtils.getPage(resList, params.getCurrentPage(), params.getPageSize());
        return VcnPageRsp.buildPage(vcnInfoList, params.getPageSize(), params.getCurrentPage(), vcnInfoList.size());
    }

    @Override
    public void remove(RemoveVcnParams params) {
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        params.getVcnIds().parallelStream().forEach(vcnId -> {
            String vcnName = null;
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                fetcher.deleteVcnById(vcnId);
            } catch (Exception e) {
                log.error("删除VCN：失败", vcnName, e);
                throw new OciException(-1, "删除 VCN 失败");
            }
        });
    }
}
