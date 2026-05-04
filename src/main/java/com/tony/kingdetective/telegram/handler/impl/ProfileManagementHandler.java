package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.io.FileUtil;
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

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile management handler - manage OCI CLI profiles
 * Generate and view OCI CLI config files
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class ProfileManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            List<OciUser> users = userService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("【Profile管理】\n\n");
            message.append("OCI CLI 配置文件管理\n\n");
            
            if (users.isEmpty()) {
                message.append("暂无账户配置");
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                KeyboardBuilder.buildBackToMainMenuRow(),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            message.append(String.format("共 %d 个Profile\n\n", users.size()));
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            for (int i = 0; i < users.size(); i++) {
                OciUser user = users.get(i);
                message.append(String.format(
                        "%d. [%s]\n" +
                        "   区域: %s\n" +
                        "   状态: %s\n\n",
                        i + 1,
                        user.getUsername(),
                        user.getOciRegion(),
                        user.getDeleted() != null && user.getDeleted() == 1 ? "禁用" : "正常"
                ));
                
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button(
                                String.format("📄 Profile%d", i + 1),
                                "profile_detail:" + user.getId()
                        )
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("📦 生成完整配置", "profile_generate_all")
            ));
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to list profiles", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取Profile列表失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "profile_management";
    }
}

/**
 * Profile detail handler - show individual profile config
 */
@Slf4j
@Component
class ProfileDetailHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String userId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(userId);
            
            if (user == null) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ Profile不存在",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "profile_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            String profileConfig = generateProfileConfig(user);
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("【Profile: %s】\n\n", user.getUsername()));
            message.append("OCI CLI 配置:\n\n");
            message.append("```ini\n");
            message.append(profileConfig);
            message.append("\n```\n\n");
            message.append("💡 将以上内容保存到 ~/.oci/config\n");
            message.append("💡 私钥保存到 ~/.oci/oci_api_key.pem");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🔑 查看私钥", "profile_show_key:" + userId)
                            ),
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "profile_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to get profile detail", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取Profile详情失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "profile_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    private String generateProfileConfig(OciUser user) {
        return String.format(
                "[%s]\n" +
                "user=%s\n" +
                "fingerprint=%s\n" +
                "tenancy=%s\n" +
                "region=%s\n" +
                "key_file=~/.oci/oci_api_key.pem",
                user.getUsername(),
                user.getUserId(),
                user.getFingerprint(),
                user.getTenantId(),
                user.getOciRegion()
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "profile_detail:";
    }
}

/**
 * Show private key handler
 */
@Slf4j
@Component
class ProfileShowKeyHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String userId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(userId);
            
            if (user == null || user.getPrivateKey() == null) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 私钥不存在",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "profile_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("【%s - 私钥】\n\n", user.getUsername()));
            message.append("⚠️ 请妥善保管私钥\n\n");
            message.append("```\n");
            message.append(user.getPrivateKey());
            message.append("\n```\n\n");
            message.append("💾 保存为 ~/.oci/oci_api_key.pem\n");
            message.append("🔒 设置权限: chmod 600 ~/.oci/oci_api_key.pem");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回详情", "profile_detail:" + userId)
                            ),
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回列表", "profile_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to show private key", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取私钥失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "profile_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "profile_show_key:";
    }
}

/**
 * Generate all profiles config handler
 */
@Slf4j
@Component
class ProfileGenerateAllHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            List<OciUser> users = userService.list();
            
            StringBuilder configContent = new StringBuilder();
            
            for (OciUser user : users) {
                if (user.getDeleted() != null && user.getDeleted() == 1) {
                    continue; // Skip disabled accounts
                }
                
                configContent.append(String.format(
                        "[%s]\n" +
                        "user=%s\n" +
                        "fingerprint=%s\n" +
                        "tenancy=%s\n" +
                        "region=%s\n" +
                        "key_file=~/.oci/%s_key.pem\n\n",
                        user.getUsername(),
                        user.getUserId(),
                        user.getFingerprint(),
                        user.getTenantId(),
                        user.getOciRegion(),
                        user.getUsername()
                ));
            }
            
            StringBuilder message = new StringBuilder();
            message.append("【完整OCI配置文件】\n\n");
            message.append(String.format("包含 %d 个Profile\n\n", users.size()));
            message.append("```ini\n");
            message.append(configContent.toString());
            message.append("```\n\n");
            message.append("💾 保存为 ~/.oci/config\n");
            message.append("💡 每个账户的私钥需单独查看");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "profile_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to generate all profiles", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 生成配置失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "profile_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "profile_generate_all";
    }
}
