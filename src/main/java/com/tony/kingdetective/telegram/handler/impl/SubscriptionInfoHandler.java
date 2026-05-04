package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Region;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.bmc.identity.requests.ListRegionSubscriptionsRequest;
import com.oracle.bmc.identity.requests.ListRegionsRequest;
import com.oracle.bmc.identity.responses.ListRegionSubscriptionsResponse;
import com.oracle.bmc.identity.responses.ListRegionsResponse;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subscription info handler - shows OCI subscription details
 * Multi-region? Home region? Subscribed regions?
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class SubscriptionInfoHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            if (CollectionUtil.isEmpty(users)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("【甲骨文订阅信息】\n\n");
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    IdentityClient identityClient = fetcher.getIdentityClient();
                    String tenantId = user.getOciCfg().getTenantId();
                    
                    message.append("━━━━━━━━━━━━━━━━\n");
                    message.append(String.format("📌 账户: %s\n\n", user.getUsername()));
                    
                    // Get region subscriptions
                    ListRegionSubscriptionsRequest subRequest = ListRegionSubscriptionsRequest.builder()
                            .tenancyId(tenantId)
                            .build();
                    
                    ListRegionSubscriptionsResponse subResponse = identityClient.listRegionSubscriptions(subRequest);
                    List<RegionSubscription> subscriptions = subResponse.getItems();
                    
                    // Get all available regions
                    ListRegionsRequest regRequest = ListRegionsRequest.builder().build();
                    ListRegionsResponse regResponse = identityClient.listRegions(regRequest);
                    List<Region> allRegions = regResponse.getItems();
                    
                    // Map region names
                    Map<String, String> regionNameMap = new HashMap<>();
                    for (Region region : allRegions) {
                        regionNameMap.put(region.getKey(), region.getName());
                    }
                    
                    // Home region
                    String homeRegion = subscriptions.stream()
                            .filter(RegionSubscription::getIsHomeRegion)
                            .map(RegionSubscription::getRegionName)
                            .findFirst()
                            .orElse("未知");
                    
                    message.append(String.format("🏠 主区域 (Home Region):\n   %s\n\n", homeRegion));
                    
                    // Multi-region check
                    boolean isMultiRegion = subscriptions.size() > 1;
                    message.append(String.format("🌍 多区域订阅: %s\n\n", isMultiRegion ? "是 ✅" : "否 ❌"));
                    
                    // List all subscribed regions
                    message.append(String.format("📍 已订阅区域 (%d个):\n", subscriptions.size()));
                    
                    for (RegionSubscription sub : subscriptions) {
                        String regionKey = sub.getRegionKey();
                        String regionName = sub.getRegionName();
                        String status = sub.getStatus().getValue();
                        boolean isHome = sub.getIsHomeRegion();
                        
                        String statusIcon = "READY".equals(status) ? "✅" : "⚠️";
                        String homeIcon = isHome ? "🏠" : "📍";
                        
                        message.append(String.format(
                                "  %s %s %s\n" +
                                "     状态: %s %s\n",
                                homeIcon,
                                regionKey,
                                regionName,
                                statusIcon,
                                status
                        ));
                    }
                    
                    message.append("\n");
                    
                } catch (Exception e) {
                    log.error("Failed to query subscription info for user: {}", user.getUsername(), e);
                    message.append(String.format("❌ 查询失败: %s\n\n", e.getMessage()));
                }
            }
            
            message.append("━━━━━━━━━━━━━━━━\n");
            message.append("💡 数据来自 OCI Identity API");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to query subscription info", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 查询失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "subscription_info";
    }
}
