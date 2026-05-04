package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;

/**
 * 任务详情回调处理器
 * 
 * @author yohann
 */
@Slf4j
@Component
public class TaskDetailsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        TelegramBotService botService = SpringUtil.getBean(TelegramBotService.class);
        String result = botService.getTaskDetails();
        
        return buildEditMessage(
                callbackQuery,
                result,
                new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "task_details";
    }
}
