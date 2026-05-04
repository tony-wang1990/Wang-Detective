package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
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
 * Delete OCI API configuration handler
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
class DeleteApiConfigHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            
            if (user == null) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 配置不存在",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "⚠️ 确认删除API配置？\n\n" +
                            "账户: %s\n" +
                            "区域: %s\n" +
                            "租户ID: ...%s\n\n" +
                            "此操作将:\n" +
                            "• 删除OCI API配置\n" +
                            "• 删除所有关联数据\n" +
                            "• 此操作不可恢复！",
                            user.getUsername(),
                            user.getOciRegion(),  
                            user.getTenantId().substring(Math.max(0, user.getTenantId().length() - 8))
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🗑 确认删除", "confirm_delete_api:" + accountId),
                                    KeyboardBuilder.button("❌ 取消", "account_detail:" + accountId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to show delete API confirmation", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 操作失败",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "delete_api_config:";
    }
}

/**
 * Confirm delete API config handler
 */
@Slf4j
@Component
class ConfirmDeleteApiHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            String username = user.getUsername();
            
            // Delete from database
            userService.removeById(accountId);
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "✅ API配置已删除\n\n" +
                            "账户: %s\n\n" +
                            "已删除:\n" +
                            "• OCI API配置\n" +
                            "• 租户信息\n" +
                            "• 用户凭证\n" +
                            "• 关联数据\n\n" +
                            "💡 如需重新添加，请使用账户管理功能",
                            username
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回列表", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to delete API config", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 删除失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_delete_api:";
    }
}
