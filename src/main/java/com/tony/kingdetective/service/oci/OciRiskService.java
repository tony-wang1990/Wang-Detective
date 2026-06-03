package com.tony.kingdetective.service.oci;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.oracle.bmc.core.model.BootVolume;
import com.oracle.bmc.core.model.IngressSecurityRule;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.model.PortRange;
import com.oracle.bmc.core.model.SecurityList;
import com.oracle.bmc.core.model.Vcn;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.response.oci.risk.OciRiskReportRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.ISysService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OciRiskService {

    private static final List<Integer> HIGH_RISK_PORTS = Arrays.asList(22, 3389, 3306, 5432, 6379, 9200, 5601, 27017, 9527);

    private final ISysService sysService;

    public OciRiskService(ISysService sysService) {
        this.sysService = sysService;
    }

    public OciRiskReportRsp report(Integer maxConfigs) {
        int scanLimit = maxConfigs == null ? 8 : Math.max(1, Math.min(maxConfigs, 50));
        List<SysUserDTO> allConfigs = sysService.list();
        List<OciRiskReportRsp.ConfigRisk> configs = new ArrayList<>();
        List<OciRiskReportRsp.RiskItem> risks = new ArrayList<>();

        SummaryAccumulator summary = new SummaryAccumulator();
        summary.configCount = allConfigs.size();

        for (SysUserDTO config : allConfigs.stream().limit(scanLimit).toList()) {
            summary.scannedConfigCount++;
            OciRiskReportRsp.ConfigRisk configRisk = scanOneConfig(config, risks, summary);
            configs.add(configRisk);
        }

        summary.highRiskCount = (int) risks.stream().filter(risk -> "HIGH".equals(risk.getLevel())).count();
        summary.warnRiskCount = (int) risks.stream().filter(risk -> "WARN".equals(risk.getLevel())).count();

        return OciRiskReportRsp.builder()
                .generatedAt(java.time.LocalDateTime.now())
                .summary(OciRiskReportRsp.Summary.builder()
                        .configCount(summary.configCount)
                        .scannedConfigCount(summary.scannedConfigCount)
                        .instanceCount(summary.instanceCount)
                        .runningInstanceCount(summary.runningInstanceCount)
                        .stoppedInstanceCount(summary.stoppedInstanceCount)
                        .armInstanceCount(summary.armInstanceCount)
                        .armOcpus(round(summary.armOcpus))
                        .armMemoryGb(round(summary.armMemoryGb))
                        .bootVolumeGb(summary.bootVolumeGb)
                        .highRiskCount(summary.highRiskCount)
                        .warnRiskCount(summary.warnRiskCount)
                        .errorConfigCount(summary.errorConfigCount)
                        .build())
                .configs(configs)
                .risks(risks)
                .build();
    }

    private OciRiskReportRsp.ConfigRisk scanOneConfig(SysUserDTO config,
                                                      List<OciRiskReportRsp.RiskItem> risks,
                                                      SummaryAccumulator summary) {
        String configId = config.getOciCfg() == null ? null : config.getOciCfg().getId();
        String configName = StrUtil.blankToDefault(config.getUsername(), configId);
        String region = config.getOciCfg() == null ? null : config.getOciCfg().getRegion();

        int instanceCount = 0;
        int runningCount = 0;
        int stoppedCount = 0;
        double armOcpus = 0;
        double armMemoryGb = 0;
        long bootVolumeGb = 0;
        int publicIngressRuleCount = 0;
        int highRiskPortRuleCount = 0;
        List<OciRiskReportRsp.ResourceRisk> resourceRisks = new ArrayList<>();
        List<OciRiskReportRsp.PortExposure> portExposures = new ArrayList<>();

        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(config)) {
            List<Instance> instances = fetcher.listInstances();
            instanceCount = instances.size();
            summary.instanceCount += instanceCount;

            for (Instance instance : instances) {
                if (instance.getLifecycleState() == Instance.LifecycleState.Running) {
                    runningCount++;
                    summary.runningInstanceCount++;
                } else if (instance.getLifecycleState() == Instance.LifecycleState.Stopped) {
                    stoppedCount++;
                    summary.stoppedInstanceCount++;
                }

                String shape = instance.getShape() == null ? "" : instance.getShape().toLowerCase();
                if (shape.contains("a1.flex")) {
                    summary.armInstanceCount++;
                    double ocpus = instance.getShapeConfig() == null ? 0 : toDouble(instance.getShapeConfig().getOcpus());
                    double memory = instance.getShapeConfig() == null ? 0 : toDouble(instance.getShapeConfig().getMemoryInGBs());
                    armOcpus += ocpus;
                    armMemoryGb += memory;
                    summary.armOcpus += ocpus;
                    summary.armMemoryGb += memory;
                }
            }

            List<BootVolume> bootVolumes = safeListBootVolumes(fetcher);
            for (BootVolume bootVolume : bootVolumes) {
                Long size = bootVolume.getSizeInGBs();
                if (size != null) {
                    bootVolumeGb += size;
                    summary.bootVolumeGb += size;
                }
            }

            List<Vcn> vcns = fetcher.listVcn();
            for (Vcn vcn : vcns) {
                SecurityList securityList = fetcher.listSecurityRule(vcn);
                if (securityList == null || CollectionUtil.isEmpty(securityList.getIngressSecurityRules())) {
                    continue;
                }
                for (IngressSecurityRule rule : securityList.getIngressSecurityRules()) {
                    if (!isPublic(rule)) {
                        continue;
                    }
                    publicIngressRuleCount++;
                    if (isHighRiskPublicPort(rule)) {
                        highRiskPortRuleCount++;
                    }
                    portExposures.add(buildPortExposure(vcn, rule));
                }
            }

            addThresholdRisks(risks, configId, configName, region, armOcpus, armMemoryGb, bootVolumeGb,
                    publicIngressRuleCount, highRiskPortRuleCount, portExposures);
            buildResourceRisks(resourceRisks, instanceCount, runningCount, stoppedCount, armOcpus, armMemoryGb,
                    bootVolumeGb, publicIngressRuleCount, highRiskPortRuleCount);

            return OciRiskReportRsp.ConfigRisk.builder()
                    .configId(configId)
                    .configName(configName)
                    .region(region)
                    .instanceCount(instanceCount)
                    .runningInstanceCount(runningCount)
                    .armOcpus(round(armOcpus))
                    .armMemoryGb(round(armMemoryGb))
                    .bootVolumeGb(bootVolumeGb)
                    .publicIngressRuleCount(publicIngressRuleCount)
                    .highRiskPortRuleCount(highRiskPortRuleCount)
                    .resourceRisks(resourceRisks)
                    .portExposures(portExposures)
                    .status(configStatus(resourceRisks))
                    .message("扫描完成")
                    .build();
        } catch (Exception e) {
            log.warn("OCI risk scan failed for {}", configName, e);
            summary.errorConfigCount++;
            resourceRisks.add(resourceRisk("ERROR", "CONFIG", "配置扫描失败",
                    "无法读取此配置的实时 OCI 数据: " + e.getMessage(),
                    "检查 API 私钥、租户/用户 OCID、区域和网络连通性。", 1));
            risks.add(risk("ERROR", "CONFIG", "配置扫描失败",
                    "配置 " + configName + " 扫描失败: " + e.getMessage(), configId, configName, region));
            return OciRiskReportRsp.ConfigRisk.builder()
                    .configId(configId)
                    .configName(configName)
                    .region(region)
                    .instanceCount(instanceCount)
                    .runningInstanceCount(runningCount)
                    .armOcpus(round(armOcpus))
                    .armMemoryGb(round(armMemoryGb))
                    .bootVolumeGb(bootVolumeGb)
                    .publicIngressRuleCount(publicIngressRuleCount)
                    .highRiskPortRuleCount(highRiskPortRuleCount)
                    .resourceRisks(resourceRisks)
                    .portExposures(portExposures)
                    .status("ERROR")
                    .message(e.getMessage())
                    .build();
        }
    }

    private void buildResourceRisks(List<OciRiskReportRsp.ResourceRisk> resourceRisks,
                                    int instanceCount,
                                    int runningCount,
                                    int stoppedCount,
                                    double armOcpus,
                                    double armMemoryGb,
                                    long bootVolumeGb,
                                    int publicIngressRuleCount,
                                    int highRiskPortRuleCount) {
        if (instanceCount == 0) {
            resourceRisks.add(resourceRisk("WARN", "COMPUTE", "配置下没有实例",
                    "当前配置没有扫描到 Compute 实例，可能是空配置，也可能是权限/区域不匹配。",
                    "确认区域是否正确；如果是备用配置可忽略。", 0));
        }
        if (stoppedCount > 0) {
            resourceRisks.add(resourceRisk("WARN", "COMPUTE", "存在已停止实例",
                    "检测到 " + stoppedCount + " 台已停止实例，可能仍保留引导卷并产生存储占用。",
                    "确认是否需要保留；长期不用建议备份后终止并清理闲置卷。", stoppedCount));
        }
        if (runningCount > 0 && armOcpus == 0 && armMemoryGb == 0) {
            resourceRisks.add(resourceRisk("OK", "COMPUTE", "非 ARM 实例运行中",
                    "当前运行实例不是 ARM A1 Flex，免费额度判断以实际 OCI 账单为准。",
                    "如目标是 Always Free，请核对 Shape 和区域可用性。", runningCount));
        }
        if (armOcpus > 4 || armMemoryGb > 24) {
            resourceRisks.add(resourceRisk("WARN", "QUOTA", "ARM 免费资源可能超额",
                    "ARM 合计 " + round(armOcpus) + " OCPU / " + round(armMemoryGb) + " GB。",
                    "Always Free 常见上限为 4 OCPU / 24 GB，请确认是否拆分或释放。", 1));
        }
        if (bootVolumeGb > 200) {
            resourceRisks.add(resourceRisk("WARN", "STORAGE", "引导卷容量偏高",
                    "引导卷合计 " + bootVolumeGb + " GB，长期闲置卷可能带来额外成本。",
                    "在备份后清理无用 boot volume，必要时降低新实例默认磁盘。", 1));
        }
        if (highRiskPortRuleCount > 0) {
            resourceRisks.add(resourceRisk("HIGH", "NETWORK", "公网高危入站规则",
                    "检测到 " + highRiskPortRuleCount + " 条公网高危入站规则。",
                    "优先在配置列表的规则明细中删除或收敛到固定管理 IP/CIDR。", highRiskPortRuleCount));
        } else if (publicIngressRuleCount > 0) {
            resourceRisks.add(resourceRisk("WARN", "NETWORK", "公网入站规则较多",
                    "检测到 " + publicIngressRuleCount + " 条公网入站规则。",
                    "确认业务确实需要公网暴露；管理端口建议收敛来源 CIDR。", publicIngressRuleCount));
        }
    }

    private OciRiskReportRsp.ResourceRisk resourceRisk(String level,
                                                       String category,
                                                       String title,
                                                       String message,
                                                       String recommendation,
                                                       int count) {
        return OciRiskReportRsp.ResourceRisk.builder()
                .level(level)
                .category(category)
                .title(title)
                .message(message)
                .recommendation(recommendation)
                .count(count)
                .build();
    }

    private String configStatus(List<OciRiskReportRsp.ResourceRisk> resourceRisks) {
        if (resourceRisks.stream().anyMatch(item -> "ERROR".equals(item.getLevel()) || "HIGH".equals(item.getLevel()))) {
            return "HIGH";
        }
        if (resourceRisks.stream().anyMatch(item -> "WARN".equals(item.getLevel()))) {
            return "WARN";
        }
        return "OK";
    }

    private List<BootVolume> safeListBootVolumes(OracleInstanceFetcher fetcher) {
        try {
            List<BootVolume> bootVolumes = fetcher.listBootVolume();
            return bootVolumes == null ? List.of() : bootVolumes;
        } catch (Exception e) {
            log.warn("Boot volume risk scan skipped: {}", e.getMessage());
            return List.of();
        }
    }

    private void addThresholdRisks(List<OciRiskReportRsp.RiskItem> risks,
                                   String configId,
                                   String configName,
                                   String region,
                                   double armOcpus,
                                   double armMemoryGb,
                                   long bootVolumeGb,
                                   int publicIngressRuleCount,
                                   int highRiskPortRuleCount,
                                   List<OciRiskReportRsp.PortExposure> portExposures) {
        if (armOcpus > 4 || armMemoryGb > 24) {
            risks.add(risk("WARN", "QUOTA", "ARM 免费资源可能超额",
                    "ARM 当前合计 " + round(armOcpus) + " OCPU / " + round(armMemoryGb) + " GB，请确认是否仍在免费额度内。",
                    configId, configName, region));
        }
        if (bootVolumeGb > 200) {
            risks.add(risk("WARN", "STORAGE", "引导卷容量偏高",
                    "当前引导卷合计 " + bootVolumeGb + " GB，建议清理闲置引导卷或确认计费。",
                    configId, configName, region));
        }
        if (highRiskPortRuleCount > 0) {
            String ports = highRiskPortSummary(portExposures);
            risks.add(risk("HIGH", "NETWORK", "公网高危端口开放",
                    "检测到 " + highRiskPortRuleCount + " 条面向公网的高危端口规则" + ports + "，请优先限制来源 IP 或关闭非必要端口。",
                    configId, configName, region, portExposures.stream().filter(item -> Boolean.TRUE.equals(item.getHighRisk())).limit(10).toList()));
        } else if (publicIngressRuleCount > 0) {
            risks.add(risk("WARN", "NETWORK", "公网入站规则较开放",
                    "检测到 " + publicIngressRuleCount + " 条面向公网的入站规则，请确认来源网段是否必要。",
                    configId, configName, region, portExposures.stream().limit(10).toList()));
        }
    }

    private OciRiskReportRsp.RiskItem risk(String level,
                                           String category,
                                           String title,
                                           String message,
                                           String configId,
                                           String configName,
                                           String region) {
        return risk(level, category, title, message, configId, configName, region, List.of());
    }

    private OciRiskReportRsp.RiskItem risk(String level,
                                           String category,
                                           String title,
                                           String message,
                                           String configId,
                                           String configName,
                                           String region,
                                           List<OciRiskReportRsp.PortExposure> portExposures) {
        return OciRiskReportRsp.RiskItem.builder()
                .level(level)
                .category(category)
                .title(title)
                .message(message)
                .configId(configId)
                .configName(configName)
                .region(region)
                .portExposures(portExposures)
                .build();
    }

    private boolean isPublic(IngressSecurityRule rule) {
        return "0.0.0.0/0".equals(rule.getSource()) || "::/0".equals(rule.getSource());
    }

    private boolean isHighRiskPublicPort(IngressSecurityRule rule) {
        PortRange range = null;
        if ("6".equals(rule.getProtocol()) && rule.getTcpOptions() != null) {
            range = rule.getTcpOptions().getDestinationPortRange();
        } else if ("17".equals(rule.getProtocol()) && rule.getUdpOptions() != null) {
            range = rule.getUdpOptions().getDestinationPortRange();
        } else if ("all".equalsIgnoreCase(rule.getProtocol())) {
            return true;
        }
        if (range == null) {
            return true;
        }
        Integer min = range.getMin();
        Integer max = range.getMax();
        if (min == null || max == null) {
            return true;
        }
        for (Integer port : HIGH_RISK_PORTS) {
            if (port >= min && port <= max) {
                return true;
            }
        }
        return false;
    }

    private OciRiskReportRsp.PortExposure buildPortExposure(Vcn vcn, IngressSecurityRule rule) {
        boolean highRisk = isHighRiskPublicPort(rule);
        return OciRiskReportRsp.PortExposure.builder()
                .vcnName(StrUtil.blankToDefault(vcn.getDisplayName(), vcn.getId()))
                .source(StrUtil.blankToDefault(rule.getSource(), "-"))
                .protocol(protocolName(rule.getProtocol()))
                .portRange(portRangeText(rule))
                .description(StrUtil.blankToDefault(rule.getDescription(), "-"))
                .highRisk(highRisk)
                .recommendation(recommendationFor(rule, highRisk))
                .build();
    }

    private String protocolName(String protocol) {
        if ("6".equals(protocol)) {
            return "TCP";
        }
        if ("17".equals(protocol)) {
            return "UDP";
        }
        if ("1".equals(protocol)) {
            return "ICMP";
        }
        if ("all".equalsIgnoreCase(protocol)) {
            return "全部协议";
        }
        return StrUtil.blankToDefault(protocol, "未知");
    }

    private String portRangeText(IngressSecurityRule rule) {
        if ("all".equalsIgnoreCase(rule.getProtocol())) {
            return "全部端口";
        }
        PortRange range = null;
        if ("6".equals(rule.getProtocol()) && rule.getTcpOptions() != null) {
            range = rule.getTcpOptions().getDestinationPortRange();
        } else if ("17".equals(rule.getProtocol()) && rule.getUdpOptions() != null) {
            range = rule.getUdpOptions().getDestinationPortRange();
        } else if ("1".equals(rule.getProtocol())) {
            if (rule.getIcmpOptions() == null) {
                return "全部 ICMP";
            }
            Integer type = rule.getIcmpOptions().getType();
            Integer code = rule.getIcmpOptions().getCode();
            return code == null ? "ICMP type " + type : "ICMP type " + type + ", code " + code;
        }
        if (range == null || range.getMin() == null || range.getMax() == null) {
            return "全部端口";
        }
        return range.getMin().equals(range.getMax()) ? String.valueOf(range.getMin()) : range.getMin() + "-" + range.getMax();
    }

    private String recommendationFor(IngressSecurityRule rule, boolean highRisk) {
        if ("all".equalsIgnoreCase(rule.getProtocol())) {
            return "规则开放全部协议，建议拆分为必要端口并限制来源 CIDR。";
        }
        if (highRisk) {
            return "建议只允许固定管理 IP/CIDR 访问，SSH/RDP/数据库端口不要直接面向公网。";
        }
        return "确认业务确实需要公网访问；若只用于管理，请收敛到固定来源 IP。";
    }

    private String highRiskPortSummary(List<OciRiskReportRsp.PortExposure> portExposures) {
        Set<String> ports = portExposures.stream()
                .filter(item -> Boolean.TRUE.equals(item.getHighRisk()))
                .map(OciRiskReportRsp.PortExposure::getPortRange)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ports.isEmpty()) {
            return "";
        }
        return "，涉及 " + ports.stream().limit(6).collect(Collectors.joining("、"));
    }

    private double toDouble(Number value) {
        return value == null ? 0 : value.doubleValue();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class SummaryAccumulator {
        int configCount;
        int scannedConfigCount;
        int instanceCount;
        int runningInstanceCount;
        int stoppedInstanceCount;
        int armInstanceCount;
        double armOcpus;
        double armMemoryGb;
        long bootVolumeGb;
        int highRiskCount;
        int warnRiskCount;
        int errorConfigCount;
    }
}
