package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.enums
 * @className: SysCfgTypeEnum
 * @author: Tony Wang
 * @date: 2024/11/30 17:29
 */
@Getter
public enum SysCfgTypeEnum {

    /**
     * 配置类型
     */
    SYS_INIT_CFG("Y001", "系统基本配置"),
    SYS_MFA_CFG("Y002", "系统MFA配置"),
    SYS_INFO("Y003", "系统信息"),

    ;

    SysCfgTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private String code;
    private String desc;
}
