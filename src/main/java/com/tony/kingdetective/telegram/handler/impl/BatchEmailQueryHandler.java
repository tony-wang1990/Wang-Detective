package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.identity.model.User;
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
import java.util.List;

/**
 * Batch email query handler - shows email addresses for all OCI accounts
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class BatchEmailQueryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            if (CollectionUtil.isEmpty(users)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置\n\n请先添加 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("【批量邮箱查询】\n\n");
            message.append(String.format("共 %d 个 OCI 账户\n\n", users.size()));
            message.append("━━━━━━━━━━━━━━━━\n\n");
            
            int successCount = 0;
            int failCount = 0;
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    // Get user info from OCI
                    User ociUser = fetcher.getUserInfo();
                    
                    String email = ociUser.getEmail();
                    String userName = ociUser.getName();
                    String description = ociUser.getDescription();
                    
                    message.append(String.format("✅ %s\n", user.getUsername()));
                    message.append(String.format("   区域: %s\n", user.getOciCfg().getRegion()));
                    
                    if (email != null && !email.isEmpty()) {
                        message.append(String.format("   📧 邮箱: %s\n", email));
                    } else {
                        message.append("   📧 邮箱: 未设置\n");
                    }
                    
                    if (userName != null && !userName.isEmpty()) {
                        message.append(String.format("   👤 用户名: %s\n", userName));
                    }
                    
                    if (description != null && !description.isEmpty()) {
                        message.append(String.format("   📝 描述: %s\n", description));
                    }
                    
                    message.append("\n");
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to query email for user: {}", user.getUsername(), e);
                    message.append(String.format("❌ %s\n", user.getUsername()));
                    message.append(String.format("   查询失败: %s\n\n", e.getMessage()));
                    failCount++;
                }
            }
            
            message.append("━━━━━━━━━━━━━━━━\n");
            message.append(String.format("📊 成功: %d / 失败: %d\n", successCount, failCount));
            message.append("\n💡 数据来自 OCI Identity API");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to query batch email", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 查询失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "batch_email_query";
    }
}
