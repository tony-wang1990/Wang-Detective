package com.tony.kingdetective.bean.params.oci.objectstorage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ObjectStorageRestoreParams {

    @NotBlank(message = "请选择本地备份文件")
    private String backupName;

    @NotBlank(message = "请确认恢复动作")
    private String confirm;
}
