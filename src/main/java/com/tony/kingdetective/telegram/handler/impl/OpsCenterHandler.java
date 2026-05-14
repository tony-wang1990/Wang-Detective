package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciCreateTask;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.vo.SystemDiagnostics;
import com.tony.kingdetective.service.IOciCreateTaskService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.service.SystemDiagnosticsService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tony.kingdetective.service.impl.OciServiceImpl.TEMP_MAP;

/**
 * Telegram operations center entry.
 */
@Slf4j
@Component
public class OpsCenterHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        return buildEditMessage(callbackQuery, "【运维中心】\n\n集中查看系统诊断、任务状态、最近日志，并快速进入常用运维动作。", buildOpsKeyboard());
    }

    @Override
    public String getCallbackPattern() {
        return "ops_center";
    }

    static InlineKeyboardMarkup buildOpsKeyboard() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("系统诊断", "ops_diagnostics"),
                KeyboardBuilder.button("任务状态", "ops_task_status")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("最近日志", "ops_recent_logs"),
                KeyboardBuilder.button("日志文件", "log_query")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("快捷运维", "ops_quick_actions"),
                KeyboardBuilder.button("版本信息", "version_info")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("SSH管理", "ssh_management"),
                KeyboardBuilder.button("安全管理", "security_management")
        ));
        rows.add(new InlineKeyboardRow(KeyboardBuilder.button("« 返回主菜单", "back_to_main")));
        rows.add(KeyboardBuilder.buildCancelRow());
        return new InlineKeyboardMarkup(rows);
    }
}

@Slf4j
@Component
class OpsDiagnosticsHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        try {
            SystemDiagnostics diagnostics = SpringUtil.getBean(SystemDiagnosticsService.class).diagnostics();
            long ok = diagnostics.getChecks().stream().filter(item -> "OK".equals(item.getStatus())).count();
            long warn = diagnostics.getChecks().stream().filter(item -> "WARN".equals(item.getStatus())).count();
            long error = diagnostics.getChecks().stream().filter(item -> "ERROR".equals(item.getStatus())).count();

            StringBuilder message = new StringBuilder("【系统诊断】\n\n");
            message.append("总体状态: ").append(diagnostics.getStatus()).append('\n');
            message.append("版本: ").append(OpsCenterSupport.blankToDash(diagnostics.getVersion())).append('\n');
            message.append("Java: ").append(OpsCenterSupport.blankToDash(diagnostics.getJavaVersion())).append('\n');
            message.append("系统: ").append(OpsCenterSupport.blankToDash(diagnostics.getOsName())).append('\n');
            message.append("运行时长: ").append(OpsCenterSupport.formatDuration(diagnostics.getUptimeSeconds())).append('\n');
            message.append("内存: ").append(OpsCenterSupport.formatBytes(diagnostics.getUsedMemoryBytes()))
                    .append(" / ").append(OpsCenterSupport.formatBytes(diagnostics.getMaxMemoryBytes())).append('\n');
            message.append("磁盘可用: ").append(OpsCenterSupport.formatBytes(diagnostics.getFreeDiskBytes())).append('\n');
            message.append("检查项: OK ").append(ok).append(" / WARN ").append(warn).append(" / ERROR ").append(error).append("\n\n");

            diagnostics.getChecks().stream().limit(12).forEach(item -> message.append(statusIcon(item.getStatus()))
                    .append(' ').append(item.getName()).append(": ")
                    .append(item.getMessage()).append('\n'));

            return buildEditMessage(callbackQuery, OpsCenterSupport.escapeMarkdown(OpsCenterSupport.limitTelegramText(message.toString())), OpsCenterHandler.buildOpsKeyboard());
        } catch (Exception e) {
            log.error("Telegram system diagnostics failed", e);
            return buildEditMessage(callbackQuery, OpsCenterSupport.escapeMarkdown("系统诊断失败: " + e.getMessage()), OpsCenterHandler.buildOpsKeyboard());
        }
    }

    @Override
    public String getCallbackPattern() {
        return "ops_diagnostics";
    }

    private String statusIcon(String status) {
        if ("OK".equals(status)) {
            return "OK";
        }
        if ("ERROR".equals(status)) {
            return "ERROR";
        }
        return "WARN";
    }
}

@Slf4j
@Component
class OpsTaskStatusHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciCreateTaskService taskService = SpringUtil.getBean(IOciCreateTaskService.class);
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciCreateTask> tasks = taskService.list();

        if (CollectionUtil.isEmpty(tasks)) {
            return buildEditMessage(callbackQuery, "【任务状态】\n\n当前没有正在执行的开机任务。", OpsCenterHandler.buildOpsKeyboard());
        }

        Map<String, OciUser> userMap = userService.list().stream()
                .collect(Collectors.toMap(OciUser::getId, item -> item, (left, right) -> left));
        Map<String, Long> architectureCount = tasks.stream()
                .collect(Collectors.groupingBy(task -> OpsCenterSupport.blankToDash(task.getArchitecture()), Collectors.counting()));

        StringBuilder message = new StringBuilder("【任务状态】\n\n");
        message.append("正在执行: ").append(tasks.size()).append(" 个\n");
        architectureCount.forEach((architecture, count) -> message.append(architecture).append(": ").append(count).append(" 个\n"));
        message.append('\n');

        tasks.stream()
                .sorted(Comparator.comparing(OciCreateTask::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .forEach(task -> appendTask(message, task, userMap.get(task.getUserId())));

        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("刷新状态", "ops_task_status"),
                KeyboardBuilder.button("进入任务管理", "task_management")
        ));
        rows.add(new InlineKeyboardRow(KeyboardBuilder.button("« 返回运维中心", "ops_center")));
        rows.add(KeyboardBuilder.buildCancelRow());

        return buildEditMessage(callbackQuery, OpsCenterSupport.escapeMarkdown(OpsCenterSupport.limitTelegramText(message.toString())), new InlineKeyboardMarkup(rows));
    }

    @Override
    public String getCallbackPattern() {
        return "ops_task_status";
    }

    private void appendTask(StringBuilder message, OciCreateTask task, OciUser user) {
        Long counts = (Long) TEMP_MAP.get(CommonUtils.CREATE_COUNTS_PREFIX + task.getId());
        message.append("- ")
                .append(user == null ? "未知配置" : user.getUsername())
                .append(" / ")
                .append(user == null ? "-" : user.getOciRegion())
                .append(" / ")
                .append(OpsCenterSupport.blankToDash(task.getArchitecture()))
                .append('\n')
                .append("  规格: ")
                .append(task.getOcpus() == null ? "-" : task.getOcpus().intValue())
                .append("C/")
                .append(task.getMemory() == null ? "-" : task.getMemory().intValue())
                .append("G/")
                .append(task.getDisk() == null ? "-" : task.getDisk())
                .append("G, 数量: ")
                .append(task.getCreateNumbers())
                .append(", 尝试: ")
                .append(counts == null ? 0 : counts)
                .append('\n');
    }
}

@Slf4j
@Component
class OpsRecentLogsHandler extends AbstractCallbackHandler {

    private static final int MAX_LINES = 30;

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        File logFile = new File(CommonUtils.LOG_FILE_PATH);
        if (!logFile.exists()) {
            return buildEditMessage(callbackQuery, OpsCenterSupport.escapeMarkdown("【最近日志】\n\n日志文件不存在: " + CommonUtils.LOG_FILE_PATH), OpsCenterHandler.buildOpsKeyboard());
        }

        List<String> lines = readLastLines(logFile, MAX_LINES);
        StringBuilder message = new StringBuilder("【最近日志】\n");
        message.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        if (lines.isEmpty()) {
            message.append("暂无日志内容。");
        } else {
            lines.forEach(line -> message.append(OpsCenterSupport.shorten(line, 180)).append('\n'));
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("刷新", "ops_recent_logs"),
                KeyboardBuilder.button("发送日志文件", "log_query")
        ));
        rows.add(new InlineKeyboardRow(KeyboardBuilder.button("« 返回运维中心", "ops_center")));
        rows.add(KeyboardBuilder.buildCancelRow());

        return buildEditMessage(callbackQuery, OpsCenterSupport.escapeMarkdown(OpsCenterSupport.limitTelegramText(message.toString())), new InlineKeyboardMarkup(rows));
    }

    @Override
    public String getCallbackPattern() {
        return "ops_recent_logs";
    }

    private List<String> readLastLines(File logFile, int limit) {
        LinkedList<String> lines = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > limit) {
                    lines.removeFirst();
                }
            }
        } catch (Exception e) {
            log.error("Read recent log failed", e);
            lines.clear();
            lines.add("读取日志失败: " + e.getMessage());
        }
        return lines;
    }
}

@Component
class OpsQuickActionsHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("一键抢机", "config_list"),
                KeyboardBuilder.button("快捷开机", "quick_start")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("一键测活", "check_alive"),
                KeyboardBuilder.button("任务管理", "task_management")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("SSH管理", "ssh_management"),
                KeyboardBuilder.button("开放端口", "open_all_ports_select")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("系统诊断", "ops_diagnostics"),
                KeyboardBuilder.button("最近日志", "ops_recent_logs")
        ));
        rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("版本信息", "version_info"),
                KeyboardBuilder.button("备份恢复", "backup_restore")
        ));
        rows.add(new InlineKeyboardRow(KeyboardBuilder.button("« 返回运维中心", "ops_center")));
        rows.add(KeyboardBuilder.buildCancelRow());

        return buildEditMessage(
                callbackQuery,
                "【快捷运维入口】\n\n这里集中放置高频、安全的运维入口。危险动作仍沿用原有确认流程。",
                new InlineKeyboardMarkup(rows)
        );
    }

    @Override
    public String getCallbackPattern() {
        return "ops_quick_actions";
    }
}

final class OpsCenterSupport {

    private OpsCenterSupport() {
    }

    static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    static String formatDuration(Long seconds) {
        if (seconds == null || seconds < 0) {
            return "-";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return days + "天" + hours + "小时";
        }
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return minutes + "分钟";
    }

    static String formatBytes(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "-";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format("%.1f %s", value, units[unit]);
    }

    static String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    static String limitTelegramText(String text) {
        if (text == null || text.length() <= 3800) {
            return text;
        }
        return text.substring(0, 3760) + "\n\n内容过长，已截断。";
    }

    static String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }
}
