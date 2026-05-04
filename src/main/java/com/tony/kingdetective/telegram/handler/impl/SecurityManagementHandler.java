package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.IpBlacklist;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IIpBlacklistService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Security Management Handler
 * Main menu for security features
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class SecurityManagementHandler extends AbstractCallbackHandler {
    
    private static final String DEFENSE_MODE_KEY = "defense_mode_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        
        try {
            // Get defense mode status
            OciKv defenseModeKv = kvService.getByKey(DEFENSE_MODE_KEY);
            boolean isDefenseModeEnabled = defenseModeKv != null && "true".equals(defenseModeKv.getValue());
            
            // Get blacklist count
            long blacklistCount = blacklistService.count();
            
            StringBuilder message = new StringBuilder();
            message.append("【🛡 安全管理】\n\n");
            
            // Defense Mode Status
            message.append("🔒 防御模式\n");
            message.append(String.format("状态: %s\n", isDefenseModeEnabled ? "✅ 已开启" : "❌ 已关闭"));
            if (isDefenseModeEnabled) {
                message.append("⚠️ 所有Web访问已阻止\n");
            }
            message.append("\n");
            
            // IP Blacklist Status
            message.append("🚫 IP黑名单\n");
            message.append(String.format("拉黑IP数量: %d\n", blacklistCount));
            message.append("\n");
            
            message.append("━━━━━━━━━━━━━━━━\n");
            message.append("💡 通过TG管理Web端安全\n");
            message.append("🔐 自动拉黑登录失败IP");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🔒 " + (isDefenseModeEnabled ? "关闭" : "开启") + "防御模式", "defense_mode_toggle")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🚫 IP黑名单管理", "ip_blacklist_management")
            ));
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to show security management", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取安全设置失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "security_management";
    }
}

/**
 * Defense Mode Toggle Handler
 */
@Slf4j
@Component
class DefenseModeToggleHandler extends AbstractCallbackHandler {
    
    private static final String DEFENSE_MODE_KEY = "defense_mode_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv defenseModeKv = kvService.getByKey(DEFENSE_MODE_KEY);
            boolean currentStatus = defenseModeKv != null && "true".equals(defenseModeKv.getValue());
            boolean newStatus = !currentStatus;
            
            if (defenseModeKv == null) {
                defenseModeKv = new OciKv();
                defenseModeKv.setCode(DEFENSE_MODE_KEY);
                defenseModeKv.setValue(String.valueOf(newStatus));
                defenseModeKv.setType("SYSTEM"); // Fix: Set type to satisfy NOT NULL constraint
                kvService.save(defenseModeKv);
            } else {
                defenseModeKv.setValue(String.valueOf(newStatus));
                kvService.updateById(defenseModeKv);
            }
            
            String message;
            if (newStatus) {
                message = "✅ 防御模式已开启\n\n" +
                        "⚠️ 重要提示:\n" +
                        "• 所有IP无法访问Web端\n" +
                        "• 包括您自己的IP\n" +
                        "• 仅可通过TG操作\n" +
                        "• OCI任务不受影响\n\n" +
                        "🔒 Web端已完全锁定";
            } else {
                message = "✅ 防御模式已关闭\n\n" +
                        "Web端访问已恢复正常\n" +
                        "IP黑名单仍然生效";
            }
            
            return buildEditMessage(
                    callbackQuery,
                    message,
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "security_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to toggle defense mode", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 切换防御模式失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "security_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "defense_mode_toggle";
    }
}

/**
 * IP Blacklist Management Handler
 */
@Slf4j
@Component
class IpBlacklistManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        
        try {
            List<IpBlacklist> blacklist = blacklistService.listAll();
            
            StringBuilder message = new StringBuilder();
            message.append("【🚫 IP黑名单管理】\n\n");
            
            if (blacklist.isEmpty()) {
                message.append("暂无拉黑IP\n\n");
                message.append("💡 登录失败5次自动拉黑");
            } else {
                message.append(String.format("共 %d 个IP被拉黑:\n\n", blacklist.size()));
                
                for (int i = 0; i < Math.min(blacklist.size(), 10); i++) {
                    IpBlacklist item = blacklist.get(i);
                    message.append(String.format(
                            "%d. %s\n" +
                            "   原因: %s\n" +
                            "   时间: %s\n\n",
                            i + 1,
                            item.getIpAddress(),
                            item.getReason() != null ? item.getReason() : "未知",
                            item.getCreateTime()
                    ));
                }
                
                if (blacklist.size() > 10) {
                    message.append(String.format("... 还有 %d 个IP\n\n", blacklist.size() - 10));
                }
            }
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            if (!blacklist.isEmpty()) {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("🗑 清空黑名单", "blacklist_clear_confirm")
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("◀️ 返回", "security_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to show blacklist", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取黑名单失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "security_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ip_blacklist_management";
    }
}

/**
 * Clear Blacklist Confirmation Handler
 */
@Slf4j
@Component
class BlacklistClearConfirmHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        return buildEditMessage(
                callbackQuery,
                "⚠️ 确认清空黑名单？\n\n" +
                "此操作将删除所有拉黑IP\n" +
                "不可恢复！",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("✅ 确认清空", "blacklist_clear"),
                                KeyboardBuilder.button("❌ 取消", "ip_blacklist_management")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "blacklist_clear_confirm";
    }
}

/**
 * Clear Blacklist Handler
 */
@Slf4j
@Component
class BlacklistClearHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        
        try {
            long count = blacklistService.count();
            blacklistService.clearAll();
            
            return buildEditMessage(
                    callbackQuery,
                    String.format("✅ 黑名单已清空\n\n已删除 %d 个IP", count),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "ip_blacklist_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to clear blacklist", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 清空失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "ip_blacklist_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "blacklist_clear";
    }
}
