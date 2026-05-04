package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciCreateTask;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.params.oci.task.StopCreateParams;
import com.tony.kingdetective.service.IOciCreateTaskService;
import com.tony.kingdetective.service.IOciService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.PaginationStorage;
import com.tony.kingdetective.telegram.storage.TaskSelectionStorage;
import com.tony.kingdetective.utils.CommonUtils;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tony.kingdetective.service.impl.OciServiceImpl.TEMP_MAP;

/**
 * 切换任务选择处理器
 * 
 * @author yohann
 */
@Slf4j
@Component
public class ToggleTaskHandler extends AbstractCallbackHandler {
    
    private static final String PAGE_TYPE = "task_management";
    private static final int PAGE_SIZE = 5;
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String taskId = callbackData.split(":")[1];
        long chatId = callbackQuery.getMessage().getChatId();
        
        TaskSelectionStorage storage = TaskSelectionStorage.getInstance();
        boolean isSelected = storage.toggleTask(chatId, taskId);
        
        // 返回回调答复以显示选中状态变化
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text(isSelected ? "已选中" : "已取消选中")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("回调查询应答失败", e);
        }
        
        // 刷新任务列表（保持当前页码）
        return refreshTaskList(callbackQuery, chatId);
    }
    
    /**
     * 刷新任务列表（保持分页状态）
     */
    public BotApiMethod<? extends Serializable> refreshTaskList(CallbackQuery callbackQuery, long chatId) {
        IOciCreateTaskService taskService = SpringUtil.getBean(IOciCreateTaskService.class);
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        List<OciCreateTask> taskList = taskService.list();
        
        if (CollectionUtil.isEmpty(taskList)) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 当前没有正在执行的任务",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        PaginationStorage paginationStorage = PaginationStorage.getInstance();
        
        // Check and adjust current page if it exceeds total pages
        int totalPages = PaginationStorage.calculateTotalPages(taskList.size(), PAGE_SIZE);
        int currentPage = paginationStorage.getCurrentPage(chatId, PAGE_TYPE);
        if (currentPage >= totalPages) {
            // Reset to last valid page
            paginationStorage.setCurrentPage(chatId, PAGE_TYPE, totalPages - 1);
        }
        
        Map<String, OciUser> userMap = userService.list().stream()
                .collect(Collectors.toMap(OciUser::getId, u -> u));
        
        return buildTaskManagementMessage(callbackQuery, taskList, userMap, chatId, paginationStorage);
    }
    
    /**
     * 构建任务管理消息
     */
    private BotApiMethod<? extends Serializable> buildTaskManagementMessage(
            CallbackQuery callbackQuery,
            List<OciCreateTask> taskList,
            Map<String, OciUser> userMap,
            long chatId,
            PaginationStorage paginationStorage) {
        
        TaskSelectionStorage selectionStorage = TaskSelectionStorage.getInstance();
        
        int currentPage = paginationStorage.getCurrentPage(chatId, PAGE_TYPE);
        int totalPages = PaginationStorage.calculateTotalPages(taskList.size(), PAGE_SIZE);
        int startIndex = PaginationStorage.getStartIndex(currentPage, PAGE_SIZE);
        int endIndex = PaginationStorage.getEndIndex(currentPage, PAGE_SIZE, taskList.size());
        
        // 获取当前页的任务列表
        List<OciCreateTask> pageTasks = taskList.subList(startIndex, endIndex);
        
        StringBuilder message = new StringBuilder("【任务管理】\n\n");
        message.append(String.format("共 %d 个正在执行的任务，当前第 %d/%d 页：\n\n",
                taskList.size(), currentPage + 1, totalPages));
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        for (int i = 0; i < pageTasks.size(); i++) {
            OciCreateTask task = pageTasks.get(i);
            OciUser user = userMap.get(task.getUserId());
            
            if (user == null) {
                continue;
            }
            
            Long counts = (Long) TEMP_MAP.get(CommonUtils.CREATE_COUNTS_PREFIX + task.getId());
            boolean isSelected = selectionStorage.isSelected(chatId, task.getId());
            int taskNumber = startIndex + i + 1; // 全局任务编号
            
            message.append(String.format(
                    "%s %d. [%s] [%s] [%s]\n" +
                    "   配置: %s核/%sG/%sG\n" +
                    "   数量: %s台 | 已运行: %s | 尝试: %s次\n\n",
                    isSelected ? "☑️" : "⬜",
                    taskNumber,
                    user.getUsername(),
                    user.getOciRegion(),
                    task.getArchitecture(),
                    task.getOcpus().intValue(),
                    task.getMemory().intValue(),
                    task.getDisk(),
                    task.getCreateNumbers(),
                    CommonUtils.getTimeDifference(task.getCreateTime()),
                    counts == null ? "0" : counts
            ));
            
            // 添加任务按钮（每行2个）
            if (i % 2 == 0) {
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("%s 任务%d", isSelected ? "☑️" : "⬜", taskNumber),
                        "toggle_task:" + task.getId()
                ));
                keyboard.add(row);
            } else {
                keyboard.get(keyboard.size() - 1).add(KeyboardBuilder.button(
                        String.format("%s 任务%d", isSelected ? "☑️" : "⬜", taskNumber),
                        "toggle_task:" + task.getId()
                ));
            }
        }
        
        // 添加分页按钮
        if (totalPages > 1) {
            keyboard.add(KeyboardBuilder.buildPaginationRow(
                    currentPage,
                    totalPages,
                    "task_page_prev",
                    "task_page_next"
            ));
        }
        
        // 添加批量操作按钮
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("✅ 全选", "select_all_tasks"),
                KeyboardBuilder.button("⬜ 取消全选", "deselect_all_tasks")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🛑 结束选中的任务", "stop_selected_tasks")
        ));
        
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "toggle_task:";
    }
}

/**
 * 全选任务处理器
 * 
 * @author yohann
 */
@Slf4j
@Component
class SelectAllTasksHandler extends AbstractCallbackHandler {
    
    private static final String PAGE_TYPE = "task_management";
    private static final int PAGE_SIZE = 5;
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        IOciCreateTaskService taskService = SpringUtil.getBean(IOciCreateTaskService.class);
        
        List<OciCreateTask> taskList = taskService.list();
        if (CollectionUtil.isEmpty(taskList)) {
            return null;
        }
        
        // Get current page info
        PaginationStorage paginationStorage = PaginationStorage.getInstance();
        int currentPage = paginationStorage.getCurrentPage(chatId, PAGE_TYPE);
        int startIndex = PaginationStorage.getStartIndex(currentPage, PAGE_SIZE);
        int endIndex = PaginationStorage.getEndIndex(currentPage, PAGE_SIZE, taskList.size());
        
        // Only select tasks on current page
        TaskSelectionStorage storage = TaskSelectionStorage.getInstance();
        List<OciCreateTask> pageTasks = taskList.subList(startIndex, endIndex);
        pageTasks.forEach(task -> storage.selectTask(chatId, task.getId()));
        
        // 回答回调
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text(String.format("已全选当前页的 %d 个任务", pageTasks.size()))
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("回调查询应答失败", e);
        }
        
        // 刷新任务列表（使用 ToggleTaskHandler 的方法保持分页）
        ToggleTaskHandler handler = SpringUtil.getBean(ToggleTaskHandler.class);
        return handler.refreshTaskList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "select_all_tasks";
    }
}

/**
 * 取消全选任务处理器
 * 
 * @author yohann
 */
@Slf4j
@Component
class DeselectAllTasksHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        TaskSelectionStorage storage = TaskSelectionStorage.getInstance();
        storage.clearSelection(chatId);
        
        // 回答回调
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("已取消所有选中")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("回调查询应答失败", e);
        }
        
        // 刷新任务列表（使用 ToggleTaskHandler 的方法保持分页）
        ToggleTaskHandler handler = SpringUtil.getBean(ToggleTaskHandler.class);
        return handler.refreshTaskList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "deselect_all_tasks";
    }
}

/**
 * 停止选中任务处理器
 * 
 * @author yohann
 */
@Slf4j
@Component
class StopSelectedTasksHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        TaskSelectionStorage storage = TaskSelectionStorage.getInstance();
        Set<String> selectedTasks = storage.getSelectedTasks(chatId);
        
        if (selectedTasks.isEmpty()) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("请先选择要停止的任务")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("回调查询应答失败", e);
            }
            return null;
        }
        
        // 调用 IOciService.stopCreate 停止任务
        IOciService ociService = SpringUtil.getBean(IOciService.class);
        IOciCreateTaskService taskService = SpringUtil.getBean(IOciCreateTaskService.class);
        
        int successCount = 0;
        int failedCount = 0;
        
        // Group tasks by userId to avoid duplicate stopCreate calls
        for (String taskId : selectedTasks) {
            try {
                OciCreateTask task = taskService.getById(taskId);
                if (task != null) {
                    StopCreateParams params = new StopCreateParams();
                    params.setUserId(task.getUserId());
                    
                    // Call IOciService.stopCreate method
                    ociService.stopCreate(params);
                    successCount++;
                    
                    log.info("Successfully stopped task: taskId={}, userId={}", taskId, task.getUserId());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to stop task: taskId={}", taskId, e);
            }
        }
        
        // Clear selection
        storage.clearSelection(chatId);
        
        // Build result message
        String resultMessage;
        if (failedCount > 0) {
            resultMessage = String.format("✅ 成功停止 %d 个任务\n❌ 失败 %d 个任务", successCount, failedCount);
        } else {
            resultMessage = String.format("✅ 已成功停止 %d 个任务", successCount);
        }
        
        // Answer callback
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text(resultMessage)
                    .showAlert(true)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Refresh task list（使用 ToggleTaskHandler 的方法保持分页）
        ToggleTaskHandler handler = SpringUtil.getBean(ToggleTaskHandler.class);
        return handler.refreshTaskList(callbackQuery, chatId);
    }
    
    @Override
    public String getCallbackPattern() {
        return "stop_selected_tasks";
    }
}
