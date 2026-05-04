package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * @ClassName EnableEnum
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-06 16:14
 **/
@Getter
public enum EnableEnum {

    ON("true", "开启"),
    OFF("false", "关闭"),
    ;

    private String code;
    private String desc;

    EnableEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
