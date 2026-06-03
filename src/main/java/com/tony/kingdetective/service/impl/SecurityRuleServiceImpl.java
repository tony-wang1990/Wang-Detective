package com.tony.kingdetective.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.UpdateSecurityListRequest;
import com.tony.kingdetective.bean.Tuple2;
import com.tony.kingdetective.bean.constant.CacheConstant;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.params.oci.securityrule.*;
import com.tony.kingdetective.bean.response.oci.securityrule.SecurityRuleListRsp;
import com.tony.kingdetective.bean.response.oci.vcn.VcnPageRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.enums.SecurityRuleProtocolEnum;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.ISecurityRuleService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.utils.CommonUtils;
import com.tony.kingdetective.utils.CustomExpiryGuavaCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.service.impl
 * @className: SecurityRuleServiceImpl
 * @author: Tony Wang
 * @date: 2025/3/1 15:38
 */
@Service
@Slf4j
public class SecurityRuleServiceImpl implements ISecurityRuleService {

    @Resource
    private ISysService sysService;
    @Resource
    private CustomExpiryGuavaCache<String, Object> customCache;

    @Override
    public Page<SecurityRuleListRsp.SecurityRuleInfo> page(GetSecurityRuleListPageParams params) {
        if (!Integer.valueOf(0).equals(params.getType()) && !Integer.valueOf(1).equals(params.getType())) {
            throw new OciException(-1, "安全规则类型不能为空，请选择入站或出站规则");
        }
        if (params.isCleanReLaunch()) {
            customCache.remove(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_PAGE + params.getOciCfgId());
            customCache.remove(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_PAGE + params.getOciCfgId());
            customCache.remove(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_MAP + params.getVcnId());
            customCache.remove(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_MAP + params.getVcnId());
        }

        List<IngressSecurityRule> ingressSecurityRuleList = (List<IngressSecurityRule>) customCache.get(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_PAGE + params.getOciCfgId());
        List<EgressSecurityRule> egressSecurityRuleList = (List<EgressSecurityRule>) customCache.get(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_PAGE + params.getOciCfgId());
        Map<String, IngressSecurityRule> ingressMap = (Map<String, IngressSecurityRule>) customCache.get(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_MAP + params.getVcnId());
        Map<String, EgressSecurityRule> egressMap = (Map<String, EgressSecurityRule>) customCache.get(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_MAP + params.getVcnId());

        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        List<SecurityRuleListRsp.SecurityRuleInfo> rspRuleList;

        if (CollectionUtil.isEmpty(ingressSecurityRuleList) || CollectionUtil.isEmpty(egressSecurityRuleList) ||
                CollectionUtil.isEmpty(ingressMap) || CollectionUtil.isEmpty(egressMap)) {
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                SecurityList securityList = fetcher.listSecurityRule(fetcher.getVcnById(params.getVcnId()));
                ingressSecurityRuleList = securityList.getIngressSecurityRules() == null ? Collections.emptyList() : securityList.getIngressSecurityRules();
                egressSecurityRuleList = securityList.getEgressSecurityRules() == null ? Collections.emptyList() : securityList.getEgressSecurityRules();

                ingressMap = new HashMap<>();
                egressMap = new HashMap<>();
            } catch (Exception e) {
                log.error("获取安全列表规则失败", e);
                throw new OciException(-1, "获取安全列表规则失败");
            }
        }

        if (Integer.valueOf(0).equals(params.getType())) {
            ingressMap = new HashMap<>();
            rspRuleList = buildIngressRuleInfoList(ingressSecurityRuleList, ingressMap);
        } else {
            egressMap = new HashMap<>();
            rspRuleList = buildEgressRuleInfoList(egressSecurityRuleList, egressMap);
        }

        customCache.put(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_PAGE + params.getOciCfgId(), ingressSecurityRuleList, 10 * 60 * 1000);
        customCache.put(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_PAGE + params.getOciCfgId(), egressSecurityRuleList, 10 * 60 * 1000);
        customCache.put(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_MAP + params.getVcnId(), ingressMap, 10 * 60 * 1000);
        customCache.put(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_MAP + params.getVcnId(), egressMap, 10 * 60 * 1000);

        List<SecurityRuleListRsp.SecurityRuleInfo> resList = rspRuleList.parallelStream()
                .filter(x -> CommonUtils.contains(x.getSourceOrDestination(), params.getKeyword(), true) ||
                        CommonUtils.contains(x.getDescription(), params.getKeyword(), true) ||
                        CommonUtils.contains(x.getSourcePort(), params.getKeyword(), true) ||
                        CommonUtils.contains(x.getDestinationPort(), params.getKeyword(), true))
                .sorted(Comparator.comparing(SecurityRuleListRsp.SecurityRuleInfo::getSourceOrDestination))
                .collect(Collectors.toList());
        List<SecurityRuleListRsp.SecurityRuleInfo> pageList = CommonUtils.getPage(resList, params.getCurrentPage(), params.getPageSize());
        return VcnPageRsp.buildPage(pageList, params.getPageSize(), params.getCurrentPage(), resList.size());
    }

    private List<SecurityRuleListRsp.SecurityRuleInfo> buildIngressRuleInfoList(List<IngressSecurityRule> ruleList,
                                                                                Map<String, IngressSecurityRule> ruleMap) {
        return ruleList.parallelStream().map(ingressSecurityRule -> {
            SecurityRuleListRsp.SecurityRuleInfo info = new SecurityRuleListRsp.SecurityRuleInfo();
            String ruleId = IdUtil.getSnowflakeNextIdStr();
            info.setId(ruleId);
            info.setIsStateless(ingressSecurityRule.getIsStateless());
            info.setProtocol(SecurityRuleProtocolEnum.fromCode(ingressSecurityRule.getProtocol()).getDesc());
            info.setSourceOrDestination(ingressSecurityRule.getSource());
            info.setTypeAndCode(buildIcmpText(ingressSecurityRule.getProtocol(), ingressSecurityRule.getIcmpOptions()));
            info.setDescription(ingressSecurityRule.getDescription());
            info.setSourcePort(buildSourcePort(ingressSecurityRule.getProtocol(), ingressSecurityRule.getTcpOptions(), ingressSecurityRule.getUdpOptions()));
            info.setDestinationPort(buildDestinationPort(ingressSecurityRule.getProtocol(), ingressSecurityRule.getTcpOptions(), ingressSecurityRule.getUdpOptions()));
            ruleMap.put(ruleId, ingressSecurityRule);
            return info;
        }).collect(Collectors.toList());
    }

    private List<SecurityRuleListRsp.SecurityRuleInfo> buildEgressRuleInfoList(List<EgressSecurityRule> ruleList,
                                                                               Map<String, EgressSecurityRule> ruleMap) {
        return ruleList.parallelStream().map(egressSecurityRule -> {
            SecurityRuleListRsp.SecurityRuleInfo info = new SecurityRuleListRsp.SecurityRuleInfo();
            String ruleId = IdUtil.getSnowflakeNextIdStr();
            info.setId(ruleId);
            info.setIsStateless(egressSecurityRule.getIsStateless());
            info.setProtocol(SecurityRuleProtocolEnum.fromCode(egressSecurityRule.getProtocol()).getDesc());
            info.setSourceOrDestination(egressSecurityRule.getDestination());
            info.setTypeAndCode(buildIcmpText(egressSecurityRule.getProtocol(), egressSecurityRule.getIcmpOptions()));
            info.setDescription(egressSecurityRule.getDescription());
            info.setSourcePort(buildSourcePort(egressSecurityRule.getProtocol(), egressSecurityRule.getTcpOptions(), egressSecurityRule.getUdpOptions()));
            info.setDestinationPort(buildDestinationPort(egressSecurityRule.getProtocol(), egressSecurityRule.getTcpOptions(), egressSecurityRule.getUdpOptions()));
            ruleMap.put(ruleId, egressSecurityRule);
            return info;
        }).collect(Collectors.toList());
    }

    private String buildIcmpText(String protocol, IcmpOptions icmpOptions) {
        if (!"1".equals(protocol)) {
            return null;
        }
        if (icmpOptions == null) {
            return "全部";
        }
        return icmpOptions.getType() + (icmpOptions.getCode() == null ? "" : ", " + icmpOptions.getCode());
    }

    private String buildSourcePort(String protocol, TcpOptions tcpOptions, UdpOptions udpOptions) {
        if (!Arrays.asList("6", "17").contains(protocol)) {
            return null;
        }
        if ("6".equals(protocol) && tcpOptions != null) {
            return portRangeText(tcpOptions.getSourcePortRange());
        }
        if ("17".equals(protocol) && udpOptions != null) {
            return portRangeText(udpOptions.getSourcePortRange());
        }
        return "全部";
    }

    private String buildDestinationPort(String protocol, TcpOptions tcpOptions, UdpOptions udpOptions) {
        if (!Arrays.asList("6", "17").contains(protocol)) {
            return null;
        }
        if ("6".equals(protocol) && tcpOptions != null) {
            return portRangeText(tcpOptions.getDestinationPortRange());
        }
        if ("17".equals(protocol) && udpOptions != null) {
            return portRangeText(udpOptions.getDestinationPortRange());
        }
        return "全部";
    }

    private String portRangeText(PortRange range) {
        if (range == null || range.getMin() == null || range.getMax() == null) {
            return "全部";
        }
        return range.getMin().equals(range.getMax()) ? String.valueOf(range.getMin()) : range.getMin() + "-" + range.getMax();
    }

    @Override
    public void addIngress(AddIngressSecurityRuleParams params) {
        List<String> list = Arrays.asList("6", "17");
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            Vcn vcn = fetcher.getVcnById(params.getVcnId());
            UpdateSecurityRuleListParams updateSecurityRuleListParams = new UpdateSecurityRuleListParams();
            UpdateSecurityRuleListParams.IngressRule ingressRule = new UpdateSecurityRuleListParams.IngressRule();
            AddIngressSecurityRuleParams.IngressInfo inboundRule = params.getInboundRule();
            ingressRule.setIcmpOptions(inboundRule.getIcmpOptions());
            ingressRule.setIsStateless(inboundRule.getIsStateless());
            ingressRule.setProtocol(inboundRule.getProtocol());
            ingressRule.setSource(inboundRule.getSource());
            ingressRule.setSourceType(inboundRule.getSourceType());
            ingressRule.setDescription(StrUtil.isBlank(inboundRule.getDescription()) ? null : inboundRule.getDescription());
            if (list.contains(inboundRule.getProtocol())) {
                Tuple2<Integer, Integer> sourcePort = getPortRange(inboundRule.getSourcePort());
                Tuple2<Integer, Integer> destinationPort = getPortRange(inboundRule.getDestinationPort());
                if ("6".equals(inboundRule.getProtocol())) {
                    ingressRule.setTcpSourcePortMin(sourcePort.getFirst());
                    ingressRule.setTcpSourcePortMax(sourcePort.getSecond());
                    ingressRule.setTcpDesPortMin(destinationPort.getFirst());
                    ingressRule.setTcpDesPortMax(destinationPort.getSecond());
                }
                if ("17".equals(inboundRule.getProtocol())) {
                    ingressRule.setUdpSourcePortMin(sourcePort.getFirst());
                    ingressRule.setUdpSourcePortMax(sourcePort.getSecond());
                    ingressRule.setUdpDesPortMin(destinationPort.getFirst());
                    ingressRule.setUdpDesPortMax(destinationPort.getSecond());
                }
            }
            updateSecurityRuleListParams.setIngressRuleList(Collections.singletonList(ingressRule));
            fetcher.updateSecurityRuleList(vcn, updateSecurityRuleListParams);
        } catch (Exception e) {
            log.error("新增入站规则失败", e);
            throw new OciException(-1, "新增入站规则失败：" + e.getMessage());
        }
    }

    @Override
    public void addEgress(AddEgressSecurityRuleParams params) {
        List<String> list = Arrays.asList("6", "17");
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            Vcn vcn = fetcher.getVcnById(params.getVcnId());
            UpdateSecurityRuleListParams updateSecurityRuleListParams = new UpdateSecurityRuleListParams();
            UpdateSecurityRuleListParams.EgressRule egressRule = new UpdateSecurityRuleListParams.EgressRule();
            AddEgressSecurityRuleParams.EgressInfo outboundRule = params.getOutboundRule();
            egressRule.setIcmpOptions(outboundRule.getIcmpOptions());
            egressRule.setDestination(outboundRule.getDestination());
            egressRule.setDestinationType(outboundRule.getDestinationType());
            egressRule.setIsStateless(outboundRule.getIsStateless());
            egressRule.setProtocol(outboundRule.getProtocol());
            egressRule.setDescription(StrUtil.isBlank(outboundRule.getDescription()) ? null : outboundRule.getDescription());
            if (list.contains(outboundRule.getProtocol())) {
                Tuple2<Integer, Integer> sourcePort = getPortRange(outboundRule.getSourcePort());
                Tuple2<Integer, Integer> destinationPort = getPortRange(outboundRule.getDestinationPort());
                if ("6".equals(outboundRule.getProtocol())) {
                    egressRule.setTcpSourcePortMin(sourcePort.getFirst());
                    egressRule.setTcpSourcePortMax(sourcePort.getSecond());
                    egressRule.setTcpDesPortMin(destinationPort.getFirst());
                    egressRule.setTcpDesPortMax(destinationPort.getSecond());
                }
                if ("17".equals(outboundRule.getProtocol())) {
                    egressRule.setUdpSourcePortMin(sourcePort.getFirst());
                    egressRule.setUdpSourcePortMax(sourcePort.getSecond());
                    egressRule.setUdpDesPortMin(destinationPort.getFirst());
                    egressRule.setUdpDesPortMax(destinationPort.getSecond());
                }
            }
            updateSecurityRuleListParams.setEgressRuleList(Collections.singletonList(egressRule));
            fetcher.updateSecurityRuleList(vcn, updateSecurityRuleListParams);
        } catch (Exception e) {
            log.error("新增出站规则失败", e);
            throw new OciException(-1, "新增出站规则失败：" + e.getMessage());
        }
    }

    @Override
    public void remove(RemoveSecurityRuleParams params) {
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        Map<String, IngressSecurityRule> ingressMap = (Map<String, IngressSecurityRule>) customCache.get(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_MAP + params.getVcnId());
        Map<String, EgressSecurityRule> egressMap = (Map<String, EgressSecurityRule>) customCache.get(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_MAP + params.getVcnId());
        if (CollectionUtil.isEmpty(params.getRuleIds())) {
            throw new OciException(-1, "请选择要删除的安全规则");
        }
        if (ingressMap == null || egressMap == null) {
            throw new OciException(-1, "安全规则缓存已过期，请先刷新规则后再删除");
        }
        params.getRuleIds().forEach(ruleId -> {
            if (params.getType().equals(0)) {
                ingressMap.remove(ruleId);
            }
            if (params.getType().equals(1)) {
                egressMap.remove(ruleId);
            }
        });

        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            Vcn vcn = fetcher.getVcnById(params.getVcnId());
            SecurityList securityList = fetcher.listSecurityRule(vcn);
            List<IngressSecurityRule> ingressSecurityRules = new ArrayList<>(ingressMap.values());
            List<EgressSecurityRule> egressSecurityRules = new ArrayList<>(egressMap.values());
            if (params.getType().equals(0)) {
                egressSecurityRules = securityList.getEgressSecurityRules();
            }
            if (params.getType().equals(1)) {
                ingressSecurityRules = securityList.getIngressSecurityRules();
            }
            fetcher.getVirtualNetworkClient().updateSecurityList(UpdateSecurityListRequest.builder()
                    .securityListId(vcn.getDefaultSecurityListId())
                    .updateSecurityListDetails(UpdateSecurityListDetails.builder()
                            .ingressSecurityRules(ingressSecurityRules)
                            .egressSecurityRules(egressSecurityRules)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error("删除安全规则失败", e);
            throw new OciException(-1, "删除安全规则失败");
        }
        customCache.remove(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_PAGE + params.getOciCfgId());
        customCache.remove(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_PAGE + params.getOciCfgId());
        customCache.remove(CacheConstant.PREFIX_INGRESS_SECURITY_RULE_MAP + params.getVcnId());
        customCache.remove(CacheConstant.PREFIX_EGRESS_SECURITY_RULE_MAP + params.getVcnId());
    }

    private Tuple2<Integer, Integer> getPortRange(String portRangeStr) {
        if (StrUtil.isBlank(portRangeStr)) {
            return Tuple2.of(null, null);
        }
        String[] split = portRangeStr.split("-");
        if (split.length == 1) {
            return Tuple2.of(Integer.valueOf(split[0]), Integer.valueOf(split[0]));
        } else if (split.length == 2) {
            return Tuple2.of(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
        } else {
            throw new OciException(-1, "格式有误：" + portRangeStr);
        }
    }
}
