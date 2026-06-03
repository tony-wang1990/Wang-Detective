package com.tony.kingdetective.bean.params.oci.objectstorage;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ObjectStorageScheduleParams {

    private String cronExpression;

    @NotNull(message = "请选择启用或关闭定时备份")
    private Boolean enabled;
}
