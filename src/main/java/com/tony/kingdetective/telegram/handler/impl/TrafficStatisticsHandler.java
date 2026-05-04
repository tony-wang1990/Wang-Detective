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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Traffic statistics callback handler
 * 
 * @author yohann
 */
@Slf4j
@Component
public class TrafficStatisticsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        // Show confirmation dialog
        return buildEditMessage(
                callbackQuery,
                "⚠️ 确认查询流量统计？\n\n此操作将获取所有OCI配置的流量使用情况。",
                InlineKeyboardMarkup.builder()
                        .keyboard(Arrays.asList(
                                new InlineKeyboardRow(
                                        InlineKeyboardButton.builder()
                                                .text("✅ 确认查询")
                                                .callbackData("traffic_statistics_confirm")
                                                .build(),
                                        InlineKeyboardButton.builder()
                                                .text("❌ 取消")
                                                .callbackData("back_to_main")
                                                .build()
                                )
                        ))
                        .build()
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "traffic_statistics";
    }
}

/**
 * Traffic statistics confirmation handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class TrafficStatisticsConfirmHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        TelegramBotService botService = SpringUtil.getBean(TelegramBotService.class);
        String result = botService.getTrafficStatistics();
        
        // Build keyboard with refresh button
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("🔄 刷新", "traffic_statistics_confirm")
                        ),
                        KeyboardBuilder.buildBackToMainMenuRow(),
                        KeyboardBuilder.buildCancelRow()
                ))
                .build();
        
        return buildEditMessage(
                callbackQuery,
                result,
                keyboard
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "traffic_statistics_confirm";
    }
}
