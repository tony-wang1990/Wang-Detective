package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.params.oci.volume.BootVolumePageParams;
import com.tony.kingdetective.bean.params.oci.volume.TerminateBootVolumeParams;
import com.tony.kingdetective.bean.response.oci.volume.BootVolumeListPage;
import com.tony.kingdetective.service.IBootVolumeService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.BootVolumeSelectionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Boot volume management handler
 * 
 * @author yohann
 */
@Slf4j
@Component
public class BootVolumeManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String ociCfgId = callbackData.split(":")[1];
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Set config context
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        storage.setConfigContext(chatId, ociCfgId);
        storage.clearSelection(chatId); // Clear previous selections
        
        // Get boot volumes
        IBootVolumeService bootVolumeService = SpringUtil.getBean(IBootVolumeService.class);
        
        try {
            BootVolumePageParams params = new BootVolumePageParams();
            params.setOciCfgId(ociCfgId);
            params.setCurrentPage(1);
            params.setPageSize(100); // Get all volumes
            params.setCleanReLaunch(false);
            
            Page<BootVolumeListPage.BootVolumeInfo> page = bootVolumeService.bootVolumeListPage(params);
            List<BootVolumeListPage.BootVolumeInfo> volumes = page.getRecords();
            
            if (CollectionUtil.isEmpty(volumes)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 暂无引导卷",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            // Cache volumes for index-based access
            storage.setVolumeCache(chatId, volumes);
            
            return buildVolumeListMessage(callbackQuery, volumes, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list boot volumes for ociCfgId: {}", ociCfgId, e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取引导卷列表失败：" + e.getMessage(),
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
     * Build boot volume list message
     */
    private BotApiMethod<? extends Serializable> buildVolumeListMessage(
            CallbackQuery callbackQuery,
            List<BootVolumeListPage.BootVolumeInfo> volumes,
            String ociCfgId,
            long chatId) {
        
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        
        StringBuilder message = new StringBuilder("【引导卷管理】\n\n");
        message.append(String.format("共 %d 个引导卷：\n\n", volumes.size()));
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Add volume buttons (using index instead of full volume ID)
        for (int i = 0; i < volumes.size(); i++) {
            BootVolumeListPage.BootVolumeInfo volume = volumes.get(i);
            boolean isSelected = storage.isSelected(chatId, volume.getId());
            
            message.append(String.format(
                    "%s %d. %s\n" +
                    "   状态: %s\n" +
                    "   大小: %sGB | VPUs: %s\n" +
                    "   可用域: %s\n" +
                    "   已附加: %s%s\n\n",
                    isSelected ? "☑️" : "⬜",
                    i + 1,
                    volume.getDisplayName(),
                    volume.getLifecycleState(),
                    volume.getSizeInGBs(),
                    volume.getVpusPerGB(),
                    volume.getAvailabilityDomain(),
                    volume.getAttached() ? "是" : "否",
                    volume.getAttached() && volume.getInstanceName() != null ? " (" + volume.getInstanceName() + ")" : ""
            ));
            
            // Add button (2 per row) - use index instead of full ID
            if (i % 2 == 0) {
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("%s 卷%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_boot_volume:" + i  // Use index
                ));
                keyboard.add(row);
            } else {
                keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                        String.format("%s 卷%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_boot_volume:" + i  // Use index
                ));
            }
        }
        
        // Add batch operation buttons
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("✅ 全选", "select_all_boot_volumes"),
                KeyboardBuilder.button("⬜ 取消全选", "deselect_all_boot_volumes")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🔄 刷新列表", "refresh_boot_volumes")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🗑 终止选中的引导卷", "confirm_terminate_boot_volumes")
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
        return "boot_volume_management:";
    }
}

/**
 * Toggle boot volume selection handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class ToggleBootVolumeHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int volumeIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        
        // Get volume by index
        BootVolumeListPage.BootVolumeInfo volume = storage.getVolumeByIndex(chatId, volumeIndex);
        if (volume == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("引导卷不存在")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        boolean isSelected = storage.toggleVolume(chatId, volume.getId());
        
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
        
        // Refresh volume list
        return refreshVolumeList(callbackQuery, chatId);
    }
    
    /**
     * Refresh boot volume list
     */
    public BotApiMethod<? extends Serializable> refreshVolumeList(CallbackQuery callbackQuery, long chatId) {
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (ociCfgId == null) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 配置上下文丢失，请重新进入引导卷管理",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        // Get cached volumes
        List<BootVolumeListPage.BootVolumeInfo> volumes = storage.getCachedVolumes(chatId);
        
        if (CollectionUtil.isEmpty(volumes)) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 引导卷缓存丢失，请重新进入引导卷管理",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
        
        return buildVolumeListMessage(callbackQuery, volumes, ociCfgId, chatId);
    }
    
    /**
     * Build boot volume list message
     */
    private BotApiMethod<? extends Serializable> buildVolumeListMessage(
            CallbackQuery callbackQuery,
            List<BootVolumeListPage.BootVolumeInfo> volumes,
            String ociCfgId,
            long chatId) {
        
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        
        StringBuilder message = new StringBuilder("【引导卷管理】\n\n");
        message.append(String.format("共 %d 个引导卷：\n\n", volumes.size()));
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Add volume buttons
        for (int i = 0; i < volumes.size(); i++) {
            BootVolumeListPage.BootVolumeInfo volume = volumes.get(i);
            boolean isSelected = storage.isSelected(chatId, volume.getId());
            
            message.append(String.format(
                    "%s %d. %s\n" +
                    "   状态: %s\n" +
                    "   大小: %sGB | VPUs: %s\n" +
                    "   可用域: %s\n" +
                    "   已附加: %s%s\n\n",
                    isSelected ? "☑️" : "⬜",
                    i + 1,
                    volume.getDisplayName(),
                    volume.getLifecycleState(),
                    volume.getSizeInGBs(),
                    volume.getVpusPerGB(),
                    volume.getAvailabilityDomain(),
                    volume.getAttached() ? "是" : "否",
                    volume.getAttached() && volume.getInstanceName() != null ? " (" + volume.getInstanceName() + ")" : ""
            ));
            
            // Add button (2 per row) - use index instead of full ID
            if (i % 2 == 0) {
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("%s 卷%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_boot_volume:" + i
                ));
                keyboard.add(row);
            } else {
                keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                        String.format("%s 卷%d", isSelected ? "☑️" : "⬜", i + 1),
                        "toggle_boot_volume:" + i
                ));
            }
        }
        
        // Add batch operation buttons
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("✅ 全选", "select_all_boot_volumes"),
                KeyboardBuilder.button("⬜ 取消全选", "deselect_all_boot_volumes")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🔄 刷新列表", "refresh_boot_volumes")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🗑 终止选中的引导卷", "confirm_terminate_boot_volumes")
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
        return "toggle_boot_volume:";
    }
}

/**
 * Select all boot volumes handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class SelectAllBootVolumesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        
        // Get cached volumes and select all
        List<BootVolumeListPage.BootVolumeInfo> volumes = storage.getCachedVolumes(chatId);
        
        if (CollectionUtil.isNotEmpty(volumes)) {
            volumes.forEach(volume -> storage.selectVolume(chatId, volume.getId()));
            
            // Answer callback query
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text(String.format("已全选 %d 个引导卷", volumes.size()))
                        .showAlert(false)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
        }
        
        // Refresh volume list
        ToggleBootVolumeHandler handler = SpringUtil.getBean(ToggleBootVolumeHandler.class);
        return handler.refreshVolumeList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "select_all_boot_volumes";
    }
}

/**
 * Deselect all boot volumes handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class DeselectAllBootVolumesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
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
        
        // Refresh volume list
        ToggleBootVolumeHandler handler = SpringUtil.getBean(ToggleBootVolumeHandler.class);
        return handler.refreshVolumeList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "deselect_all_boot_volumes";
    }
}

/**
 * Confirm terminate boot volumes handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class ConfirmTerminateBootVolumesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        Set<String> selectedVolumes = storage.getSelectedVolumes(chatId);
        
        if (selectedVolumes.isEmpty()) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("请先选择要终止的引导卷")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        // Show confirmation dialog
        List<InlineKeyboardRow> keyboard = List.of(
                new InlineKeyboardRow(
                        KeyboardBuilder.button("✅ 确认终止", "execute_terminate_boot_volumes")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("◀️ 返回", "boot_volume_management:" + storage.getConfigContext(chatId))
                ),
                KeyboardBuilder.buildCancelRow()
        );
        
        String message = String.format(
                "【确认终止引导卷】\n\n" +
                "⚠️ 您选择了 %d 个引导卷，即将终止这些引导卷。\n\n" +
                "⚠️ 注意：此操作不可逆！\n" +
                "引导卷一旦终止将无法恢复，请确认！",
                selectedVolumes.size()
        );
        
        return buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_terminate_boot_volumes";
    }
}

/**
 * Execute terminate boot volumes handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class ExecuteTerminateBootVolumesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        Set<String> selectedVolumes = storage.getSelectedVolumes(chatId);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (selectedVolumes.isEmpty()) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("没有选中的引导卷")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        if (ociCfgId == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("配置上下文丢失")
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
                    .text("正在终止引导卷...")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Delete the confirmation message
        try {
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(Math.toIntExact(callbackQuery.getMessage().getMessageId()))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
        
        // Send processing message
        String processingMessage = String.format(
                "⏳ 正在终止 %d 个引导卷...\n\n请稍候，任务已提交...",
                selectedVolumes.size()
        );
        
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(processingMessage)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send processing message", e);
        }
        
        // Terminate boot volumes
        IBootVolumeService bootVolumeService = SpringUtil.getBean(IBootVolumeService.class);
        
        try {
            TerminateBootVolumeParams params = new TerminateBootVolumeParams();
            params.setOciCfgId(ociCfgId);
            params.setBootVolumeIds(new ArrayList<>(selectedVolumes));
            
            bootVolumeService.terminateBootVolume(params);
            
            // Send success message
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("✅ 已成功提交终止 %d 个引导卷的任务！", selectedVolumes.size()))
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to send success message", e);
            }
            
        } catch (Exception e) {
            log.error("Failed to terminate boot volumes", e);
            
            // Send error message
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("❌ 终止引导卷失败：" + e.getMessage())
                        .build());
            } catch (TelegramApiException ex) {
                log.error("Failed to send error message", ex);
            }
        }
        
        // Clear all data (selection, context, and cache)
        storage.clearAll(chatId);
        
        return null;
    }
    
    @Override
    public String getCallbackPattern() {
        return "execute_terminate_boot_volumes";
    }
}

/**
 * Refresh boot volumes handler
 * 
 * @author yohann
 */
@Slf4j
@Component
class RefreshBootVolumesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        BootVolumeSelectionStorage storage = BootVolumeSelectionStorage.getInstance();
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (ociCfgId == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("配置上下文丢失，请重新进入引导卷管理")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        // Answer callback query first
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("正在刷新引导卷列表...")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Get boot volumes with cleanReLaunch=true to force refresh
        IBootVolumeService bootVolumeService = SpringUtil.getBean(IBootVolumeService.class);
        
        try {
            BootVolumePageParams params = new BootVolumePageParams();
            params.setOciCfgId(ociCfgId);
            params.setCurrentPage(1);
            params.setPageSize(100); // Get all volumes
            params.setCleanReLaunch(true); // Force refresh cache
            
            Page<BootVolumeListPage.BootVolumeInfo> page = bootVolumeService.bootVolumeListPage(params);
            List<BootVolumeListPage.BootVolumeInfo> volumes = page.getRecords();
            
            if (CollectionUtil.isEmpty(volumes)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 暂无引导卷",
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
            storage.setVolumeCache(chatId, volumes);
            
            // Build message with refresh timestamp
            BootVolumeSelectionStorage storage2 = BootVolumeSelectionStorage.getInstance();
            
            StringBuilder message = new StringBuilder("【引导卷管理】\n\n");
            message.append(String.format("共 %d 个引导卷：\n", volumes.size()));
            message.append("🔄 刷新时间: ");
            message.append(java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            message.append("\n\n");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            // Add volume buttons (using index instead of full volume ID)
            for (int i = 0; i < volumes.size(); i++) {
                BootVolumeListPage.BootVolumeInfo volume = volumes.get(i);
                boolean isSelected = storage2.isSelected(chatId, volume.getId());
                
                message.append(String.format(
                        "%s %d. %s\n" +
                        "   状态: %s\n" +
                        "   大小: %sGB | VPUs: %s\n" +
                        "   可用域: %s\n" +
                        "   已附加: %s%s\n\n",
                        isSelected ? "☑️" : "⬜",
                        i + 1,
                        volume.getDisplayName(),
                        volume.getLifecycleState(),
                        volume.getSizeInGBs(),
                        volume.getVpusPerGB(),
                        volume.getAvailabilityDomain(),
                        volume.getAttached() ? "是" : "否",
                        volume.getAttached() && volume.getInstanceName() != null ? " (" + volume.getInstanceName() + ")" : ""
                ));
                
                // Add button (2 per row) - use index instead of full ID
                if (i % 2 == 0) {
                    InlineKeyboardRow row = new InlineKeyboardRow();
                    row.add(KeyboardBuilder.button(
                            String.format("%s 卷%d", isSelected ? "☑️" : "⬜", i + 1),
                            "toggle_boot_volume:" + i  // Use index
                    ));
                    keyboard.add(row);
                } else {
                    keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                            String.format("%s 卷%d", isSelected ? "☑️" : "⬜", i + 1),
                            "toggle_boot_volume:" + i  // Use index
                    ));
                }
            }
            
            // Add batch operation buttons
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("✅ 全选", "select_all_boot_volumes"),
                    KeyboardBuilder.button("⬜ 取消全选", "deselect_all_boot_volumes")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🔄 刷新列表", "refresh_boot_volumes")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("🗑 终止选中的引导卷", "confirm_terminate_boot_volumes")
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
            log.error("Failed to refresh boot volumes for ociCfgId: {}", ociCfgId, e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 刷新失败：" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🔄 重试", "refresh_boot_volumes")
                            ),
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "refresh_boot_volumes";
    }
}
