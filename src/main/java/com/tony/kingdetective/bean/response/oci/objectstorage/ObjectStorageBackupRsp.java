package com.tony.kingdetective.bean.response.oci.objectstorage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ObjectStorageBackupRsp {
    private String localPath;
    private String namespaceName;
    private String bucketName;
    private String objectName;
    private Long sizeBytes;
    private String md5;
    private LocalDateTime createTime;

    @Data
    @Builder
    public static class BucketInfo {
        private String name;
        private String namespaceName;
        private String compartmentId;
        private String storageTier;
        private LocalDateTime timeCreated;
    }

    @Data
    @Builder
    public static class ObjectInfo {
        private String name;
        private Long sizeBytes;
        private String md5;
        private LocalDateTime timeCreated;
    }

    @Data
    @Builder
    public static class ObjectList {
        private String namespaceName;
        private String bucketName;
        private String prefix;
        private List<ObjectInfo> objects;
    }

    @Data
    @Builder
    public static class LocalBackupInfo {
        private String name;
        private String path;
        private Long sizeBytes;
        private LocalDateTime modifiedTime;
    }

    @Data
    @Builder
    public static class RestorePlan {
        private String backupName;
        private String backupPath;
        private Long sizeBytes;
        private String command;
        private List<String> steps;
        private List<String> warnings;
    }

    @Data
    @Builder
    public static class SchedulePlan {
        private String cronExpression;
        private String command;
        private List<String> steps;
        private List<String> objectStoragePolicy;
    }
}
