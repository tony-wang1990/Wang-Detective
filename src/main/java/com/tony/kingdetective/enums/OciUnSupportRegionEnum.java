package com.tony.kingdetective.enums;

import com.oracle.bmc.Realm;
import lombok.Getter;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.enums
 * @className: OciUnSupportRegionEnum
 * @author: Tony Wang
 * @date: 2024/11/30 17:29
 */
@Getter
public enum OciUnSupportRegionEnum {

    EU_TURIN_1("eu-turin-1",Realm.OC1,"nrq"),
    ;


    OciUnSupportRegionEnum(String regionId, Realm realm, String regionCode) {
        this.regionId = regionId;
        this.realm = realm;
        this.regionCode = regionCode;
    }

    private String regionId;
    private Realm realm;
    private String regionCode;
}
