package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.InstanceSelectionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Instance management handler
 * 
 * @author yohann
 */
@Slf4j
@Component
public class InstanceManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String ociCfgId = callbackData.split(":")[1];
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Set config context
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        storage.setConfigContext(chatId, ociCfgId);
        storage.clearSelection(chatId); // Clear previous selections
        
        // Get running instances
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(sysUserDTO);
            
            if (CollectionUtil.isEmpty(instances)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 暂无运行中的实例",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            // Cache instances for index-based access
            storage.setInstanceCache(chatId, instances);
            
            return buildInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list running instances for ociCfgId: {}", ociCfgId, e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取实例列表失败：" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    /**
     * Build instance list message
     */
    private BotApiMethod<? extends Serializable> buildInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        
        StringBuilder message = new StringBuilder("【实例管理】\n\n");
        message.append(String.format("共 %d 个运行中的实例：\n\n", instances.size()));
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Add instance buttons (using index instead of full instance ID)
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            boolean isSelected = storage.isSelected(chatId, instance.getOcId());
            
            // Format public IPs
            String publicIps = CollectionUtil.isEmpty(instance.getPublicIp()) 
                    ? "无" 
                    : String.join(", ", instance.getPublicIp());
            
            message.append(String.format(
                    "%s %d. %s\n" +
                    "   区域: %s\n" +
                    "   ID: ...%s\n" +
                    "   Shape: %s\n" +
                    "   公网IP: %s\n\n",
                    isSelected ? "☑️" : "⬜",
                    i + 1,
                    instance.getName(),
                    instance.getRegion(),
                    instance.getOcId().substring(Math.max(0, instance.getOcId().length() - 8)), // Show last 8 chars
                    instance.getShape(),
                    publicIps
            ));
            
            // Add button (2 per row) - use index instead of full ID
            if (i % 2 == 0) {
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("%s 实例%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_instance:" + i  // Use index
                ));
                keyboard.add(row);
            } else {
                keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                        String.format("%s 实例%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_instance:" + i  // Use index
                ));
            }
        }
        
        // Add batch operation buttons
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("✅ 全选", "select_all_instances"),
                KeyboardBuilder.button("⬜ 取消全选", "deselect_all_instances")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🔄 刷新列表", "refresh_instances")
        ));
        
        // Add VNC button only when exactly one instance is selected
        java.util.Set<String> selectedInstances = storage.getSelectedInstances(chatId);
        if (selectedInstances != null && selectedInstances.size() == 1) {
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🖥 开启VNC连接", "start_vnc_connection")
            ));
        }
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🗑 终止选中的实例", "confirm_terminate_instances")
        ));
        
        // Back button
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "instance_management:";
    }
}

/**
 * Toggle instance selection handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class ToggleInstanceHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int instanceIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        
        // Get instance by index
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
        if (instance == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("实例不存在")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        boolean isSelected = storage.toggleInstance(chatId, instance.getOcId());
        
        // Answer callback query
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text(isSelected ? "已选中" : "已取消选中")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Refresh instance list
        return refreshInstanceList(callbackQuery, chatId);
    }
    
    /**
     * Refresh instance list
     */
    public BotApiMethod<? extends Serializable> refreshInstanceList(CallbackQuery callbackQuery, long chatId) {
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (ociCfgId == null) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 配置上下文丢失，请重新进入实例管理",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        // Get cached instances
        List<SysUserDTO.CloudInstance> instances = storage.getCachedInstances(chatId);
        
        if (CollectionUtil.isEmpty(instances)) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 实例缓存丢失，请重新进入实例管理",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
        
        return buildInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
    }
    
    /**
     * Build instance list message
     */
    private BotApiMethod<? extends Serializable> buildInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        
        StringBuilder message = new StringBuilder("【实例管理】\n\n");
        message.append(String.format("共 %d 个运行中的实例：\n\n", instances.size()));
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Add instance buttons
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            boolean isSelected = storage.isSelected(chatId, instance.getOcId());
            
            // Format public IPs
            String publicIps = CollectionUtil.isEmpty(instance.getPublicIp()) 
                    ? "无" 
                    : String.join(", ", instance.getPublicIp());
            
            message.append(String.format(
                    "%s %d. %s\n" +
                    "   区域: %s\n" +
                    "   ID: ...%s\n" +
                    "   Shape: %s\n" +
                    "   公网IP: %s\n\n",
                    isSelected ? "☑️" : "⬜",
                    i + 1,
                    instance.getName(),
                    instance.getRegion(),
                    instance.getOcId().substring(Math.max(0, instance.getOcId().length() - 8)),
                    instance.getShape(),
                    publicIps
            ));
            
            // Add button (2 per row) - use index instead of full ID
            if (i % 2 == 0) {
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("%s 实例%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_instance:" + i
                ));
                keyboard.add(row);
            } else {
                keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                        String.format("%s 实例%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_instance:" + i
                ));
            }
        }
        
        // Add batch operation buttons
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("✅ 全选", "select_all_instances"),
                KeyboardBuilder.button("⬜ 取消全选", "deselect_all_instances")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🔄 刷新列表", "refresh_instances")
        ));
        
        // Add VNC button only when exactly one instance is selected
        java.util.Set<String> selectedInstances = storage.getSelectedInstances(chatId);
        if (selectedInstances != null && selectedInstances.size() == 1) {
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🖥 开启VNC连接", "start_vnc_connection")
            ));
        }
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🗑 终止选中的实例", "confirm_terminate_instances")
        ));
        
        // Back button
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "toggle_instance:";
    }
}

/**
 * Select all instances handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class SelectAllInstancesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        
        // Get cached instances and select all
        List<SysUserDTO.CloudInstance> instances = storage.getCachedInstances(chatId);
        
        if (CollectionUtil.isNotEmpty(instances)) {
            instances.forEach(instance -> storage.selectInstance(chatId, instance.getOcId()));
            
            // Answer callback query
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text(String.format("已全选 %d 个实例", instances.size()))
                        .showAlert(false)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
        }
        
        // Refresh instance list
        ToggleInstanceHandler handler = SpringUtil.getBean(ToggleInstanceHandler.class);
        return handler.refreshInstanceList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "select_all_instances";
    }
}

/**
 * Deselect all instances handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class DeselectAllInstancesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        storage.clearSelection(chatId);
        
        // Answer callback query
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("已取消所有选中")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Refresh instance list
        ToggleInstanceHandler handler = SpringUtil.getBean(ToggleInstanceHandler.class);
        return handler.refreshInstanceList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "deselect_all_instances";
    }
}

/**
 * Refresh instances handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class RefreshInstancesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (ociCfgId == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("配置上下文丢失，请重新进入实例管理")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        // Answer callback query
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("正在刷新实例列表...")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Get running instances
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(sysUserDTO);
            
            if (CollectionUtil.isEmpty(instances)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 暂无运行中的实例",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            // Clear previous selections and update cache
            storage.clearSelection(chatId);
            storage.setInstanceCache(chatId, instances);
            
            // Build message with refresh timestamp
            InstanceSelectionStorage storage2 = InstanceSelectionStorage.getInstance();
            
            StringBuilder message = new StringBuilder("【实例管理】\n\n");
            message.append(String.format("共 %d 个运行中的实例：\n", instances.size()));
            message.append("🔄 刷新时间: ");
            message.append(java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            message.append("\n\n");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            // Add instance buttons (using index instead of full instance ID)
            for (int i = 0; i < instances.size(); i++) {
                SysUserDTO.CloudInstance instance = instances.get(i);
                boolean isSelected = storage2.isSelected(chatId, instance.getOcId());
                
                // Format public IPs
                String publicIps = CollectionUtil.isEmpty(instance.getPublicIp()) 
                        ? "无" 
                        : String.join(", ", instance.getPublicIp());
                
                message.append(String.format(
                        "%s %d. %s\n" +
                        "   区域: %s\n" +
                        "   ID: ...%s\n" +
                        "   Shape: %s\n" +
                        "   公网IP: %s\n\n",
                        isSelected ? "☑️" : "⬜",
                        i + 1,
                        instance.getName(),
                        instance.getRegion(),
                        instance.getOcId().substring(Math.max(0, instance.getOcId().length() - 8)), // Show last 8 chars
                        instance.getShape(),
                        publicIps
                ));
                
                // Add button (2 per row) - use index instead of full ID
                if (i % 2 == 0) {
                    InlineKeyboardRow row = new InlineKeyboardRow();
                    row.add(KeyboardBuilder.button(
                            String.format("%s 实例%d", isSelected ? "☑️" : "⬜", i + 1),
                            "toggle_instance:" + i  // Use index
                    ));
                    keyboard.add(row);
                } else {
                    keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                            String.format("%s 实例%d", isSelected ? "☑️" : "⬜", i + 1),
                            "toggle_instance:" + i  // Use index
                    ));
                }
            }
            
            // Add batch operation buttons
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("✅ 全选", "select_all_instances"),
                    KeyboardBuilder.button("⬜ 取消全选", "deselect_all_instances")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🔄 刷新列表", "refresh_instances")
            ));
            
            // Add VNC button only when exactly one instance is selected
            java.util.Set<String> selectedInstances = storage2.getSelectedInstances(chatId);
            if (selectedInstances != null && selectedInstances.size() == 1) {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("🖥 开启VNC连接", "start_vnc_connection")
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🗑 终止选中的实例", "confirm_terminate_instances")
            ));
            
            // Back button
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to refresh instances for ociCfgId: {}", ociCfgId, e);
            
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("刷新失败：" + e.getMessage())
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException ex) {
                log.error("Failed to answer callback query", ex);
            }
            
            return null;
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "refresh_instances";
    }
}

/**
 * Start VNC connection handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class StartVncConnectionHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        
        // Get selected instance
        java.util.Set<String> selectedInstances = storage.getSelectedInstances(chatId);
        
        if (selectedInstances == null || selectedInstances.size() != 1) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("请先选择一个实例")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        String instanceId = selectedInstances.iterator().next();
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (ociCfgId == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("配置上下文丢失，请重新进入实例管理")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        // Get instance info
        SysUserDTO.CloudInstance instance = null;
        java.util.List<SysUserDTO.CloudInstance> instances = storage.getCachedInstances(chatId);
        for (SysUserDTO.CloudInstance inst : instances) {
            if (inst.getOcId().equals(instanceId)) {
                instance = inst;
                break;
            }
        }
        
        if (instance == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("实例不存在")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        // Answer callback query immediately
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("正在启动 VNC 连接...")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Call startVnc service
        com.tony.kingdetective.service.IOciService ociService = SpringUtil.getBean(com.tony.kingdetective.service.IOciService.class);
        com.tony.kingdetective.service.IOciKvService kvService = SpringUtil.getBean(com.tony.kingdetective.service.IOciKvService.class);
        
        try {
            com.tony.kingdetective.bean.params.oci.instance.StartVncParams params = 
                new com.tony.kingdetective.bean.params.oci.instance.StartVncParams();
            params.setOciCfgId(ociCfgId);
            params.setInstanceId(instanceId);
            // compartmentId is optional, will be null
            
            String result = ociService.startVnc(params);
            
                        // Get VNC URL from system config
            // The VNC URL can be configured via Telegram bot "VNC 配置" menu
            // If not configured, will use default (host public IP:6080)
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.tony.kingdetective.bean.entity.OciKv> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(com.tony.kingdetective.bean.entity.OciKv::getCode, com.tony.kingdetective.enums.SysCfgEnum.SYS_VNC.getCode());
            com.tony.kingdetective.bean.entity.OciKv vncConfig = kvService.getOne(wrapper);
            
                        
            // Determine VNC URL strategy:
            // 1. If VNC URL is configured via bot (recommended), use it
            // 2. Otherwise, use default: http://host-public-ip:6080
            String vncUrl;
            boolean isDefaultUrl = false;
            if (vncConfig != null && org.apache.commons.lang3.StringUtils.isNotBlank(vncConfig.getValue())) {
                // Strategy 1: Use configured VNC URL (from "VNC 配置" menu)
                vncUrl = vncConfig.getValue().trim();
                // Ensure no trailing slash
                if (vncUrl.endsWith("/")) {
                    vncUrl = vncUrl.substring(0, vncUrl.length() - 1);
                }
                        } else {
                // Strategy 2: Use default URL with host public IP
                // Note: This gets the host machine's public IP, not the instance's IP
                String hostPublicIp = getHostPublicIpAddress();
                
                if (hostPublicIp == null) {
                    return buildEditMessage(
                            callbackQuery,
                            "❌ 无法获取宿主机公网IP，请稍后重试",
                            new InlineKeyboardMarkup(java.util.List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("◀️ 返回", "instance_management:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                vncUrl = "http://" + hostPublicIp + ":6080";
                isDefaultUrl = true;
            }
            
                        
            // Determine VNC path based on URL format
            // Different URL formats require different VNC viewer paths:
            // - IP:port format (e.g., http://1.2.3.4:6080) → /vnc.html
            // - HTTP domain (e.g., http://vnc.example.com) → /vnc.html
            // - HTTPS domain (e.g., https://vnc.example.com) → /myvnc/vnc.html
            String vncPath;
            if (isDefaultUrl || isIpPortFormat(vncUrl)) {
                // Default or IP:port format → use /vnc.html
                vncPath = "/vnc.html?autoconnect=true";
            } else if (vncUrl.startsWith("https://")) {
                // HTTPS (domain-based) → use /myvnc/vnc.html
                vncPath = "/myvnc/vnc.html?autoconnect=true";
            } else {
                // HTTP (non-IP or domain) → use /vnc.html as fallback
                vncPath = "/vnc.html?autoconnect=true";
            }
            
            String fullVncUrl = vncUrl + vncPath;
            
            // Build success message
            String message = String.format(
                    "✅ VNC 连接已启动\n\n" +
                    "实例: %s\n" +
                    "区域: %s\n" +
                    "ID: ...%s\n\n" +
                    "VNC 连接地址:\n%s\n\n" +
                    "⚠️ 请确保已配置反向代理或放行相应端口",
                    instance.getName(),
                    instance.getRegion(),
                    instanceId.substring(Math.max(0, instanceId.length() - 8)),
                    fullVncUrl
            );
            
            return buildEditMessage(
                    callbackQuery,
                    message,
                    new InlineKeyboardMarkup(java.util.List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回实例列表", "instance_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to start VNC connection for instanceId: {}", instanceId, e);
            
            return buildEditMessage(
                    callbackQuery,
                    "❌ 启动 VNC 连接失败：" + e.getMessage(),
                    new InlineKeyboardMarkup(java.util.List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回实例列表", "instance_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
        /**
     * Get host public IP address from external API
     * 
     * @return host public IP address or null if failed
     */
    private String getHostPublicIpAddress() {
        try {
            // Try multiple services in case one is down
            String[] services = {
                "https://api.ipify.org",
                "https://ifconfig.me/ip",
                "https://icanhazip.com"
            };
            
            for (String service : services) {
                try {
                    java.net.URL url = new java.net.URL(service);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                        String ip = reader.readLine();
                        if (ip != null && !ip.isEmpty()) {
                            return ip.trim();
                        }
                    }
                } catch (Exception e) {
                    // Try next service
                    log.debug("Failed to get IP from {}: {}", service, e.getMessage());
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("Failed to get host public IP address", e);
        }
        return null;
    }
    
    /**
     * Check if URL is in IP:port format (e.g., http://1.2.3.4:8080 or https://192.168.1.1:443)
     * 
     * @param url the URL to check
     * @return true if URL is in IP:port format
     */
    private boolean isIpPortFormat(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        try {
            // Remove protocol
            String host = url;
            if (host.startsWith("http://")) {
                host = host.substring(7);
            } else if (host.startsWith("https://")) {
                host = host.substring(8);
            }
            
            // Check if host contains port
            int colonIndex = host.indexOf(':');
            if (colonIndex > 0) {
                host = host.substring(0, colonIndex);
            }
            
            // Remove any path
            int slashIndex = host.indexOf('/');
            if (slashIndex > 0) {
                host = host.substring(0, slashIndex);
            }
            
            // Check if it's an IP address (simple pattern matching)
            // IPv4: x.x.x.x where x is 0-255
            String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
            return host.matches(ipPattern);
            
        } catch (Exception e) {
            log.warn("Failed to parse URL format: {}", url, e);
            return false;
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "start_vnc_connection";
    }
}
