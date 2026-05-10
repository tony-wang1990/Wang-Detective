package com.tony.kingdetective.bean.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Runtime diagnostics for deployment and security preflight checks.
 */
@Data
@Builder
public class SystemDiagnostics {
    private String status;
    private String version;
    private String javaVersion;
    private String osName;
    private Long uptimeSeconds;
    private Long usedMemoryBytes;
    private Long maxMemoryBytes;
    private Long freeDiskBytes;
    private String databasePath;
    private Long databaseBytes;
    private String keyDirPath;
    private String logFilePath;
    private List<CheckItem> checks;

    @Data
    @Builder
    public static class CheckItem {
        private String name;
        private String status;
        private String message;
    }
}
