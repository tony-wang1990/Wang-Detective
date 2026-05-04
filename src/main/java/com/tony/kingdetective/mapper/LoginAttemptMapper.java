package com.tony.kingdetective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tony.kingdetective.bean.entity.LoginAttempt;
import org.apache.ibatis.annotations.Mapper;

/**
 * Login Attempt Mapper
 * 
 * @author antigravity-ai
 */
@Mapper
public interface LoginAttemptMapper extends BaseMapper<LoginAttempt> {
}
