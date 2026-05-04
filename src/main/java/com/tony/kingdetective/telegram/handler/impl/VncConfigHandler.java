package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
 * VNC Configuration Management Handler
 * Handles VNC URL configuration for instance connections
 * 
 * @author yohann
 */
@Slf4j
@Component
public class VncConfigHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        // Get current VNC configuration
        LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OciKv::getCode, SysCfgEnum.SYS_VNC.getCode());
        OciKv vncConfig = kvService.getOne(wrapper);
        
        boolean hasConfig = vncConfig != null && StringUtils.isNotBlank(vncConfig.getValue());
        
        String text;
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        if (hasConfig) {
            String vncUrl = vncConfig.getValue().trim();
            text = String.format(
                "🔧 *VNC 配置管理*\n\n" +
                "📌 当前配置：\n" +
                "• VNC URL: %s\n" +
                "• 状态: ✅ 已配置\n\n" +
                "💡 使用说明：\n" +
                "此 URL 将用于所有实例的 VNC 连接。\n" +
                "当在实例管理中选择一个实例并点击\"开启VNC连接\"时，\n" +
                "系统会使用此 URL 生成完整的 VNC 连接地址。\n\n" +
                "⚙️ URL 格式说明：\n" +
                "• IP格式: http://IP:端口 (自动使用 /vnc.html)\n" +
                "• 域名HTTP: http://domain.com (使用 /vnc.html)\n" +
                "• 域名HTTPS: https://domain.com (使用 /myvnc/vnc.html)\n\n" +
                "📝 示例：\n" +
                "• http://192.168.1.100:6080\n" +
                "• http://vnc.example.com\n" +
                "• https://vnc.example.com\n\n" +
                "⚠️ 注意：\n" +
                "• 不要在 URL 末尾添加斜杠\n" +
                "• 确保 VNC 代理服务已正确配置\n" +
                "• 如果不配置，将自动使用宿主机IP:6080\n\n" +
                "⚙️ 请选择功能：",
                vncUrl
            );
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🔄 重新配置", "vnc_setup")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🗑️ 删除配置", "vnc_delete")
            ));
        } else {
            text = "🔧 *VNC 配置管理*\n\n" +
                   "📝 当前没有配置 VNC URL\n\n" +
                   "💡 使用说明：\n" +
                   "配置 VNC URL 后，在实例管理中开启 VNC 连接时，\n" +
                   "系统会使用此 URL 作为 VNC 代理地址。\n\n" +
                   "⚙️ URL 格式说明：\n" +
                   "• IP格式: http://IP:端口 (自动使用 /vnc.html)\n" +
                   "• 域名HTTP: http://domain.com (使用 /vnc.html)\n" +
                   "• 域名HTTPS: https://domain.com (使用 /myvnc/vnc.html)\n\n" +
                   "📝 示例：\n" +
                   "• http://192.168.1.100:6080\n" +
                   "• http://vnc.example.com\n" +
                   "• https://vnc.example.com\n\n" +
                   "⚠️ 注意：\n" +
                   "• 不要在 URL 末尾添加斜杠\n" +
                   "• 如果不配置，将自动使用宿主机公网IP:6080\n" +
                   "• 确保 VNC 代理服务（如 noVNC）已正确部署\n\n" +
                   "⚙️ 请选择功能：";
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("➕ 配置 VNC URL", "vnc_setup")
            ));
        }
        
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "vnc_config";
    }
}

/**
 * VNC Setup Handler
 * Prompts user to enter VNC URL
 */
@Component
class VncSetupHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VncSetupHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Mark this chat as configuring VNC
        ConfigSessionStorage.getInstance().startVncConfig(chatId);
        
        String text = "🔧 *配置 VNC URL*\n\n" +
                     "请直接发送 VNC URL（不需要命令前缀）：\n\n" +
                     "📝 格式示例：\n" +
                     "• http://192.168.1.100:6080\n" +
                     "• http://vnc.example.com\n" +
                     "• https://vnc.example.com\n\n" +
                     "⚠️ 注意事项：\n" +
                     "• 必须以 http:// 或 https:// 开头\n" +
                     "• 不要在末尾添加斜杠或路径\n" +
                     "• 端口号是可选的（默认80/443）\n" +
                     "• URL 格式会影响 VNC 路径选择：\n" +
                     "  - IP格式 → /vnc.html\n" +
                     "  - HTTP域名 → /vnc.html\n" +
                     "  - HTTPS域名 → /myvnc/vnc.html\n\n" +
                     "💡 提示：\n" +
                     "发送 /cancel 可取消配置";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("◀️ 返回", "vnc_config")
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "vnc_setup";
    }
}

/**
 * VNC Delete Handler
 * Deletes VNC configuration
 */
@Component
class VncDeleteHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VncDeleteHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Delete VNC configuration
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OciKv::getCode, SysCfgEnum.SYS_VNC.getCode());
            kvService.remove(wrapper);
            
            log.info("VNC configuration deleted");
            
            String text = "✅ *VNC 配置已删除*\n\n" +
                         "系统将使用默认配置（宿主机公网IP:6080）\n\n" +
                         "💡 提示：\n" +
                         "需要时可以重新配置 VNC URL";
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "vnc_config")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to delete VNC configuration", e);
            
            String text = "❌ *删除 VNC 配置失败*\n\n" +
                         "错误信息：" + e.getMessage();
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "vnc_config")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "vnc_delete";
    }
}
