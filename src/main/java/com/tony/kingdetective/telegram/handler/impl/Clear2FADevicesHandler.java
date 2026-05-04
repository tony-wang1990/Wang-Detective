package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.MfaTotpDeviceSummary;
import com.oracle.bmc.identity.requests.DeleteMfaTotpDeviceRequest;
import com.oracle.bmc.identity.requests.ListMfaTotpDevicesRequest;
import com.oracle.bmc.identity.responses.ListMfaTotpDevicesResponse;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.List;

/**
 * Clear all 2FA devices handler
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class Clear2FADevicesHandler extends AbstractCallbackHandler {
    
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
            message.append("【清除所有2FA设备】\n\n");
            message.append("⚠️ 警告: 此操作将删除所有MFA设备!\n\n");
            
            int totalDevices = 0;
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    IdentityClient identityClient = fetcher.getIdentityClient();
                    String userId = fetcher.getUserInfo().getId();
                    
                    ListMfaTotpDevicesRequest listRequest = ListMfaTotpDevicesRequest.builder()
                            .userId(userId)
                            .build();
                    
                    ListMfaTotpDevicesResponse listResponse = identityClient.listMfaTotpDevices(listRequest);
                    List<MfaTotpDeviceSummary> devices = listResponse.getItems();
                    
                    message.append(String.format("📌 %s: ", user.getUsername()));
                    
                    if (devices.isEmpty()) {
                        message.append("无2FA设备\n");
                    } else {
                        message.append(String.format("%d个设备\n", devices.size()));
                        totalDevices += devices.size();
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to list 2FA devices for user: {}", user.getUsername(), e);
                    message.append(String.format("查询失败: %s\n", e.getMessage()));
                }
            }
            
            message.append(String.format("\n共发现 %d 个2FA设备\n\n", totalDevices));
            
            if (totalDevices > 0) {
                message.append("确认要删除所有2FA设备吗？");
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("❌ 确认删除", "confirm_clear_2fa"),
                                        KeyboardBuilder.button("✅ 取消", "back_to_main")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            } else {
                message.append("💡 无需清除");
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                KeyboardBuilder.buildBackToMainMenuRow(),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to check 2FA devices", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 查询失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "clear_2fa_devices";
    }
}

/**
 * Confirm and execute 2FA devices deletion
 */
@Slf4j
@Component
class ConfirmClear2FAHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("【清除结果】\n\n");
            
            int successCount = 0;
            int failCount = 0;
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    IdentityClient identityClient = fetcher.getIdentityClient();
                    String userId = fetcher.getUserInfo().getId();
                    
                    ListMfaTotpDevicesRequest listRequest = ListMfaTotpDevicesRequest.builder()
                            .userId(userId)
                            .build();
                    
                    ListMfaTotpDevicesResponse listResponse = identityClient.listMfaTotpDevices(listRequest);
                    List<MfaTotpDeviceSummary> devices = listResponse.getItems();
                    
                    message.append(String.format("📌 %s:\n", user.getUsername()));
                    
                    for (MfaTotpDeviceSummary device : devices) {
                        try {
                            DeleteMfaTotpDeviceRequest deleteRequest = DeleteMfaTotpDeviceRequest.builder()
                                    .userId(userId)
                                    .mfaTotpDeviceId(device.getId())
                                    .build();
                            
                            identityClient.deleteMfaTotpDevice(deleteRequest);
                            message.append(String.format("  ✅ 已删除: %s\n", device.getId()));
                            successCount++;
                            
                        } catch (Exception e) {
                            message.append(String.format("  ❌ 删除失败: %s\n", e.getMessage()));
                            failCount++;
                        }
                    }
                    
                    message.append("\n");
                    
                } catch (Exception e) {
                    log.error("Failed to clear 2FA for user: {}", user.getUsername(), e);
                    message.append(String.format("  ❌ 处理失败: %s\n\n", e.getMessage()));
                    failCount++;
                }
            }
            
            message.append("━━━━━━━━━━━━━━━━\n");
            message.append(String.format("✅ 成功: %d / ❌ 失败: %d\n", successCount, failCount));
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to clear 2FA devices", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 清除失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_clear_2fa";
    }
}
