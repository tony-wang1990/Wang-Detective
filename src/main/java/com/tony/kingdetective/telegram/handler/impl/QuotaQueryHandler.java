package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.oracle.bmc.limits.LimitsClient;
import com.oracle.bmc.limits.model.ResourceAvailability;
import com.oracle.bmc.limits.requests.GetResourceAvailabilityRequest;
import com.oracle.bmc.limits.responses.GetResourceAvailabilityResponse;
import com.oracle.bmc.identity.model.AvailabilityDomain;

import java.io.Serializable;
import java.util.*;

/**
 * Quota query handler - shows resource quotas and usage
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class QuotaQueryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Get the first available config for demo
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            if (users == null || users.isEmpty()) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置\n\n请先添加 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            // Use first config
            SysUserDTO user = users.get(0);
            
            // Fetch quota information
            StringBuilder message = new StringBuilder();
            message.append("【配额查询】\n\n");
            message.append(String.format("租户: %s\n", user.getUsername()));
            message.append(String.format("区域: %s\n\n", user.getOciCfg().getRegion()));
            message.append("━━━━━━━━━━━━━━━━\n\n");
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                LimitsClient limitsClient = LimitsClient.builder()
                        .build(fetcher.getAuthenticationDetailsProvider());
                
                // Get availability domains
                List<AvailabilityDomain> ads = fetcher.getAvailabilityDomains();
                String compartmentId = fetcher.getCompartmentId();
                
                // Query key resources
                Map<String, QuotaInfo> quotas = new LinkedHashMap<>();
                
                // Compute instance quotas
                queryQuota(limitsClient, compartmentId, ads.get(0).getName(), 
                          "compute", "vm-standard-a1-count", "ARM实例", quotas);
                queryQuota(limitsClient, compartmentId, ads.get(0).getName(),
                          "compute", "vm-standard-e2-1-micro-count", "E2.1 Micro实例", quotas);
                
                // Storage quotas  
                queryQuota(limitsClient, compartmentId, ads.get(0).getName(),
                          "compute", "boot-volume-count", "启动卷", quotas);
                queryQuota(limitsClient, compartmentId, ads.get(0).getName(),
                          "compute", "volume-count", "块存储卷", quotas);
                
                // VCN quotas
                queryQuota(limitsClient, compartmentId, null,
                          "vcn", "vcn-count", "VCN", quotas);
                queryQuota(limitsClient, compartmentId, null,
                          "vcn", "subnet-count", "子网", quotas);
                
                // Format output
                for (Map.Entry<String, QuotaInfo> entry : quotas.entrySet()) {
                    QuotaInfo info = entry.getValue();
                    message.append(String.format("📊 %s\n", info.name));
                    message.append(String.format("   配额: %d\n", info.limit));
                    message.append(String.format("   已用: %d\n", info.used));
                    message.append(String.format("   剩余: %d\n", info.available));
                    
                    // Progress bar
                    double usage = info.limit > 0 ? (double) info.used / info.limit : 0;
                    String progressBar = createProgressBar(usage);
                    message.append(String.format("   %s %.1f%%\n\n", progressBar, usage * 100));
                }
                
                limitsClient.close();
                
            } catch (Exception e) {
                log.error("Failed to query quotas", e);
                return buildEditMessage(
                        callbackQuery,
                        "❌ 查询配额失败\n\n" + e.getMessage(),
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            message.append("━━━━━━━━━━━━━━━━\n");
            message.append("💡 配额数据来自 OCI Limits API");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to handle quota query", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 处理失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    /**
     * Query a single quota
     */
    private void queryQuota(LimitsClient client, String compartmentId, String ad,
                           String serviceName, String limitName, String displayName,
                           Map<String, QuotaInfo> quotas) {
        try {
            GetResourceAvailabilityRequest.Builder requestBuilder = GetResourceAvailabilityRequest.builder()
                    .serviceName(serviceName)
                    .limitName(limitName)
                    .compartmentId(compartmentId);
            
            if (ad != null) {
                requestBuilder.availabilityDomain(ad);
            }
            
            GetResourceAvailabilityResponse response = client.getResourceAvailability(requestBuilder.build());
            ResourceAvailability availability = response.getResourceAvailability();
            
            QuotaInfo info = new QuotaInfo();
            info.name = displayName;
            // ResourceAvailability doesn't have getLimit(), use available field directly
            Long available = availability.getAvailable();
            Long used = availability.getUsed();
            info.available = available != null ? available.intValue() : 0;
            info.used = used != null ? used.intValue() : 0;
            info.limit = info.available + info.used; // Calculated limit

            
            quotas.put(limitName, info);
            
        } catch (Exception e) {
            log.warn("Failed to query quota for {}: {}", limitName, e.getMessage());
            // Add with unknown values
            QuotaInfo info = new QuotaInfo();
            info.name = displayName;
            info.limit = 0;
            info.used = 0;
            info.available = 0;
            quotas.put(limitName, info);
        }
    }
    
    /**
     * Create a text-based progress bar
     */
    private String createProgressBar(double ratio) {
        int total = 10;
        int filled = (int) Math.round(ratio * total);
        filled = Math.max(0, Math.min(total, filled));
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        return bar.toString();
    }
    
    @Override
    public String getCallbackPattern() {
        return "quota_query";
    }
    
    /**
     * Inner class to hold quota information
     */
    private static class QuotaInfo {
        String name;
        int limit;
        int used;
        int available;
    }
}
