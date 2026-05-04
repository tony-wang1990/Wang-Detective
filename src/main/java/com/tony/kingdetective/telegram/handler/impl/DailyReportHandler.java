package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.service.IInstanceService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Daily report handler
 * Send daily summary reports of OCI resources
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class DailyReportHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_KEY = "daily_report_enabled";
    private static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Check current report status
            OciKv reportKv = kvService.getByKey(REPORT_KEY);
            boolean isEnabled = reportKv != null && "true".equals(reportKv.getValue());
            
            // Get report time
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            String reportTime = (timeKv != null && timeKv.getValue() != null) ? timeKv.getValue() : "09";
            
            StringBuilder message = new StringBuilder();
            message.append("【每日报告】\n\n");
            message.append(String.format("当前状态: %s\n\n", isEnabled ? "✅ 已开启" : "❌ 已关闭"));
            
            message.append("📊 报告内容:\n");
            message.append("• 所有账户资源概览\n");
            message.append("• 实例运行状态统计\n");
            message.append("• 当日流量使用情况\n");
            message.append("• 配额使用率\n");
            message.append("• 异常事件提醒\n\n");
            
            message.append(String.format("⏰ 发送时间: 每天 %s:00\n\n", reportTime));
            
            if (isEnabled) {
                message.append("💡 每天早上会自动发送报告\n");
                message.append("帮助您掌握资源使用情况\n\n");
                message.append("⚠️ 注意: 需要后台定时任务支持");
            } else {
                message.append("💡 开启后每天自动接收报告\n");
                message.append("无需手动查询");
            }
            
            List<InlineKeyboardRow> keyboard = List.of(
                    new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    isEnabled ? "❌ 关闭报告" : "✅ 开启报告",
                                    isEnabled ? "report_disable" : "report_enable"
                            )
                    ),
                    new InlineKeyboardRow(
                            KeyboardBuilder.button("📋 查看今日报告", "report_today"),
                            KeyboardBuilder.button("⚙️ 设置时间", "report_schedule")
                    ),
                    KeyboardBuilder.buildBackToMainMenuRow(),
                    KeyboardBuilder.buildCancelRow()
            );
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get daily report status", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取报告状态失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "daily_report";
    }
}

/**
 * Enable daily report handler
 */
@Slf4j
@Component
class ReportEnableHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_KEY = "daily_report_enabled";
    private static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv reportKv = kvService.getByKey(REPORT_KEY);
            if (reportKv == null) {
                reportKv = new OciKv();
                reportKv.setCode(REPORT_KEY);
                reportKv.setValue("true");
                reportKv.setType("SYSTEM"); // Fix: Set type for NOT NULL constraint
                kvService.save(reportKv);
            } else {
                reportKv.setValue("true");
                kvService.updateById(reportKv);
            }
            
            // Get report time
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            String reportTime = (timeKv != null && timeKv.getValue() != null) ? timeKv.getValue() : "09";
            
            return buildEditMessage(
                    callbackQuery,
                    "✅ 每日报告已开启\n\n" +
                    String.format("系统将在每天 %s:00 发送资源报告\n\n", reportTime) +
                    "报告内容包括:\n" +
                    "• 实例状态统计\n" +
                    "• 流量使用情况\n" +
                    "• 配额使用率\n" +
                    "• 异常事件提醒\n\n" +
                    "💡 提示: 需要确保后台定时任务正在运行",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to enable daily report", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 开启报告失败: " + e.getMessage(),
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
        return "report_enable";
    }
}

/**
 * Disable daily report handler
 */
@Slf4j
@Component
class ReportDisableHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_KEY = "daily_report_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv reportKv = kvService.getByKey(REPORT_KEY);
            if (reportKv != null) {
                reportKv.setValue("false");
                kvService.updateById(reportKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "✅ 每日报告已关闭\n\n" +
                    "系统将不再自动发送每日报告\n\n" +
                    "💡 您随时可以:\n" +
                    "• 重新开启自动报告\n" +
                    "• 手动查看今日报告",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to disable daily report", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 关闭报告失败: " + e.getMessage(),
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
        return "report_disable";
    }
}

/**
 * View today's report handler
 */
@Slf4j
@Component
class ReportTodayHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("【今日资源报告】\n");
            message.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            message.append("\n\n");
            
            int totalInstances = 0;
            int runningInstances = 0;
            
            for (SysUserDTO user : users) {
                if (Boolean.TRUE.equals(user.getOciCfg().getDeleted())) {
                    continue;
                }
                
                try {
                    List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(user);
                    totalInstances += instances.size();
                    runningInstances += instances.size();
                    
                    message.append(String.format("📌 %s: %d个运行中\n", 
                            user.getUsername(), instances.size()));
                } catch (Exception e) {
                    log.error("Failed to get instances for user: {}", user.getUsername(), e);
                }
            }
            
            message.append("\n━━━━━━━━━━━━━━━━\n");
            message.append("📊 总体统计:\n");
            message.append(String.format("• 总账户数: %d\n", users.size()));
            message.append(String.format("• 运行实例: %d\n", runningInstances));
            message.append(String.format("• 总实例数: %d\n\n", totalInstances));
            
            message.append("💡 详细信息请查看各功能模块");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🔄 刷新", "report_today"),
                                    KeyboardBuilder.button("◀️ 返回", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to generate today's report", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 生成报告失败: " + e.getMessage(),
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
        return "report_today";
    }
}

/**
 * Report schedule settings handler
 */
@Slf4j
@Component
class ReportScheduleHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        String reportTime = "09";
        
        try {
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            if (timeKv != null && timeKv.getValue() != null) {
                reportTime = timeKv.getValue();
            }
        } catch (Exception ignored) {}
        
        StringBuilder message = new StringBuilder();
        message.append("【报告时间设置】\n\n");
        message.append(String.format("当前发送时间: %s:00\n\n", reportTime));
        message.append("选择新的发送时间:\n");
        
        List<InlineKeyboardRow> keyboard = List.of(
                new InlineKeyboardRow(
                        KeyboardBuilder.button("🌅 07:00", "report_time:07"),
                        KeyboardBuilder.button("🌄 09:00", "report_time:09")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("🌞 12:00", "report_time:12"),
                        KeyboardBuilder.button("🌆 18:00", "report_time:18")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("🌙 21:00", "report_time:21"),
                        KeyboardBuilder.button("🌃 23:00", "report_time:23")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("◀️ 返回", "daily_report")
                ),
                KeyboardBuilder.buildCancelRow()
        );
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "report_schedule";
    }
}
