package com.tony.kingdetective.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tony.kingdetective.bean.entity.LoginAttempt;
import com.tony.kingdetective.mapper.LoginAttemptMapper;
import com.tony.kingdetective.service.ILoginAttemptService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Login Attempt Service Implementation
 * 
 * @author antigravity-ai
 */
@Service
public class LoginAttemptServiceImpl extends ServiceImpl<LoginAttemptMapper, LoginAttempt> implements ILoginAttemptService {
    
    @Override
    public void recordFailure(String ipAddress) {
        LambdaQueryWrapper<LoginAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginAttempt::getIpAddress, ipAddress);
        LoginAttempt attempt = getOne(wrapper);
        
        if (attempt == null) {
            // Create new record
            attempt = LoginAttempt.builder()
                    .ipAddress(ipAddress)
                    .attemptCount(1)
                    .lastAttempt(LocalDateTime.now())
                    .build();
            save(attempt);
        } else {
            // Increment count
            attempt.setAttemptCount(attempt.getAttemptCount() + 1);
            attempt.setLastAttempt(LocalDateTime.now());
            updateById(attempt);
        }
    }
    
    @Override
    public int getAttemptCount(String ipAddress) {
        LambdaQueryWrapper<LoginAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginAttempt::getIpAddress, ipAddress);
        LoginAttempt attempt = getOne(wrapper);
        return attempt == null ? 0 : attempt.getAttemptCount();
    }
    
    @Override
    public void clearAttempts(String ipAddress) {
        LambdaQueryWrapper<LoginAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginAttempt::getIpAddress, ipAddress);
        remove(wrapper);
    }
    
    @Override
    public void cleanExpiredAttempts() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
        LambdaQueryWrapper<LoginAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(LoginAttempt::getLastAttempt, expireTime);
        remove(wrapper);
    }
}
