package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.service.ISysService;
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
 * IPv6 config selection handler - select which OCI config to use for IPv6 management
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class Ipv6ConfigSelectHandler extends AbstractCallbackHandler {
    
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
            message.append("【IPv6 管理】\n\n");
            message.append("请选择要管理的 OCI 配置：\n\n");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            for (SysUserDTO user : users) {
                message.append(String.format(
                        "📌 %s\n" +
                        "   区域: %s\n\n",
                        user.getUsername(),
                        user.getOciCfg().getRegion()
                ));
                
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button(
                                user.getUsername() + " (" + user.getOciCfg().getRegion() + ")",
                                "ipv6_management:" + user.getOciCfg().getId()
                        )
                ));
            }
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to list OCI configs", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取配置列表失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ipv6_config_select";
    }
}
