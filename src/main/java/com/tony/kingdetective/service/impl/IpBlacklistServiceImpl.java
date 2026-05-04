package com.tony.kingdetective.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tony.kingdetective.bean.entity.IpBlacklist;
import com.tony.kingdetective.mapper.IpBlacklistMapper;
import com.tony.kingdetective.service.IIpBlacklistService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IP Blacklist Service Implementation
 * 
 * @author antigravity-ai
 */
@Service
public class IpBlacklistServiceImpl extends ServiceImpl<IpBlacklistMapper, IpBlacklist> implements IIpBlacklistService {
    
    @Override
    public boolean isBlacklisted(String ipAddress) {
        LambdaQueryWrapper<IpBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IpBlacklist::getIpAddress, ipAddress);
        return count(wrapper) > 0;
    }
    
    @Override
    public void addToBlacklist(String ipAddress, String reason, String bannedBy) {
        // Check if already exists
        if (isBlacklisted(ipAddress)) {
            return;
        }
        
        IpBlacklist blacklist = IpBlacklist.builder()
                .ipAddress(ipAddress)
                .reason(reason)
                .bannedBy(bannedBy)
                .createTime(LocalDateTime.now())
                .build();
        save(blacklist);
    }
    
    @Override
    public void removeFromBlacklist(String ipAddress) {
        LambdaQueryWrapper<IpBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IpBlacklist::getIpAddress, ipAddress);
        remove(wrapper);
    }
    
    @Override
    public List<IpBlacklist> listAll() {
        LambdaQueryWrapper<IpBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(IpBlacklist::getCreateTime);
        return list(wrapper);
    }
    
    @Override
    public void clearAll() {
        remove(new LambdaQueryWrapper<>());
    }
}
