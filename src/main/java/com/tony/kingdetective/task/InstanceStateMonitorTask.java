package com.tony.kingdetective.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oracle.bmc.core.model.Instance;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.service.ISysService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * feat: 实例状态变化主动推送 TG 通知
 *
 * 每 5 分钟轮询所有 OCI 账号的实例状态，当状态发生变化时主动推送 TG 通知。
 * 例如：实例从 Running → Stopped，或从 Stopped → Running 都会触发通知。
 *
 * 默认开启，可通过 INSTANCE_MONITOR_ENABLED=false 关闭。
 *
 * @author Tony Wang
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "king-detective.instance-monitor",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true   // 默认开启
)
public class InstanceStateMonitorTask {

    @Resource
    private IOciUserService userService;

    @Resource
    private ISysService sysService;

    /**
     * 上次已知的实例状态缓存：key = "ociUserId:instanceId", value = lifecycleState
     */
    private final Map<String, String> lastKnownStates = new ConcurrentHashMap<>();

    /**
     * 是否已完成首次初始化（首次加载只记录状态，不推送通知）
     */
    private volatile boolean initialized = false;

    /**
     * 每 5 分钟执行一次状态检查
     */
    @Scheduled(fixedDelayString = "${king-detective.instance-monitor.interval-ms:300000}")
    public void checkInstanceStates() {
        List<OciUser> users = userService.list(new LambdaQueryWrapper<OciUser>()
                .eq(OciUser::getDeleted, 0));

        if (users == null || users.isEmpty()) {
            return;
        }

        for (OciUser ociUser : users) {
            try {
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
                    List<Instance> instances = fetcher.listInstances();
                    if (instances == null || instances.isEmpty()) continue;

                    for (Instance instance : instances) {
                        String key = ociUser.getId() + ":" + instance.getId();
                        String currentState = instance.getLifecycleState().getValue();
                        String previousState = lastKnownStates.put(key, currentState);

                        if (previousState == null) {
                            // 首次记录，不推送
                            continue;
                        }

                        if (!initialized) {
                            // 初始化轮完所有实例后才开始推送
                            continue;
                        }

                        if (!previousState.equals(currentState)) {
                            String stateIcon = getStateIcon(currentState);
                            String msg = String.format(
                                    "【实例状态变化】\n\n" +
                                    "账号：%s\n" +
                                    "区域：%s\n" +
                                    "实例：%s\n" +
                                    "状态变化：%s → %s %s",
                                    ociUser.getUsername(),
                                    ociUser.getOciRegion(),
                                    instance.getDisplayName(),
                                    translateState(previousState),
                                    translateState(currentState),
                                    stateIcon
                            );
                            sysService.sendMessage(msg);
                            log.info("【实例监控】状态变化通知：用户={}, 实例={}, {}→{}",
                                    ociUser.getUsername(), instance.getDisplayName(),
                                    previousState, currentState);
                        }
                    }
                }
            } catch (Exception e) {
                // 单个账号失败不影响其他账号
                log.debug("【实例监控】检查用户 {} 失败: {}", ociUser.getUsername(), e.getMessage());
            }
        }

        if (!initialized) {
            initialized = true;
            log.info("【实例监控】首次状态快照完成，共记录 {} 个实例状态，后续变化将主动推送 TG",
                    lastKnownStates.size());
        }
    }

    private String translateState(String state) {
        if (state == null) return "未知";
        return switch (state) {
            case "RUNNING"          -> "运行中";
            case "STOPPED"         -> "已停止";
            case "STARTING"        -> "启动中";
            case "STOPPING"        -> "停止中";
            case "TERMINATED"      -> "已终止";
            case "TERMINATING"     -> "终止中";
            case "PROVISIONING"    -> "初始化中";
            case "MOVING"          -> "迁移中";
            default                -> state;
        };
    }

    private String getStateIcon(String state) {
        if (state == null) return "";
        return switch (state) {
            case "RUNNING"     -> "✅";
            case "STOPPED"     -> "⏹️";
            case "TERMINATED"  -> "❌";
            case "STARTING"    -> "🔄";
            case "STOPPING"    -> "⏳";
            default            -> "ℹ️";
        };
    }
}
