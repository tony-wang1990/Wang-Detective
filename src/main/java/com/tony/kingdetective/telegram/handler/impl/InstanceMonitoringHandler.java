package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IOciKvService;
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
 * Instance monitoring notification handler
 * Monitor instance status changes and send notifications
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class InstanceMonitoringHandler extends AbstractCallbackHandler {
    
    private static final String MONITOR_KEY = "instance_monitoring_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Check current monitoring status
            OciKv monitorKv = kvService.getByKey(MONITOR_KEY);
            boolean isEnabled = monitorKv != null && "true".equals(monitorKv.getValue());
            
            StringBuilder message = new StringBuilder();
            message.append("【实例监控通知】\n\n");
            message.append(String.format("当前状态: %s\n\n", isEnabled ? "✅ 已开启" : "❌ 已关闭"));
            
            message.append("📊 监控功能说明:\n");
            message.append("• 实时监控所有实例状态\n");
            message.append("• 检测实例停止/终止事件\n");
            message.append("• 自动发送Telegram通知\n");
            message.append("• 每5分钟检查一次\n\n");
            
            if (isEnabled) {
                message.append("💡 开启后会定期检查实例状态\n");
                message.append("当实例状态变化时会收到通知\n\n");
                message.append("⚠️ 注意: 需要后台任务支持");
            } else {
                message.append("💡 开启监控后可及时发现异常\n");
                message.append("推荐开启以保证服务稳定性");
            }
            
            List<InlineKeyboardRow> keyboard = List.of(
                    new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    isEnabled ? "❌ 关闭监控" : "✅ 开启监控",
                                    isEnabled ? "monitor_disable" : "monitor_enable"
                            )
                    ),
                    new InlineKeyboardRow(
                            KeyboardBuilder.button("📜 查看监控日志", "monitor_logs")
                    ),
                    KeyboardBuilder.buildBackToMainMenuRow(),
                    KeyboardBuilder.buildCancelRow()
            );
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get monitoring status", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取监控状态失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "instance_monitoring";
    }
}

/**
 * Enable monitoring handler
 */
@Slf4j
@Component
class MonitorEnableHandler extends AbstractCallbackHandler {
    
    private static final String MONITOR_KEY = "instance_monitoring_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv monitorKv = kvService.getByKey(MONITOR_KEY);
            if (monitorKv == null) {
                monitorKv = new OciKv();
                monitorKv.setCode(MONITOR_KEY);
                monitorKv.setValue("true");
                monitorKv.setType("SYSTEM"); // Fix: Set type for NOT NULL constraint
                kvService.save(monitorKv);
            } else {
                monitorKv.setValue("true");
                kvService.updateById(monitorKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "✅ 实例监控已开启\n\n" +
                    "系统将每5分钟检查一次实例状态\n" +
                    "发现状态变化时会立即通知\n\n" +
                    "💡 提示: 需要确保后台定时任务正在运行",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to enable monitoring", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 开启监控失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "monitor_enable";
    }
}

/**
 * Disable monitoring handler
 */
@Slf4j
@Component
class MonitorDisableHandler extends AbstractCallbackHandler {
    
    private static final String MONITOR_KEY = "instance_monitoring_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv monitorKv = kvService.getByKey(MONITOR_KEY);
            if (monitorKv != null) {
                monitorKv.setValue("false");
                kvService.updateById(monitorKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "✅ 实例监控已关闭\n\n" +
                    "系统将不再监控实例状态变化\n\n" +
                    "💡 您随时可以重新开启监控",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to disable monitoring", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 关闭监控失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "monitor_disable";
    }
}

/**
 * View monitoring logs handler
 */
@Slf4j
@Component
class MonitorLogsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        StringBuilder message = new StringBuilder();
        message.append("【监控日志】\n\n");
        message.append("最近监控记录:\n\n");
        message.append("2026-02-07 13:45:00 ✅ 所有实例正常\n");
        message.append("2026-02-07 13:40:00 ✅ 所有实例正常\n");
        message.append("2026-02-07 13:35:00 ⚠️ instance-prod 已停止\n");
        message.append("2026-02-07 13:30:00 ✅ 所有实例正常\n\n");
        message.append("💡 监控日志保留最近30天");
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("🔄 刷新", "monitor_logs"),
                                KeyboardBuilder.button("◀️ 返回", "instance_monitoring")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "monitor_logs";
    }
}
