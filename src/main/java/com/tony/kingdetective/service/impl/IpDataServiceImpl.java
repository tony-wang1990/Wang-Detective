package com.tony.kingdetective.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.IpData;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.params.ipdata.AddIpDataParams;
import com.tony.kingdetective.bean.params.ipdata.PageIpDataParams;
import com.tony.kingdetective.bean.params.ipdata.RemoveIpDataParams;
import com.tony.kingdetective.bean.params.ipdata.UpdateIpDataParams;
import com.tony.kingdetective.bean.response.ipdata.IpDataPageRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.enums.IpDataTypeEnum;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.IIpDataService;
import com.tony.kingdetective.mapper.IpDataMapper;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.utils.CommonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * @author Tony Wang
 * @description 针对表【ip_data】的数据库操作Service实现
 * @createDate 2025-08-04 17:28:41
 */
@Service
@Slf4j
public class IpDataServiceImpl extends ServiceImpl<IpDataMapper, IpData> implements IIpDataService {

    @Resource
    private IInstanceService instanceService;
    @Resource
    private ISysService sysService;
    @Resource
    private IOciUserService ociUserService;
    @Resource
    private ExecutorService virtualExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AddIpDataParams params) {
        String jsonStr = HttpUtil.get(String.format("https://ipapi.co/%s/json", params.getIp()));
        JSONObject json = JSONUtil.parseObj(jsonStr);
        IpData ipData = new IpData();
        ipData.setId(IdUtil.getSnowflakeNextIdStr());
        ipData.setIp(json.getStr("ip"));
        ipData.setCountry(json.getStr("country"));
        ipData.setArea(json.getStr("region"));
        ipData.setCity(json.getStr("city"));
        ipData.setOrg(json.getStr("org"));
        ipData.setAsn(json.getStr("asn"));
        ipData.setLat(Double.valueOf(json.getStr("latitude")));
        ipData.setLng(Double.valueOf(json.getStr("longitude")));
        List<IpData> ipDataList = this.list(new LambdaQueryWrapper<IpData>()
                .eq(IpData::getIp, json.getStr("ip")));
        if (CollectionUtil.isNotEmpty(ipDataList)) {
            this.remove(new LambdaQueryWrapper<IpData>().eq(IpData::getIp, json.getStr("ip")));
        }
        this.save(ipData);
    }

    @Override
    public void loadOciIpData() {
        virtualExecutor.execute(() -> {
            log.info("【同步IP数据任务】开始同步已有的oci配置的最新IP数据...");
            this.remove(new LambdaQueryWrapper<IpData>().eq(IpData::getType, IpDataTypeEnum.IP_DATA_ORACLE.getCode()));
            log.info("【同步IP数据任务】清除已有的oci配置的旧IP数据成功");
            List<String> ociCfgIds = ociUserService.listObjs(new LambdaQueryWrapper<OciUser>().select(OciUser::getId), String::valueOf);
            if (CollectionUtil.isNotEmpty(ociCfgIds)) {
                for (String x : ociCfgIds) {
                    SysUserDTO ociUser = sysService.getOciUser(x);
                    try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(ociUser)) {
                        fetcher.getAvailabilityDomains();
                    } catch (Exception e) {
                        log.warn("oci配置：[{}]，区域：[{}] 已失效，跳过本次IP数据同步", ociUser.getUsername(), ociUser.getOciCfg().getRegion());
                        continue;
                    }
                    List<SysUserDTO.CloudInstance> cloudInstances = instanceService.listRunningInstances(ociUser);
                    if (CollectionUtil.isEmpty(cloudInstances)) {
                        continue;
                    }
                    for (SysUserDTO.CloudInstance y : cloudInstances) {
                        if (CollectionUtil.isEmpty(y.getPublicIp())) {
                            continue;
                        }
                        for (String z : y.getPublicIp()) {
                            try {
                                String jsonStr = HttpUtil.get(String.format("https://ipapi.co/%s/json", z));
                                JSONObject json = JSONUtil.parseObj(jsonStr);
                                IpData ipData = new IpData();
                                ipData.setId(IdUtil.getSnowflakeNextIdStr());
                                ipData.setIp(json.getStr("ip"));
                                ipData.setCountry(json.getStr("country"));
                                ipData.setArea(json.getStr("region"));
                                ipData.setCity(json.getStr("city"));
                                ipData.setOrg(json.getStr("org"));
                                ipData.setAsn(json.getStr("asn"));
                                ipData.setLat(json.getDouble("latitude"));
                                ipData.setLng(json.getDouble("longitude"));
                                ipData.setType(IpDataTypeEnum.IP_DATA_ORACLE.getCode());
                                if (this.save(ipData)) {
                                    log.info("oci配置：[{}]，区域：[{}]，IP地址：[{}] 已添加至地图IP数据", ociUser.getUsername(), ociUser.getOciCfg().getRegion(), z);
                                }
                            } catch (Exception e) {
                                log.error("oci配置：[{}]，区域：[{}]，IP地址：[{}] 添加至地图IP数据失败", ociUser.getUsername(), ociUser.getOciCfg().getRegion(), z, e);
                            }
                        }
                    }
                }
            }
            log.info("【同步IP数据任务】任务完成");
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIpData(UpdateIpDataParams params) {
        IpData ipData = Optional.ofNullable(this.getById(params.getId())).orElseThrow(() -> new OciException(-1, "当前记录不存在"));
        String jsonStr = HttpUtil.get(String.format("https://ipapi.co/%s/json", ipData.getIp()));
        JSONObject json = JSONUtil.parseObj(jsonStr);
        this.update(new LambdaUpdateWrapper<IpData>().eq(IpData::getId, params.getId())
                .set(IpData::getIp, json.getStr("ip"))
                .set(IpData::getCountry, json.getStr("country"))
                .set(IpData::getArea, json.getStr("region"))
                .set(IpData::getCity, json.getStr("city"))
                .set(IpData::getOrg, json.getStr("org"))
                .set(IpData::getAsn, json.getStr("asn"))
                .set(IpData::getLat, json.getDouble("latitude"))
                .set(IpData::getLng, json.getDouble("longitude")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeIpData(RemoveIpDataParams params) {
        this.removeByIds(params.getIdList());
    }

    @Override
    public Page<IpDataPageRsp> pageIpData(PageIpDataParams params) {
        List<IpDataPageRsp> list = this.baseMapper.pageIpData(params.getOffset(), params.getPageSize(), params.getKeyword());
        Long total = this.baseMapper.pageIpDataTotal(params.getKeyword());
        return CommonUtils.buildPage(list, params.getPageSize(), params.getCurrentPage(), total);
    }
}




