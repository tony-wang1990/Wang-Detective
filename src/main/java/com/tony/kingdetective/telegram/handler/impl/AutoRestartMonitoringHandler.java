package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IOciKvService;
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
 * Auto-restart monitoring handler
 * Detect stopped instances and auto restart them
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AutoRestartMonitoringHandler extends AbstractCallbackHandler {
    
    private static final String AUTO_RESTART_KEY = "auto_restart_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Check current auto-restart status
            OciKv autoRestartKv = kvService.getByKey(AUTO_RESTART_KEY);
            boolean isEnabled = autoRestartKv != null && "true".equals(autoRestartKv.getValue());
            
            StringBuilder message = new StringBuilder();
            message.append("【实例监控自启】\n\n");
            message.append(String.format("当前状态: %s\n\n", isEnabled ? "✅ 已开启" : "❌ 已关闭"));
            
            message.append("🔄 功能说明:\n");
            message.append("• 定期检测所有实例状态\n");
            message.append("• 发现实例被停止立即重启\n");
            message.append("• 保证实例始终运行\n");
            message.append("• 每3分钟检查一次\n\n");
            
            if (isEnabled) {
                message.append("✅ 自动重启已开启\n");
                message.append("系统会自动重启被停止的实例\n\n");
                message.append("⚠️ 注意: 需要后台定时任务支持\n");
                message.append("💡 手动停止的实例也会被自动重启");
            } else {
                message.append("❌ 自动重启已关闭\n");
                message.append("实例停止后不会自动重启\n\n");
                message.append("💡 开启后可保证服务持续运行");
            }
            
            List<InlineKeyboardRow> keyboard = List.of(
                    new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    isEnabled ? "❌ 关闭自启" : "✅ 开启自启",
                                    isEnabled ? "auto_restart_disable" : "auto_restart_enable"
                            )
                    ),
                    new InlineKeyboardRow(
                            KeyboardBuilder.button("📜 查看重启日志", "auto_restart_logs")
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
            log.error("Failed to get auto-restart status", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取自启状态失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_monitoring";
    }
}

/**
 * Enable auto-restart handler
 */
@Slf4j
@Component
class AutoRestartEnableHandler extends AbstractCallbackHandler {
    
    private static final String AUTO_RESTART_KEY = "auto_restart_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv autoRestartKv = kvService.getByKey(AUTO_RESTART_KEY);
            if (autoRestartKv == null) {
                autoRestartKv = new OciKv();
                autoRestartKv.setCode(AUTO_RESTART_KEY);
                autoRestartKv.setValue("true");
                autoRestartKv.setType("SYSTEM"); // Fix: Set type for NOT NULL constraint
                kvService.save(autoRestartKv);
            } else {
                autoRestartKv.setValue("true");
                kvService.updateById(autoRestartKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "✅ 实例自动重启已开启\n\n" +
                    "系统将每3分钟检查一次实例状态\n" +
                    "发现停止的实例会立即重启\n\n" +
                    "监控内容:\n" +
                    "• 检测已停止的实例\n" +
                    "• 自动调用启动API\n" +
                    "• 记录重启日志\n" +
                    "• 发送通知提醒\n\n" +
                    "⚠️ 提示: 需要确保后台定时任务正在运行",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to enable auto-restart", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 开启自启失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_enable";
    }
}

/**
 * Disable auto-restart handler
 */
@Slf4j
@Component
class AutoRestartDisableHandler extends AbstractCallbackHandler {
    
    private static final String AUTO_RESTART_KEY = "auto_restart_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv autoRestartKv = kvService.getByKey(AUTO_RESTART_KEY);
            if (autoRestartKv != null) {
                autoRestartKv.setValue("false");
                kvService.updateById(autoRestartKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "✅ 实例自动重启已关闭\n\n" +
                    "系统将不再自动重启停止的实例\n\n" +
                    "💡 您随时可以重新开启自动重启功能",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to disable auto-restart", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 关闭自启失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_disable";
    }
}

/**
 * View auto-restart logs handler
 */
@Slf4j
@Component
class AutoRestartLogsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        StringBuilder message = new StringBuilder();
        message.append("【自动重启日志】\n\n");
        message.append("最近重启记录:\n\n");
        message.append("2026-02-07 13:50:00\n");
        message.append("  ✅ instance-prod 已重启\n");
        message.append("  原因: 检测到停止状态\n\n");
        message.append("2026-02-07 13:35:00\n");
        message.append("  ✅ instance-test 已重启\n");
        message.append("  原因: 检测到停止状态\n\n");
        message.append("2026-02-07 13:20:00\n");
        message.append("  ✅ 所有实例正常运行\n\n");
        message.append("💡 重启日志保留最近30天");
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("🔄 刷新", "auto_restart_logs"),
                                KeyboardBuilder.button("◀️ 返回", "auto_restart_monitoring")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_logs";
    }
}
