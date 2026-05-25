package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.params.oci.instance.AutoRescueParams;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.service.IOciService;
import com.tony.kingdetective.service.IOciUserService;
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
 * 救援中心 TG 操作入口
 *
 * 提供：
 * 1. 救援中心首页菜单（rescue_center）
 * 2. 选择账号后列出实例（rescue_select_account:<ociUserId>）
 * 3. 确认后触发 autoRescue（rescue_confirm:<ociUserId>:<instanceId>）
 *
 * 高危操作均需要二次确认（confirm/cancel 按钮），确保操作安全。
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class RescueActionHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        String text = "🆘 *救援中心*\n\n" +
                "面向 OCI 实例失联、SSH 异常、Boot Volume 修复的应急操作向导。\n\n" +
                "⚠️ *高危操作说明*\n" +
                "• 自动救援会创建临时救援实例并挂载目标卷\n" +
                "• 操作前请确认目标实例数据已备份\n" +
                "• 救援完成后请手动检查数据完整性\n\n" +
                "📖 *可用操作*\n" +
                "• 自动救援 — 自动化拆卷救援流程\n" +
                "• 救援指南 — 查看当前救援状态和文档\n\n" +
                "请选择操作：";

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🚑 发起自动救援", "rescue_choose_account"),
                KeyboardBuilder.button("📋 救援指南", "rescue_guide")
        ));
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());

        return buildEditMessage(callbackQuery, text, new InlineKeyboardMarkup(keyboard));
    }

    @Override
    public String getCallbackPattern() {
        return "rescue_center";
    }
}

/**
 * 救援 - 选择账号（列出所有 OCI 账号）
 */
@Component
class RescueChooseAccountHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> users = userService.list(
                new LambdaQueryWrapper<OciUser>().eq(OciUser::getDeleted, 0));

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return buildEditMessage(callbackQuery,
                    "❌ 没有可用的 OCI 账号，请先添加账号。",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildBackToMainMenuRow())));
        }

        StringBuilder sb = new StringBuilder("🚑 *自动救援 — 选择账号*\n\n请选择要救援的实例所在的 OCI 账号：\n\n");
        for (OciUser user : users) {
            sb.append(String.format("• %s（%s）\n", user.getUsername(), user.getOciRegion()));
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("👤 " + user.getUsername(), "rescue_list_instances:" + user.getId())
            ));
        }

        keyboard.add(new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回", "rescue_center")));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(keyboard));
    }

    @Override
    public String getCallbackPattern() {
        return "rescue_choose_account";
    }
}

/**
 * 救援 - 列出账号下的实例
 */
@Slf4j
@Component
class RescueListInstancesHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();
        String ociUserId = data.contains(":") ? data.split(":", 2)[1] : "";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser ociUser = userService.getById(ociUserId);
            if (ociUser == null) {
                return buildEditMessage(callbackQuery, "❌ 账号不存在。",
                        new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildBackToMainMenuRow())));
            }

            SysUserDTO sysUserDTO = SysUserDTO.builder()
                    .username(ociUser.getUsername())
                    .ociCfg(SysUserDTO.OciCfg.builder()
                            .userId(ociUser.getOciUserId())
                            .tenantId(ociUser.getOciTenantId())
                            .region(ociUser.getOciRegion())
                            .fingerprint(ociUser.getOciFingerprint())
                            .privateKeyPath(ociUser.getOciKeyPath())
                            .privateKey(ociUser.getPrivateKey())
                            .build())
                    .build();

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                List<com.oracle.bmc.core.model.Instance> instances = fetcher.listInstances();
                if (instances == null || instances.isEmpty()) {
                    return buildEditMessage(callbackQuery,
                            "❌ 该账号下没有实例，无法发起救援。",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回", "rescue_choose_account")),
                                    KeyboardBuilder.buildCancelRow())));
                }

                StringBuilder sb = new StringBuilder(
                        String.format("🚑 *自动救援 — 选择实例*\n\n账号：%s\n\n请选择要救援的实例：\n\n", ociUser.getUsername()));

                for (com.oracle.bmc.core.model.Instance inst : instances) {
                    String stateIcon = "RUNNING".equals(inst.getLifecycleState().getValue()) ? "🟢" :
                                       "STOPPED".equals(inst.getLifecycleState().getValue()) ? "⛔" : "🟡";
                    sb.append(String.format("%s %s\n", stateIcon, inst.getDisplayName()));
                    keyboard.add(new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    stateIcon + " " + inst.getDisplayName(),
                                    "rescue_confirm:" + ociUserId + ":" + inst.getId())
                    ));
                }

                keyboard.add(new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回", "rescue_choose_account")));
                keyboard.add(KeyboardBuilder.buildCancelRow());

                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(keyboard));
            }
        } catch (Exception e) {
            log.error("Failed to list instances for rescue: ociUserId={}", ociUserId, e);
            return buildEditMessage(callbackQuery,
                    "❌ 获取实例列表失败：" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回", "rescue_choose_account")),
                            KeyboardBuilder.buildCancelRow())));
        }
    }

    @Override
    public String getCallbackPattern() {
        return "rescue_list_instances:";
    }
}

/**
 * 救援 - 二次确认（高危操作必须确认）
 */
@Component
class RescueConfirmHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData(); // rescue_confirm:<ociUserId>:<instanceId>
        String[] parts = data.split(":", 3);
        if (parts.length < 3) {
            return buildEditMessage(callbackQuery, "❌ 参数错误，请重新操作。",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildBackToMainMenuRow())));
        }
        String ociUserId = parts[1];
        String instanceId = parts[2];

        String text = "⚠️ *高危操作确认*\n\n" +
                "您即将发起自动救援流程。\n\n" +
                "📋 *操作内容*：\n" +
                "1. 停止目标实例\n" +
                "2. 创建启动卷备份\n" +
                "3. 启动 AMD 临时救援实例\n" +
                "4. 将目标卷挂载到救援实例\n\n" +
                "⚠️ *请确认*：\n" +
                "• 目标实例数据已备份\n" +
                "• 明确知晓此操作不可立即撤销\n\n" +
                "确认后操作将在后台异步执行，完成后会推送通知。\n\n" +
                "是否继续？";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("✅ 确认救援", "rescue_execute:" + ociUserId + ":" + instanceId),
                KeyboardBuilder.button("❌ 取消", "rescue_center")
        ));

        return buildEditMessage(callbackQuery, text, new InlineKeyboardMarkup(keyboard));
    }

    @Override
    public String getCallbackPattern() {
        return "rescue_confirm:";
    }
}

/**
 * 救援 - 执行（确认后触发 autoRescue）
 */
@Component
class RescueExecuteHandler extends AbstractCallbackHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RescueExecuteHandler.class);

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData(); // rescue_execute:<ociUserId>:<instanceId>
        String[] parts = data.split(":", 3);
        if (parts.length < 3) {
            return buildEditMessage(callbackQuery, "❌ 参数错误，请重新操作。",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildBackToMainMenuRow())));
        }
        String ociUserId = parts[1];
        String instanceId = parts[2];

        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser ociUser = userService.getById(ociUserId);
            if (ociUser == null) {
                return buildEditMessage(callbackQuery, "❌ 账号不存在。",
                        new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildBackToMainMenuRow())));
            }

            // 找到 OCI 配置的 cfgId（这里用 ociUser.getId() 对应前端的 ociCfgId）
            IOciService ociService = SpringUtil.getBean(IOciService.class);
            AutoRescueParams params = new AutoRescueParams();
            params.setOciCfgId(ociUserId);
            params.setInstanceId(instanceId);
            params.setName(ociUser.getUsername() + "-rescue-" + System.currentTimeMillis() % 10000);
            params.setKeepBackupVolume(true); // 默认保留备份卷，安全起见

            // autoRescue 是异步执行的
            ociService.autoRescue(params);

            String text = "✅ *救援任务已提交*\n\n" +
                    "账号：" + ociUser.getUsername() + "\n" +
                    "区域：" + ociUser.getOciRegion() + "\n\n" +
                    "救援流程正在后台异步执行，状态变化将通过实例监控自动推送通知。\n\n" +
                    "💡 *后续步骤*：\n" +
                    "1. 等待救援实例启动\n" +
                    "2. 通过 Web 面板查看实例列表\n" +
                    "3. SSH 登录救援实例进行数据修复\n" +
                    "4. 完成后手动终止救援实例";

            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            return buildEditMessage(callbackQuery, text, new InlineKeyboardMarkup(keyboard));

        } catch (Exception e) {
            log.error("Rescue execution failed: ociUserId={}, instanceId={}", ociUserId, instanceId, e);
            return buildEditMessage(callbackQuery,
                    "❌ *救援失败*\n\n错误：" + e.getMessage() + "\n\n请检查账号权限和实例状态后重试。",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回", "rescue_center")),
                            KeyboardBuilder.buildCancelRow())));
        }
    }

    @Override
    public String getCallbackPattern() {
        return "rescue_execute:";
    }
}

/**
 * 救援指南 Handler — 展示轻量自救文档链接
 */
@Component
class RescueGuideHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "📋 *救援指南*\n\n" +
                "🔍 *轻量自救检查步骤*\n" +
                "1. 检查实例状态（控制台确认 Running）\n" +
                "2. 检查安全组规则（22 端口是否开放）\n" +
                "3. 检查 VCN/子网路由是否正常\n" +
                "4. 尝试 OCI 控制台 VNC 连接\n" +
                "5. 检查 /var/log/auth.log 登录日志\n\n" +
                "🔧 *Boot Volume 救援*\n" +
                "• 适用：实例启动失败、文件系统损坏\n" +
                "• 流程：停机 → 拆卷 → 挂载到救援机 → 修复 → 换回\n" +
                "• 建议：先从 Web 面板发起自动救援\n\n" +
                "🌐 *netboot.xyz 实验区*\n" +
                "• 适用：系统级别彻底崩溃需要重装\n" +
                "• 注意：数据会丢失，谨慎使用\n\n" +
                "📎 Web 面板救援中心：`/dashboard/rescue`";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🚑 发起自动救援", "rescue_choose_account")
        ));
        keyboard.add(new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回", "rescue_center")));
        keyboard.add(KeyboardBuilder.buildCancelRow());

        return buildEditMessage(callbackQuery, text, new InlineKeyboardMarkup(keyboard));
    }

    @Override
    public String getCallbackPattern() {
        return "rescue_guide";
    }
}
