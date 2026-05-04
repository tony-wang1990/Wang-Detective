package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Account management handler - manage OCI accounts (CRUD)
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AccountManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            List<OciUser> users = userService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("【账户管理】\n\n");
            
            if (CollectionUtil.isEmpty(users)) {
                message.append("暂无OCI账户\n\n");
                message.append("💡 请先添加账户");
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("➕ 添加账户", "account_add")
                                ),
                                KeyboardBuilder.buildBackToMainMenuRow(),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            message.append(String.format("共 %d 个账户\n\n", users.size()));
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            for (int i = 0; i < users.size(); i++) {
                OciUser user = users.get(i);
                String status = (user.getDeleted() != null && user.getDeleted() == 1) ? "❌ 已禁用" : "✅ 正常";
                
                message.append(String.format(
                        "%d. %s\n" +
                        "   状态: %s\n" +
                        "   区域: %s\n" +
                        "   租户ID: ...%s\n\n",
                        i + 1,
                        user.getUsername(),
                        status,
                        user.getOciRegion(),
                        user.getTenantId().substring(Math.max(0, user.getTenantId().length() - 8))
                ));
                
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("⚙️ 账户%d", i + 1),
                        "account_detail:" + user.getId()
                ));
                keyboard.add(row);
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("➕ 添加新账户", "account_add"),
                    KeyboardBuilder.button("🔄 刷新列表", "account_management")
            ));
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to list accounts", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取账户列表失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_management";
    }
}

/**
 * Account detail handler
 */
@Slf4j
@Component
class AccountDetailHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String accountId = callbackData.split(":")[1];
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            
            if (user == null) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 账户不存在",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("【账户详情】\n\n");
            message.append(String.format("账户名: %s\n", user.getUsername()));
            message.append(String.format("状态: %s\n", (user.getDeleted() != null && user.getDeleted() == 1) ? "❌ 已禁用" : "✅ 正常"));
            message.append(String.format("主区域: %s\n", user.getOciRegion()));
            message.append(String.format("租户ID: %s\n", user.getTenantId()));
            message.append(String.format("用户ID: %s\n", user.getUserId()));
            message.append(String.format("指纹: %s\n", user.getFingerprint()));
            message.append(String.format("创建时间: %s\n", user.getCreateTime()));
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            // Enable/Disable button
            if (user.getDeleted() != null && user.getDeleted() == 1) {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("✅ 启用账户", "account_enable:" + accountId)
                ));
            } else {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("❌ 禁用账户", "account_disable:" + accountId)
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🗑 删除API配置", "delete_api_config:" + accountId)
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🗑 删除账户", "account_delete_confirm:" + accountId)
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("◀️ 返回", "account_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get account detail", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取账户详情失败: " + e.getMessage(),
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
        return "account_detail:";
    }
}

/**
 * Enable account handler
 */
@Slf4j
@Component
class AccountEnableHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            if (user != null) {
                user.setDeleted(0);
                userService.updateById(user);
                
                return buildEditMessage(
                        callbackQuery,
                        String.format("✅ 账户 %s 已启用", user.getUsername()),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回详情", "account_detail:" + accountId)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回列表", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to enable account", e);
        }
        
        return buildEditMessage(
                callbackQuery,
                "❌ 启用失败",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("◀️ 返回", "account_management")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_enable:";
    }
}

/**
 * Disable account handler
 */
@Slf4j
@Component
class AccountDisableHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            if (user != null) {
                user.setDeleted(1);
                userService.updateById(user);
                
                return buildEditMessage(
                        callbackQuery,
                        String.format("✅ 账户 %s 已禁用", user.getUsername()),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回详情", "account_detail:" + accountId)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回列表", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to disable account", e);
        }
        
        return buildEditMessage(
                callbackQuery,
                "❌ 禁用失败",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("◀️ 返回", "account_management")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_disable:";
    }
}

/**
 * Delete account confirmation handler
 */
@Slf4j
@Component
class AccountDeleteConfirmHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "⚠️ 确认删除账户？\n\n" +
                            "账户: %s\n" +
                            "区域: %s\n\n" +
                            "此操作不可恢复！",
                            user.getUsername(),
                            user.getOciRegion()
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🗑 确认删除", "account_delete:" + accountId),
                                    KeyboardBuilder.button("❌ 取消", "account_detail:" + accountId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to show delete confirmation", e);
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
        return "account_delete_confirm:";
    }
}

/**
 * Delete account handler
 */
@Slf4j
@Component
class AccountDeleteHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            String username = user.getUsername();
            
            userService.removeById(accountId);
            
            return buildEditMessage(
                    callbackQuery,
                    String.format("✅ 账户 %s 已删除", username),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回列表", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to delete account", e);
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
        return "account_delete:";
    }
}
