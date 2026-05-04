package com.tony.kingdetective.bean.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @TableName oci_user
 */
@TableName(value ="oci_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OciUser implements Serializable {

    @TableId
    private String id;

    private String username;

    private String tenantName;

    private LocalDateTime tenantCreateTime;

    private String ociTenantId;

    private String ociUserId;

    private String ociFingerprint;

    private String ociRegion;

    private String ociKeyPath;

    @TableField("deleted")
    private Integer deleted;

    private LocalDateTime createTime;

    public String getUserId() { return this.ociUserId; }
    public String getTenantId() { return this.ociTenantId; }
    public String getFingerprint() { return this.ociFingerprint; }

    public String getPrivateKey() {
        if (this.ociKeyPath != null && cn.hutool.core.io.FileUtil.exist(this.ociKeyPath)) {
            return cn.hutool.core.io.FileUtil.readUtf8String(this.ociKeyPath);
        }
        return null;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}