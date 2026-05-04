package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tony.kingdetective.bean.entity.IpBlacklist;

import java.util.List;

/**
 * IP Blacklist Service
 * 
 * @author antigravity-ai
 */
public interface IIpBlacklistService extends IService<IpBlacklist> {
    
    /**
     * Check if IP is blacklisted
     */
    boolean isBlacklisted(String ipAddress);
    
    /**
     * Add IP to blacklist
     */
    void addToBlacklist(String ipAddress, String reason, String bannedBy);
    
    /**
     * Remove IP from blacklist
     */
    void removeFromBlacklist(String ipAddress);
    
    /**
     * Get all blacklisted IPs
     */
    List<IpBlacklist> listAll();
    
    /**
     * Clear all blacklist
     */
    void clearAll();
}
