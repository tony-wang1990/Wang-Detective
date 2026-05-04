package com.tony.kingdetective.telegram.handler.impl;

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
 * Account add handler
 * Guide user to use Web UI for adding accounts
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AccountAddHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String message = "⚠️ *Telegram Bot 暂不支持直接添加账户*\n\n" +
                "由于 OCI 账户配置涉及敏感密钥文件，为了安全起见，请前往 Web 控制台添加：\n\n" +
                "🌐 *Web 控制台*: 使用浏览器访问本服务地址\n" +
                "(登录后点击右上角 '添加账户')\n\n" +
                "✨ *提示*: 添加成功后，可以在 Bot 中看到并管理该账户。";
        
        return buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("🤖 Bot 直接输入", "account_add_bot")
                        ),
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("◀️ 返回账户列表", "account_management")
                        ),
                        KeyboardBuilder.buildBackToMainMenuRow(),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_add";
    }
}
