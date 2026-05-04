package com.tony.kingdetective.telegram.handler.impl;

import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;

/**
 * No operation handler - for category title buttons that don't need any action
 * 
 * @author yohann
 */
@Slf4j
@Component
public class NoopHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        // Do nothing - category title button clicked, just return null
        log.debug("Category title button clicked: {}", callbackQuery.getData());
        return null;
    }
    
    @Override
    public String getCallbackPattern() {
        return "noop";
    }
}
