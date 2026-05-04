package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IOciKvService;
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
 * Report time setting handler
 * Handles the 'report_time:HH' callback
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class ReportTimeHandler extends AbstractCallbackHandler {
    
    public static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String timeStr = callbackData.split(":")[1];
        
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            if (timeKv == null) {
                timeKv = new OciKv();
                timeKv.setCode(REPORT_TIME_KEY);
                timeKv.setValue(timeStr);
                timeKv.setType("SYSTEM");
                kvService.save(timeKv);
            } else {
                timeKv.setValue(timeStr);
                kvService.updateById(timeKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    String.format("✅ 报告时间已更新\n\n新的发送时间: 每天 %s:00", timeStr),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to update report time", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 设置失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "report_time:";
    }
}
