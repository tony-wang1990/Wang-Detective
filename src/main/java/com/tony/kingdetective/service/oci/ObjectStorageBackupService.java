package com.tony.kingdetective.service.oci;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageBackupParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageListObjectsParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageObjectParams;
import com.tony.kingdetective.bean.response.oci.objectstorage.ObjectStorageBackupRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.ISysService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ObjectStorageBackupService {

    private final ISysService sysService;

    @Value("${king-detective.app-dir:${KING_DETECTIVE_APP_DIR:/app/king-detective}}")
    private String appDir;

    public ObjectStorageBackupService(ISysService sysService) {
        this.sysService = sysService;
    }

    public List<ObjectStorageBackupRsp.BucketInfo> listBuckets(String ociCfgId) {
        try (OracleInstanceFetcher fetcher = createFetcher(ociCfgId)) {
            ObjectStorageClient client = createClient(fetcher);
            try {
            String namespace = getNamespace(client);
            List<BucketSummary> buckets = client.listBuckets(ListBucketsRequest.builder()
                    .namespaceName(namespace)
                    .compartmentId(fetcher.getCompartmentId())
                    .build()).getItems();

            List<ObjectStorageBackupRsp.BucketInfo> result = new ArrayList<>();
            if (buckets != null) {
                for (BucketSummary bucket : buckets) {
                    result.add(ObjectStorageBackupRsp.BucketInfo.builder()
                            .name(bucket.getName())
                            .namespaceName(StrUtil.blankToDefault(bucket.getNamespace(), namespace))
                            .compartmentId(bucket.getCompartmentId())
                            .storageTier("Standard")
                            .timeCreated(toLocalDateTime(bucket.getTimeCreated()))
                            .build());
                }
            }
            return result;
            } finally {
                client.close();
            }
        } catch (Exception e) {
            log.error("Object Storage bucket list failed", e);
            throw new OciException(-1, "读取对象存储桶失败: " + e.getMessage());
        }
    }

    public ObjectStorageBackupRsp.ObjectList listObjects(ObjectStorageListObjectsParams params) {
        try (OracleInstanceFetcher fetcher = createFetcher(params.getOciCfgId())) {
            ObjectStorageClient client = createClient(fetcher);
            try {
            String namespace = getNamespace(client);
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(params.getBucketName())
                    .prefix(StrUtil.blankToDefault(params.getPrefix(), "wang-detective/backups"))
                    .limit(limit(params.getLimit()))
                    .build();
            List<ObjectSummary> objects = client.listObjects(request).getListObjects().getObjects();
            List<ObjectStorageBackupRsp.ObjectInfo> objectInfos = new ArrayList<>();
            if (objects != null) {
                for (ObjectSummary object : objects) {
                    objectInfos.add(ObjectStorageBackupRsp.ObjectInfo.builder()
                            .name(object.getName())
                            .sizeBytes(object.getSize())
                            .md5(object.getMd5())
                            .timeCreated(toLocalDateTime(object.getTimeCreated()))
                            .build());
                }
            }
            return ObjectStorageBackupRsp.ObjectList.builder()
                    .namespaceName(namespace)
                    .bucketName(params.getBucketName())
                    .prefix(params.getPrefix())
                    .objects(objectInfos)
                    .build();
            } finally {
                client.close();
            }
        } catch (Exception e) {
            log.error("Object Storage object list failed", e);
            throw new OciException(-1, "读取对象存储文件失败: " + e.getMessage());
        }
    }

    public ObjectStorageBackupRsp createBackup(ObjectStorageBackupParams params) {
        File backupFile = createLocalArchive(Boolean.TRUE.equals(params.getIncludeLogs()));
        String namespace = null;
        String objectName = null;

        if (Boolean.TRUE.equals(params.getUploadToObjectStorage())) {
            if (StrUtil.isBlank(params.getOciCfgId())) {
                throw new OciException(-1, "上传对象存储时必须选择 OCI 配置");
            }
            if (StrUtil.isBlank(params.getBucketName())) {
                throw new OciException(-1, "上传对象存储时必须选择 Bucket");
            }
            try (OracleInstanceFetcher fetcher = createFetcher(params.getOciCfgId());
                 InputStream inputStream = new FileInputStream(backupFile)) {
                ObjectStorageClient client = createClient(fetcher);
                try {
                namespace = getNamespace(client);
                objectName = normalizeObjectName(params.getPrefix(), backupFile.getName());
                client.putObject(PutObjectRequest.builder()
                        .namespaceName(namespace)
                        .bucketName(params.getBucketName())
                        .objectName(objectName)
                        .contentLength(backupFile.length())
                        .putObjectBody(inputStream)
                        .build());
                } finally {
                    client.close();
                }
            } catch (Exception e) {
                log.error("Object Storage backup upload failed", e);
                throw new OciException(-1, "备份上传对象存储失败: " + e.getMessage());
            }
        }

        return ObjectStorageBackupRsp.builder()
                .localPath(backupFile.getAbsolutePath())
                .namespaceName(namespace)
                .bucketName(params.getBucketName())
                .objectName(objectName)
                .sizeBytes(backupFile.length())
                .md5(md5Hex(backupFile))
                .createTime(LocalDateTime.now())
                .build();
    }

    public void deleteObject(ObjectStorageObjectParams params) {
        try (OracleInstanceFetcher fetcher = createFetcher(params.getOciCfgId())) {
            ObjectStorageClient client = createClient(fetcher);
            try {
            String namespace = getNamespace(client);
            client.deleteObject(DeleteObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(params.getBucketName())
                    .objectName(params.getObjectName())
                    .build());
            } finally {
                client.close();
            }
        } catch (Exception e) {
            log.error("Object Storage object delete failed", e);
            throw new OciException(-1, "删除对象存储文件失败: " + e.getMessage());
        }
    }

    public List<String> listLocalBackups(int limit) {
        return listLocalBackupInfos(limit).stream()
                .map(file -> file.getName() + " (" + FileUtil.readableFileSize(file.getSizeBytes()) + ")")
                .toList();
    }

    public List<ObjectStorageBackupRsp.LocalBackupInfo> listLocalBackupInfos(int limit) {
        File dir = FileUtil.mkdir(new File(appDir, "backups").getAbsolutePath());
        File[] files = dir.listFiles((file) -> file.isFile() && file.getName().endsWith(".tar.gz"));
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files)
                .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(file -> ObjectStorageBackupRsp.LocalBackupInfo.builder()
                        .name(file.getName())
                        .path(file.getAbsolutePath())
                        .sizeBytes(file.length())
                        .modifiedTime(LocalDateTime.ofInstant(new Date(file.lastModified()).toInstant(), ZoneId.systemDefault()))
                        .build())
                .toList();
    }

    public ObjectStorageBackupRsp.RestorePlan restorePlan(String backupName) {
        File file = safeLocalBackup(backupName);
        return ObjectStorageBackupRsp.RestorePlan.builder()
                .backupName(file.getName())
                .backupPath(file.getAbsolutePath())
                .sizeBytes(file.length())
                .command("cd " + appDir + " && RESTORE_CONFIRM=YES bash scripts/restore.sh " + shellQuote(file.getAbsolutePath()))
                .warnings(List.of(
                        "恢复会先停止服务并移动当前 data/keys/scripts 等目录，请确认已经有最新备份。",
                        "建议先在低峰期执行，恢复期间 Web 面板会短暂不可用。",
                        "Object Storage 归档对象需要先下载到本地 backups/ 目录后再执行恢复。"
                ))
                .steps(List.of(
                        "1. 在 Web 端或命令行确认备份文件完整。",
                        "2. 复制恢复命令到服务器 SSH 执行。",
                        "3. restore.sh 会把旧文件移动到 .restore-archive-* 后再恢复。",
                        "4. 恢复后执行 bash scripts/server-smoke-test.sh 验证服务。"
                ))
                .build();
    }

    public ObjectStorageBackupRsp.SchedulePlan schedulePlan(String cronExpression) {
        String cron = StrUtil.blankToDefault(cronExpression, "0 3 * * *").trim();
        return ObjectStorageBackupRsp.SchedulePlan.builder()
                .cronExpression(cron)
                .command("cd " + appDir + " && BACKUP_CRON='" + cron.replace("'", "'\"'\"'") + "' bash scripts/setup-backup-cron.sh")
                .steps(List.of(
                        "1. 确认 scripts/setup-backup-cron.sh 存在且可执行。",
                        "2. 执行命令写入系统 crontab。",
                        "3. 执行 crontab -l 确认计划任务。",
                        "4. 首次计划执行后在 backups/ 目录确认备份包。"
                ))
                .objectStoragePolicy(List.of(
                        "Object Storage 建议使用前缀 wang-detective/backups/yyyyMMdd/ 分日归档，保留最近 7 天本地备份。",
                        "重要升级前手动创建一次备份并上传到 Standard 桶；稳定后可把 30 天前对象转 Archive 或删除。",
                        "恢复时先从 Object Storage 下载对象到 /app/king-detective/backups，再执行恢复命令。"
                ))
                .build();
    }

    private File createLocalArchive(boolean includeLogs) {
        try {
            File backupDir = FileUtil.mkdir(new File(appDir, "backups").getAbsolutePath());
            File script = new File(appDir, "scripts/backup.sh");
            if (!script.exists() || !script.isFile()) {
                throw new OciException(-1, "备份脚本不存在: " + script.getAbsolutePath());
            }
            long startedAt = System.currentTimeMillis();
            ProcessBuilder processBuilder = new ProcessBuilder("bash", script.getAbsolutePath());
            processBuilder.directory(new File(appDir));
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("APP_DIR", appDir);
            processBuilder.environment().put("BACKUP_DIR", backupDir.getAbsolutePath());
            processBuilder.environment().put("INCLUDE_LOGS", includeLogs ? "1" : "0");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw new OciException(-1, "备份脚本执行超时");
            }
            if (process.exitValue() != 0) {
                throw new OciException(-1, "备份脚本执行失败: " + StrUtil.sub(output, 0, 800));
            }

            File[] files = backupDir.listFiles(file -> file.isFile()
                    && file.getName().endsWith(".tar.gz")
                    && file.lastModified() >= startedAt - 1000);
            if (files == null || files.length == 0) {
                throw new OciException(-1, "备份脚本已执行，但未找到新生成的 tar.gz 备份包");
            }
            return Arrays.stream(files)
                    .max((a, b) -> Long.compare(a.lastModified(), b.lastModified()))
                    .orElseThrow(() -> new OciException(-1, "未找到新生成的备份包"));
        } catch (Exception e) {
            log.error("Create local backup archive failed", e);
            throw new OciException(-1, "创建本地备份包失败: " + e.getMessage());
        }
    }

    private OracleInstanceFetcher createFetcher(String ociCfgId) {
        SysUserDTO user = sysService.getOciUser(ociCfgId);
        return new OracleInstanceFetcher(user);
    }

    private ObjectStorageClient createClient(OracleInstanceFetcher fetcher) {
        return ObjectStorageClient.builder().build(fetcher.getAuthenticationDetailsProvider());
    }

    private String getNamespace(ObjectStorageClient client) {
        return client.getNamespace(GetNamespaceRequest.builder().build()).getValue();
    }

    private Integer limit(Integer value) {
        if (value == null) {
            return 100;
        }
        return Math.max(1, Math.min(value, 1000));
    }

    private String normalizeObjectName(String prefix, String fileName) {
        String normalizedPrefix = StrUtil.blankToDefault(prefix, "wang-detective/backups").trim();
        normalizedPrefix = normalizedPrefix.replace("\\", "/");
        while (normalizedPrefix.startsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(1);
        }
        while (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        return normalizedPrefix + "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "/" + fileName;
    }

    private File safeLocalBackup(String backupName) {
        if (StrUtil.isBlank(backupName)) {
            throw new OciException(-1, "请选择本地备份文件");
        }
        String normalized = backupName.replace("\\", "/");
        if (normalized.contains("/") || normalized.contains("..")) {
            throw new OciException(-1, "备份文件名不合法");
        }
        if (!normalized.endsWith(".tar.gz")) {
            throw new OciException(-1, "恢复仅支持 scripts/backup.sh 生成的 .tar.gz 备份包");
        }
        File file = new File(new File(appDir, "backups"), normalized);
        if (!file.exists() || !file.isFile()) {
            throw new OciException(-1, "备份文件不存在: " + backupName);
        }
        return file;
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private String md5Hex(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                String item = Integer.toHexString(0xff & b);
                if (item.length() == 1) {
                    hex.append('0');
                }
                hex.append(item);
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
