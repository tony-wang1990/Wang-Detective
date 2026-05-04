package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.entity.OciUser;

/**
* @author Administrator
* @description 针对表【oci_kv】的数据库操作Service
* @createDate 2024-11-12 16:44:39
*/
public interface IOciKvService extends IService<OciKv> {

    OciKv getByKey(String key);

}
