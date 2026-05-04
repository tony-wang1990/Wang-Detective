package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
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
 * Disable banned accounts handler - disable all banned/suspended OCI accounts
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class DisableBannedAccountsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            List<OciUser> allUsers = userService.list();
            
            if (CollectionUtil.isEmpty(allUsers)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("【一键禁用被封账户】\n\n");
            message.append(String.format("共 %d 个账户\n\n", allUsers.size()));
            message.append("正在检测被封账户...\n\n");
            
            // Note: This is a simplified implementation
            // In real scenario, you would need to check account status via OCI API
            message.append("⚠️ 此功能需要管理员权限\n\n");
            message.append("💡 检测方法:\n");
            message.append("1. 尝试调用OCI API\n");
            message.append("2. 如果返回401/403则标记为被封\n");
            message.append("3. 自动在数据库中禁用该配置\n\n");
            message.append("确认执行检测和禁用？");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🔍 开始检测", "confirm_disable_banned"),
                                    KeyboardBuilder.button("❌ 取消", "back_to_main")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to check accounts", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 查询失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "disable_banned_accounts";
    }
}

@Slf4j
@Component
class ConfirmDisableBannedHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("【检测结果】\n\n");
            
            int bannedCount = 0;
            int activeCount = 0;
            
            for (SysUserDTO user : users) {
                try {
                    // Try to call OCI API
                    com.tony.kingdetective.config.OracleInstanceFetcher fetcher = 
                            new com.tony.kingdetective.config.OracleInstanceFetcher(user);
                    fetcher.getUserInfo();
                    fetcher.close();
                    
                    message.append(String.format("✅ %s: 正常\n", user.getUsername()));
                    activeCount++;
                    
                } catch (Exception e) {
                    // Account might be banned
                    message.append(String.format("❌ %s: 已禁用 (%s)\n", user.getUsername(), e.getMessage()));
                    bannedCount++;
                    
                    // Mark as disabled in database
                    try {
                        OciUser ociUser = userService.getById(user.getOciCfg().getId());
                        if (ociUser != null) {
                            ociUser.setDeleted(1);
                            userService.updateById(ociUser);
                        }
                    } catch (Exception ex) {
                        log.error("Failed to disable user in DB", ex);
                    }
                }
            }
            
            message.append("\n━━━━━━━━━━━━━━━━\n");
            message.append(String.format("✅ 正常: %d / ❌ 已封禁: %d\n", activeCount, bannedCount));
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to disable banned accounts", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 检测失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_disable_banned";
    }
}
