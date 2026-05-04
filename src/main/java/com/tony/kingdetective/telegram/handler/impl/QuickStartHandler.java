package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.requests.InstanceActionRequest;
import com.oracle.bmc.core.requests.ListInstancesRequest;
import com.oracle.bmc.core.responses.ListInstancesResponse;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Quick start instances handler - quickly start all stopped instances
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class QuickStartHandler extends AbstractCallbackHandler {
    
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
            message.append("【快捷开机】\n\n");
            
            List<InstanceInfo> stoppedInstances = new ArrayList<>();
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    ComputeClient computeClient = fetcher.getComputeClient();
                    String compartmentId = fetcher.getCompartmentId();
                    
                    ListInstancesRequest listRequest = ListInstancesRequest.builder()
                            .compartmentId(compartmentId)
                            .build();
                    
                    ListInstancesResponse listResponse = computeClient.listInstances(listRequest);
                    
                    for (Instance instance : listResponse.getItems()) {
                        if (instance.getLifecycleState() == Instance.LifecycleState.Stopped) {
                            InstanceInfo info = new InstanceInfo();
                            info.username = user.getUsername();
                            info.instanceId = instance.getId();
                            info.instanceName = instance.getDisplayName();
                            info.region = user.getOciCfg().getRegion();
                            info.ociCfgId = user.getOciCfg().getId();
                            stoppedInstances.add(info);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to list instances for user: {}", user.getUsername(), e);
                }
            }
            
            if (stoppedInstances.isEmpty()) {
                message.append("✅ 所有实例都在运行中\n\n");
                message.append("💡 无需开机");
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                KeyboardBuilder.buildBackToMainMenuRow(),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            message.append(String.format("发现 %d 个已停止的实例:\n\n", stoppedInstances.size()));
            
            for (int i = 0; i < stoppedInstances.size(); i++) {
                InstanceInfo info = stoppedInstances.get(i);
                message.append(String.format(
                        "%d. %s\n" +
                        "   账户: %s\n" +
                        "   区域: %s\n\n",
                        i + 1,
                        info.instanceName,
                        info.username,
                        info.region
                ));
            }
            
            message.append("确认要启动所有实例吗？");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🚀 全部开机", "confirm_quick_start"),
                                    KeyboardBuilder.button("❌ 取消", "back_to_main")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to check stopped instances", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 查询失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "quick_start";
    }
    
    private static class InstanceInfo {
        String username;
        String instanceId;
        String instanceName;
        String region;
        String ociCfgId;
    }
}

/**
 * Confirm and execute quick start
 */
@Slf4j
@Component
class ConfirmQuickStartHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("【开机结果】\n\n");
            
            int successCount = 0;
            int failCount = 0;
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    ComputeClient computeClient = fetcher.getComputeClient();
                    String compartmentId = fetcher.getCompartmentId();
                    
                    ListInstancesRequest listRequest = ListInstancesRequest.builder()
                            .compartmentId(compartmentId)
                            .build();
                    
                    ListInstancesResponse listResponse = computeClient.listInstances(listRequest);
                    
                    for (Instance instance : listResponse.getItems()) {
                        if (instance.getLifecycleState() == Instance.LifecycleState.Stopped) {
                            try {
                                InstanceActionRequest actionRequest = InstanceActionRequest.builder()
                                        .instanceId(instance.getId())
                                        .action("START")
                                        .build();
                                
                                computeClient.instanceAction(actionRequest);
                                
                                message.append(String.format(
                                        "✅ %s (%s)\n",
                                        instance.getDisplayName(),
                                        user.getUsername()
                                ));
                                successCount++;
                                
                            } catch (Exception e) {
                                message.append(String.format(
                                        "❌ %s: %s\n",
                                        instance.getDisplayName(),
                                        e.getMessage()
                                ));
                                failCount++;
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to start instances for user: {}", user.getUsername(), e);
                    failCount++;
                }
            }
            
            message.append("\n━━━━━━━━━━━━━━━━\n");
            message.append(String.format("✅ 成功: %d / ❌ 失败: %d\n", successCount, failCount));
            message.append("\n💡 实例启动中，请稍候片刻");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to quick start", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 开机失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_quick_start";
    }
}
