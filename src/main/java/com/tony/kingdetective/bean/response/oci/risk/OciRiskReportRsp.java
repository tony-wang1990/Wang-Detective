package com.tony.kingdetective.bean.response.oci.risk;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OciRiskReportRsp {
    private LocalDateTime generatedAt;
    private Summary summary;
    private List<ConfigRisk> configs;
    private List<RiskItem> risks;

    @Data
    @Builder
    public static class Summary {
        private Integer configCount;
        private Integer scannedConfigCount;
        private Integer instanceCount;
        private Integer runningInstanceCount;
        private Integer stoppedInstanceCount;
        private Integer armInstanceCount;
        private Double armOcpus;
        private Double armMemoryGb;
        private Long bootVolumeGb;
        private Integer highRiskCount;
        private Integer warnRiskCount;
        private Integer errorConfigCount;
    }

    @Data
    @Builder
    public static class ConfigRisk {
        private String configId;
        private String configName;
        private String region;
        private Integer instanceCount;
        private Integer runningInstanceCount;
        private Double armOcpus;
        private Double armMemoryGb;
        private Long bootVolumeGb;
        private Integer publicIngressRuleCount;
        private Integer highRiskPortRuleCount;
        private List<ResourceRisk> resourceRisks;
        private List<PortExposure> portExposures;
        private String status;
        private String message;
    }

    @Data
    @Builder
    public static class RiskItem {
        private String level;
        private String category;
        private String title;
        private String message;
        private String configId;
        private String configName;
        private String region;
        private List<PortExposure> portExposures;
    }

    @Data
    @Builder
    public static class PortExposure {
        private String vcnName;
        private String source;
        private String protocol;
        private String portRange;
        private String description;
        private Boolean highRisk;
        private String recommendation;
    }

    @Data
    @Builder
    public static class ResourceRisk {
        private String level;
        private String category;
        private String title;
        private String message;
        private String recommendation;
        private Integer count;
    }
}
