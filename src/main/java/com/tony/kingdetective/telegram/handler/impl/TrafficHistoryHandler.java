package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.VnicAttachment;
import com.oracle.bmc.core.requests.GetVnicRequest;
import com.oracle.bmc.core.requests.ListVnicAttachmentsRequest;
import com.oracle.bmc.core.responses.GetVnicResponse;
import com.oracle.bmc.core.responses.ListVnicAttachmentsResponse;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.*;
import com.oracle.bmc.monitoring.requests.SummarizeMetricsDataRequest;
import com.oracle.bmc.monitoring.responses.SummarizeMetricsDataResponse;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.InstanceSelectionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Traffic history handler - Step 1: Select Instance
 */
@Slf4j
@Component
public class TrafficHistoryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            if (CollectionUtil.isEmpty(users)) {
                // Clear stale cache if no configs exist
                InstanceSelectionStorage.getInstance().clearAll(chatId);
                return buildEditMessage(callbackQuery, "❌ 未找到 OCI 配置", new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu()));
            }

            // If only one user or specific config selected
            String ociCfgId;
            if (callbackData.contains(":")) {
                ociCfgId = callbackData.split(":")[1];
                // Validate that the config still exists
                IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
                if (userService.getById(ociCfgId) == null) {
                    // Config was deleted, clear cache and show current configs
                    InstanceSelectionStorage.getInstance().clearAll(chatId);
                    if (users.size() == 1) {
                        ociCfgId = users.get(0).getOciCfg().getId();
                    } else {
                        return buildConfigSelector(callbackQuery, users);
                    }
                }
            } else if (users.size() == 1) {
                ociCfgId = users.get(0).getOciCfg().getId();
            } else {
                // Show Config Selector
                return buildConfigSelector(callbackQuery, users);
            }

            // List Instances
            IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(sysUserDTO);
            
            if (CollectionUtil.isEmpty(instances)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 该配置下无运行中的实例",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回主菜单", "back_to_main")),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            InstanceSelectionStorage.getInstance().setInstanceCache(chatId, instances);
            InstanceSelectionStorage.getInstance().setConfigContext(chatId, ociCfgId);
            
            return buildInstanceListMessage(callbackQuery, instances, ociCfgId);
            
        } catch (Exception e) {
            log.error("Traffic History Error", e);
            return buildEditMessage(callbackQuery, "❌ 获取列表失败: " + e.getMessage(), new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu()));
        }
    }
    
    private BotApiMethod<? extends Serializable> buildConfigSelector(CallbackQuery callbackQuery, List<SysUserDTO> users) {
        StringBuilder message = new StringBuilder("【流量历史查询】\n\n请选择 OCI 配置：\n\n");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (SysUserDTO user : users) {
            rows.add(new InlineKeyboardRow(
                    KeyboardBuilder.button(user.getUsername() + " (" + user.getOciCfg().getRegion() + ")", 
                            "traffic_history_config:" + user.getOciCfg().getId())
            ));
        }
        rows.add(KeyboardBuilder.buildBackToMainMenuRow());
        rows.add(KeyboardBuilder.buildCancelRow());
        return buildEditMessage(callbackQuery, message.toString(), new InlineKeyboardMarkup(rows));
    }
    
    private BotApiMethod<? extends Serializable> buildInstanceListMessage(CallbackQuery callbackQuery, List<SysUserDTO.CloudInstance> instances, String ociCfgId) {
        StringBuilder message = new StringBuilder("【流量历史查询】\n\n请选择要查询的实例：\n\n");
        List<InlineKeyboardRow> rows = new ArrayList<>();
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            message.append(String.format("%d. %s (%s)\n", i + 1, instance.getName(), instance.getRegion()));
            rows.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("📊 " + instance.getName(), "traffic_history_instance:" + i)
            ));
        }
        
        rows.add(new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回配置列表", "traffic_history")));
        rows.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(callbackQuery, message.toString(), new InlineKeyboardMarkup(rows));
    }

    @Override
    public String getCallbackPattern() {
        return "traffic_history";
    }
}

/**
 * Handle Config Selection (Redirection)
 */
@Component
class TrafficHistoryConfigHandler extends TrafficHistoryHandler {
    @Override
    public String getCallbackPattern() {
        return "traffic_history_config:";
    }
}

/**
 * Step 2: Select Time Range
 */
@Slf4j
@Component
class TrafficHistoryInstanceHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int index = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        log.info("Traffic history instance selected: chatId={}, index={}, callbackData={}", chatId, index, callbackData);
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        storage.setSelectedInstanceIndex(chatId, index);
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, index);
        
        if (instance == null) {
            log.error("Instance is null: chatId={}, index={}", chatId, index);
            return buildEditMessage(callbackQuery, "❌ 实例信息已过期，请重新选择", new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu()));
        }
        
        log.info("Instance found: chatId={}, instanceName={}, instanceId={}", chatId, instance.getName(), instance.getOcId());
        
        String configContext = storage.getConfigContext(chatId);
        log.info("Config context: chatId={}, ociCfgId={}", chatId, configContext);
        
        return buildEditMessage(
                callbackQuery,
                String.format("【流量查询 - %s】\n\n请选择查询时间范围：", instance.getName()),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(KeyboardBuilder.button("📅 近 24 小时", "traffic_history_query:1")),
                        new InlineKeyboardRow(KeyboardBuilder.button("📅 近 7 天", "traffic_history_query:7")),
                        new InlineKeyboardRow(KeyboardBuilder.button("📅 近 30 天", "traffic_history_query:30")),
                        new InlineKeyboardRow(KeyboardBuilder.button("📅 近 90 天", "traffic_history_query:90")),
                        new InlineKeyboardRow(KeyboardBuilder.button("◀️ 返回实例列表", "traffic_history_config:" + storage.getConfigContext(chatId))),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }

    @Override
    public String getCallbackPattern() {
        return "traffic_history_instance:";
    }
}

/**
 * Step 3: Execute Query
 */
@Slf4j
@Component
class TrafficHistoryQueryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        int days = Integer.parseInt(callbackQuery.getData().split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Instant feedback
        try {
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("⏳ 正在从 Oracle Cloud 查询监控数据，请稍候...")
                    .build());
        } catch (Exception ignored) {}
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        SysUserDTO.CloudInstance instance = storage.getSelectedInstance(chatId);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (instance == null) {
            storage.clearAll(chatId);
            return buildEditMessage(callbackQuery, "❌ 会话过期，请重新选择", new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu()));
        }
        
        // Validate that ociCfgId exists
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        if (ociCfgId == null || userService.getById(ociCfgId) == null) {
            storage.clearAll(chatId);
            return buildEditMessage(
                callbackQuery, 
                "❌ OCI 配置已被删除，请返回主菜单重新选择", 
                new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                
                // 1. Get VNIC ID
                String vnicId = getVnicId(fetcher, instance);
                if (vnicId == null) {
                     return buildEditMessage(callbackQuery, "❌ 无法获取实例网卡信息 (VNIC ID)", buildRetryKeyboard(days));
                }
                
                // 2. Query Metrics
                Date endTime = Date.from(Instant.now());
                Date startTime = Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
                // Use 1h resolution for > 1 day, 1m for 1 day
                String resolution = days > 1 ? "1h" : "1m";
                
                TrafficStats stats = queryTrafficMetrics(fetcher, vnicId, startTime, endTime, resolution);
                
                String msg = String.format(
                        "📊 **流量统计报告**\n\n" +
                        "实例: %s\n" +
                        "区域: %s\n" +
                        "时间: 近 %d 天\n\n" +
                        "⬇️ **入站流量**: %s\n" +
                        "⬆️ **出站流量**: %s\n" +
                        "🔁 **总计流量**: %s\n\n" +
                        "💡 数据来源: OCI Monitoring (oci_vcn)",
                        instance.getName(), instance.getRegion(), days,
                        formatBytes(stats.inboundBytes),
                        formatBytes(stats.outboundBytes),
                        formatBytes(stats.totalBytes)
                );
                
                return buildEditMessage(callbackQuery, msg, buildRetryKeyboard(days));
            }
        } catch (Exception e) {
            log.error("Traffic Query Failed", e);
            return buildEditMessage(callbackQuery, "❌ 查询失败: " + e.getMessage(), buildRetryKeyboard(days));
        }
    }
    
    private InlineKeyboardMarkup buildRetryKeyboard(int days) {
        return new InlineKeyboardMarkup(List.of(
                new InlineKeyboardRow(
                        KeyboardBuilder.button("🔄 刷新", "traffic_history_query:" + days),
                        KeyboardBuilder.button("◀️ 返回", "traffic_history") // Go loop back to start or instance select
                ),
                KeyboardBuilder.buildCancelRow()
        ));
    }
    
    private String getVnicId(OracleInstanceFetcher fetcher, SysUserDTO.CloudInstance instance) {
        try {
            ListVnicAttachmentsRequest request = ListVnicAttachmentsRequest.builder()
                    .compartmentId(fetcher.getCompartmentId())
                    .instanceId(instance.getOcId())
                    .build();
            ListVnicAttachmentsResponse response = fetcher.getComputeClient().listVnicAttachments(request);
            if (!response.getItems().isEmpty()) {
                return response.getItems().get(0).getVnicId();
            }
        } catch (Exception e) {
            log.warn("Failed to get VNIC: {}", e.getMessage());
        }
        return null;
    }
    
    private TrafficStats queryTrafficMetrics(OracleInstanceFetcher fetcher, String vnicId, Date startTime, Date endTime, String resolution) {
         TrafficStats stats = new TrafficStats();
         try {
             MonitoringClient client = fetcher.getMonitoringClient();
             String compartmentId = fetcher.getCompartmentId();
             
             // Inbound (BytesFromNetwork ? No, VnicFromNetworkBytes)
             // Namespace: oci_vcn
             // Metric: VnicFromNetworkBytes
             // Dimension: resourceId = vnicId
             
             Map<String, String> dimensions = new HashMap<>();
             dimensions.put("resourceId", vnicId);
             
             SummarizeMetricsDataDetails inboundDetails = SummarizeMetricsDataDetails.builder()
                     .namespace("oci_vcn")
                     .query("VnicFromNetworkBytes[1h].sum()") // Using 1h interval for sum
                     .startTime(startTime)
                     .endTime(endTime)
                     .resolution(resolution)
                     .build();
             // Note: OCI API requires filtering by dimension in the query string or filter object?
             // Actually, for SummarizeMetricsData, we usually put it in the query:
             // "VnicFromNetworkBytes[1h]{resourceId = \"...\"}.sum()"
             
             String queryIn = String.format("VnicFromNetworkBytes[%s]{resourceId = \"%s\"}.sum()", resolution, vnicId);
             String queryOut = String.format("VnicToNetworkBytes[%s]{resourceId = \"%s\"}.sum()", resolution, vnicId);
             
             stats.inboundBytes = executeQuery(client, compartmentId, "oci_vcn", queryIn, startTime, endTime, resolution);
             stats.outboundBytes = executeQuery(client, compartmentId, "oci_vcn", queryOut, startTime, endTime, resolution);
             stats.totalBytes = stats.inboundBytes + stats.outboundBytes;
             
         } catch (Exception e) {
             log.error("Metric Query Error", e);
             throw e;
         }
         return stats;
    }
    
    private long executeQuery(MonitoringClient client, String compartmentId, String namespace, String query, Date start, Date end, String resolution) {
        SummarizeMetricsDataDetails details = SummarizeMetricsDataDetails.builder()
                .namespace(namespace)
                .query(query)
                .startTime(start)
                .endTime(end)
                .resolution(resolution)
                .build();
        SummarizeMetricsDataRequest request = SummarizeMetricsDataRequest.builder()
                .compartmentId(compartmentId)
                .summarizeMetricsDataDetails(details)
                .build();
        SummarizeMetricsDataResponse response = client.summarizeMetricsData(request);
        return calculateTotal(response.getItems());
    }

    private long calculateTotal(List<MetricData> metricDataList) {
        if (metricDataList == null) return 0;
        long total = 0;
        for (MetricData md : metricDataList) {
            for (AggregatedDatapoint dp : md.getAggregatedDatapoints()) {
                if (dp.getValue() != null) total += dp.getValue().longValue();
            }
        }
        return total;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public String getCallbackPattern() {
        return "traffic_history_query:";
    }
    
    private static class TrafficStats {
        long inboundBytes = 0;
        long outboundBytes = 0;
        long totalBytes = 0;
    }
}
