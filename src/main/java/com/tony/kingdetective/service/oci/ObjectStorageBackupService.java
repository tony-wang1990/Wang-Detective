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
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageRestoreParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageScheduleParams;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
                        "Web 一键恢复会通过 watcher 执行，期间主服务会短暂重启。",
                        "恢复会移动当前 data/keys/scripts 等目录，请确认已经有最新备份。",
                        "Object Storage 归档对象需要先下载到本地 backups/ 目录后再执行恢复。"
                ))
                .steps(List.of(
                        "1. 在左侧选择本地备份包。",
                        "2. 输入 RESTORE 后点击 Web 一键恢复。",
                        "3. watcher 会调用 restore.sh，把旧文件移动到 .restore-archive-* 后再恢复。",
                        "4. 恢复后等待健康检查恢复为 UP；右侧命令只作为 SSH 兜底方案。"
                ))
                .build();
    }

    public ObjectStorageBackupRsp.SchedulePlan schedulePlan(String cronExpression) {
        String cron = StrUtil.blankToDefault(cronExpression, "0 3 * * *").trim();
        return ObjectStorageBackupRsp.SchedulePlan.builder()
                .cronExpression(cron)
                .command("cd " + appDir + " && BACKUP_CRON='" + cron.replace("'", "'\"'\"'") + "' bash scripts/setup-backup-cron.sh")
                .steps(List.of(
                        "1. 在 Web 端确认 Cron 表达式。",
                        "2. 点击一键安装定时备份，任务会交给 watcher 持久化。",
                        "3. watcher 到点会调用 scripts/backup.sh 创建本地备份包。",
                        "4. 右侧命令只作为 SSH 兜底方案。"
                ))
                .objectStoragePolicy(List.of(
                        "Object Storage 上传已经集成在一键备份里，选择配置和 Bucket 后即可同步归档。",
                        "建议使用 wang-detective/backups/yyyyMMdd/ 前缀分日归档，保留最近 7 天本地备份。",
                        "恢复云端归档时先把对象下载到 /app/king-detective/backups，再使用 Web 一键恢复。"
                ))
                .build();
    }

    public ObjectStorageBackupRsp.ActionResult dispatchRestore(ObjectStorageRestoreParams params) {
        if (!"RESTORE".equals(params.getConfirm())) {
            throw new OciException(-1, "恢复属于高危操作，请输入 RESTORE 确认");
        }
        File file = safeLocalBackup(params.getBackupName());
        Path actionFile = writeWatcherAction(Map.of(
                "ACTION", "restore",
                "BACKUP_NAME", file.getName(),
                "CREATED_AT", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
        return ObjectStorageBackupRsp.ActionResult.builder()
                .action("restore")
                .status("QUEUED")
                .message("恢复任务已交给 watcher 执行，服务会短暂重启，请稍后查看 Docker 日志和健康检查。")
                .watcherActionFile(actionFile.toString())
                .createTime(LocalDateTime.now())
                .build();
    }

    public ObjectStorageBackupRsp.ActionResult dispatchSchedule(ObjectStorageScheduleParams params) {
        boolean enabled = Boolean.TRUE.equals(params.getEnabled());
        String cron = normalizeCron(params.getCronExpression());
        Path actionFile = writeWatcherAction(enabled
                ? Map.of("ACTION", "schedule_install", "CRON_SCHEDULE", cron, "CREATED_AT", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                : Map.of("ACTION", "schedule_remove", "CREATED_AT", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        return ObjectStorageBackupRsp.ActionResult.builder()
                .action(enabled ? "schedule_install" : "schedule_remove")
                .status("QUEUED")
                .message(enabled ? "定时备份安装任务已交给 watcher 执行。" : "定时备份移除任务已交给 watcher 执行。")
                .watcherActionFile(actionFile.toString())
                .createTime(LocalDateTime.now())
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

    private String normalizeCron(String cronExpression) {
        String cron = StrUtil.blankToDefault(cronExpression, "0 3 * * *").trim();
        if (cron.contains("\n") || cron.contains("\r")) {
            throw new OciException(-1, "Cron 表达式不能包含换行");
        }
        if (cron.split("\\s+").length != 5) {
            throw new OciException(-1, "请使用 5 段 Linux cron 表达式，例如 0 3 * * *");
        }
        return cron;
    }

    private Path writeWatcherAction(Map<String, String> values) {
        try {
            Path runtimeDir = Path.of(appDir, "runtime");
            Files.createDirectories(runtimeDir);
            Path actionFile = runtimeDir.resolve("watcher_action.env");
            Path processingFile = runtimeDir.resolve("watcher_action.env.processing");
            if (Files.exists(actionFile) || Files.exists(processingFile)) {
                throw new OciException(-1, "已有 watcher 动作正在排队或执行，请稍后再试");
            }
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String value = StrUtil.blankToDefault(entry.getValue(), "");
                if (value.contains("\n") || value.contains("\r")) {
                    throw new OciException(-1, "动作参数不合法");
                }
                content.append(entry.getKey()).append('=').append(value).append('\n');
            }
            Path tempFile = runtimeDir.resolve("watcher_action.env.tmp");
            Files.writeString(tempFile, content.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(tempFile, actionFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, actionFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return actionFile;
        } catch (OciException e) {
            throw e;
        } catch (Exception e) {
            log.error("Write watcher action failed", e);
            throw new OciException(-1, "写入 watcher 动作失败: " + e.getMessage());
        }
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
