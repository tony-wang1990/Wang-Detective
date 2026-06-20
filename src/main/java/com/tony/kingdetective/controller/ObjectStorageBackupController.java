package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageBackupParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageListObjectsParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageObjectParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageRestoreParams;
import com.tony.kingdetective.bean.params.oci.objectstorage.ObjectStorageScheduleParams;
import com.tony.kingdetective.bean.response.oci.objectstorage.ObjectStorageBackupRsp;
import com.tony.kingdetective.service.oci.ObjectStorageBackupService;
import com.tony.kingdetective.service.OperationAuditSupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/backups")
public class ObjectStorageBackupController {

    private final ObjectStorageBackupService backupService;
    private final OperationAuditSupport audit;

    public ObjectStorageBackupController(ObjectStorageBackupService backupService, OperationAuditSupport audit) {
        this.backupService = backupService;
        this.audit = audit;
    }

    @GetMapping("/buckets")
    public ResponseData<List<ObjectStorageBackupRsp.BucketInfo>> buckets(@RequestParam("ociCfgId") String ociCfgId) {
        return ResponseData.successData(backupService.listBuckets(ociCfgId));
    }

    @PostMapping("/objects")
    public ResponseData<ObjectStorageBackupRsp.ObjectList> objects(@RequestBody @Valid ObjectStorageListObjectsParams params) {
        return ResponseData.successData(backupService.listObjects(params));
    }

    @PostMapping("/archive")
    public ResponseData<ObjectStorageBackupRsp> archive(@RequestBody @Valid ObjectStorageBackupParams params) {
        return ResponseData.successData(audit.supply(
                "BACKUP_CREATE",
                params.getBucketName(),
                "cfgId=" + params.getOciCfgId() + ", upload=" + params.getUploadToObjectStorage(),
                () -> backupService.createBackup(params)
        ));
    }

    @PostMapping("/delete-object")
    public ResponseData<Void> deleteObject(@RequestBody @Valid ObjectStorageObjectParams params) {
        audit.run(
                "BACKUP_OBJECT_DELETE",
                params.getObjectName(),
                "cfgId=" + params.getOciCfgId() + ", bucket=" + params.getBucketName(),
                () -> backupService.deleteObject(params)
        );
        return ResponseData.successData();
    }

    @GetMapping("/local")
    public ResponseData<List<ObjectStorageBackupRsp.LocalBackupInfo>> localBackups(@RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return ResponseData.successData(backupService.listLocalBackupInfos(limit));
    }

    @GetMapping("/restore-plan")
    public ResponseData<ObjectStorageBackupRsp.RestorePlan> restorePlan(@RequestParam("backupName") String backupName) {
        return ResponseData.successData(backupService.restorePlan(backupName));
    }

    @GetMapping("/schedule-plan")
    public ResponseData<ObjectStorageBackupRsp.SchedulePlan> schedulePlan(@RequestParam(value = "cron", required = false, defaultValue = "0 3 * * *") String cron) {
        return ResponseData.successData(backupService.schedulePlan(cron));
    }

    @PostMapping("/restore-local")
    public ResponseData<ObjectStorageBackupRsp.ActionResult> restoreLocal(@RequestBody @Valid ObjectStorageRestoreParams params) {
        return ResponseData.successData(audit.supply(
                "BACKUP_RESTORE_QUEUE",
                params.getBackupName(),
                "restore queued through watcher",
                () -> backupService.dispatchRestore(params)
        ));
    }

    @PostMapping("/schedule")
    public ResponseData<ObjectStorageBackupRsp.ActionResult> schedule(@RequestBody @Valid ObjectStorageScheduleParams params) {
        return ResponseData.successData(audit.supply(
                "BACKUP_SCHEDULE_UPDATE",
                "backup-schedule",
                "enabled=" + params.getEnabled() + ", cron=" + params.getCronExpression(),
                () -> backupService.dispatchSchedule(params)
        ));
    }
}
