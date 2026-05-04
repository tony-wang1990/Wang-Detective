package com.tony.kingdetective.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
import org.springframework.stereotype.Service;
import com.tony.kingdetective.mapper.OciUserMapper;


/**
* @author Administrator
* @description 针对表【oci_user】的数据库操作Service实现
* @createDate 2024-11-12 16:44:39
*/
@Service
public class OciUserServiceImpl extends ServiceImpl<OciUserMapper, OciUser>
    implements IOciUserService {

}




