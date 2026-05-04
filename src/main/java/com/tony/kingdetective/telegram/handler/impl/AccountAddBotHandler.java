package com.tony.kingdetective.telegram.handler.impl;

import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
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
 * Handler for initiating Bot-based account addition
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AccountAddBotHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Start the config session
        ConfigSessionStorage.getInstance().startAddAccountConfig(chatId);
        
        String message = "📝 *第一步：输入 OCI 配置*\n\n" +
                "请复制 `~/.oci/config` 文件的内容并发送给机器人。\n\n" +
                "格式示例：\n" +
                "```ini\n" +
                "[DEFAULT]\n" +
                "user=ocid1.user.oc1..aaaa...\n" +
                "fingerprint=1a:d7:...\n" +
                "tenancy=ocid1.tenancy.oc1..aaaa...\n" +
                "region=us-sanjose-1\n" +
                "```\n\n" +
                "💡 *提示*: 发送 `/cancel` 可随时取消。";
        
        return buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(List.of(
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_add_bot";
    }
}
