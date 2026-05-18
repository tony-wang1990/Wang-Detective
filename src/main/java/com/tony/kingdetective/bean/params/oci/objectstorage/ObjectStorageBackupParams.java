package com.tony.kingdetective.bean.params.oci.objectstorage;

import lombok.Data;

@Data
public class ObjectStorageBackupParams {
    private String ociCfgId;

    private String bucketName;

    private String prefix = "wang-detective/backups";

    private Boolean includeLogs = false;

    private Boolean uploadToObjectStorage = true;
}
