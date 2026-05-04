package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.*;
import com.oracle.bmc.monitoring.requests.SummarizeMetricsDataRequest;
import com.oracle.bmc.monitoring.responses.SummarizeMetricsDataResponse;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IInstanceService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Instance resource usage handler - view CPU, memory usage
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class InstanceResourceUsageHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String ociCfgId = callbackData.split(":")[1];
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        storage.setConfigContext(chatId, ociCfgId);
        storage.clearSelection(chatId);
        
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
            
            storage.setInstanceCache(chatId, instances);
            
            return buildResourceUsageInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list instances for resource usage", e);
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
    
    private BotApiMethod<? extends Serializable> buildResourceUsageInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        StringBuilder message = new StringBuilder("【查看客户端负载】\n\n");
        message.append("查看实例资源占用详情\n\n");
        message.append(String.format("共 %d 个运行中的实例\n", instances.size()));
        message.append("选择实例查看负载：\n\n");
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            
            message.append(String.format(
                    "%d. %s\n" +
                    "   配置: %s\n" +
                    "   区域: %s\n\n",
                    i + 1,
                    instance.getName(),
                    instance.getShape(),
                    instance.getRegion()
            ));
            
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(KeyboardBuilder.button(
                    String.format("📊 实例%d", i + 1),
                    "view_resource_usage:" + i
            ));
            keyboard.add(row);
        }
        
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
        return "instance_resource_usage:";
    }
}

/**
 * View resource usage for selected instance - Enhanced version with complete details
 */
@Slf4j
@Component
class ViewResourceUsageHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int instanceIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (instance == null) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 实例不存在",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow()))
            );
        }
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                com.oracle.bmc.core.ComputeClient computeClient = fetcher.getComputeClient();
                MonitoringClient monitoringClient = fetcher.getMonitoringClient();
                
                // Get instance details
                com.oracle.bmc.core.requests.GetInstanceRequest getInstReq = 
                        com.oracle.bmc.core.requests.GetInstanceRequest.builder()
                                .instanceId(instance.getOcId())
                                .build();
                com.oracle.bmc.core.responses.GetInstanceResponse getInstResp = 
                        computeClient.getInstance(getInstReq);
                com.oracle.bmc.core.model.Instance instanceDetail = getInstResp.getInstance();
                
                // Get boot volume info
                com.oracle.bmc.core.requests.ListBootVolumeAttachmentsRequest bootVolReq = 
                        com.oracle.bmc.core.requests.ListBootVolumeAttachmentsRequest.builder()
                                .availabilityDomain(instanceDetail.getAvailabilityDomain())
                                .compartmentId(fetcher.getCompartmentId())
                                .instanceId(instance.getOcId())
                                .build();
                com.oracle.bmc.core.responses.ListBootVolumeAttachmentsResponse bootVolResp = 
                        computeClient.listBootVolumeAttachments(bootVolReq);
                
                String bootVolumeSize = "N/A";
                if (!bootVolResp.getItems().isEmpty()) {
                    String bootVolumeId = bootVolResp.getItems().get(0).getBootVolumeId();
                    com.oracle.bmc.core.BlockstorageClient blockClient = fetcher.getBlockstorageClient();
                    com.oracle.bmc.core.requests.GetBootVolumeRequest getBootVolReq = 
                            com.oracle.bmc.core.requests.GetBootVolumeRequest.builder()
                                    .bootVolumeId(bootVolumeId)
                                    .build();
                    com.oracle.bmc.core.responses.GetBootVolumeResponse getBootVolResp = 
                            blockClient.getBootVolume(getBootVolReq);
                    bootVolumeSize = String.format("%d GB", getBootVolResp.getBootVolume().getSizeInGBs());
                }
                
                Instant endTime = Instant.now();
                Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
                
                // Query metrics
                double avgCpu = queryMetric(monitoringClient, fetcher.getCompartmentId(), 
                        instance.getOcId(), "CpuUtilization", startTime, endTime);
                double avgMem = queryMetric(monitoringClient, fetcher.getCompartmentId(), 
                        instance.getOcId(), "MemoryUtilization", startTime, endTime);
                double diskRead = queryMetric(monitoringClient, fetcher.getCompartmentId(), 
                        instance.getOcId(), "DiskBytesRead", startTime, endTime);
                double diskWrite = queryMetric(monitoringClient, fetcher.getCompartmentId(), 
                        instance.getOcId(), "DiskBytesWritten", startTime, endTime);
                double netIn = queryMetric(monitoringClient, fetcher.getCompartmentId(), 
                        instance.getOcId(), "NetworksBytesIn", startTime, endTime);
                double netOut = queryMetric(monitoringClient, fetcher.getCompartmentId(), 
                        instance.getOcId(), "NetworksBytesOut", startTime, endTime);
                
                // Calculate uptime
                String uptime = "N/A";
                if (instanceDetail.getTimeCreated() != null) {
                    long uptimeSeconds = java.time.Duration.between(
                            instanceDetail.getTimeCreated().toInstant(), 
                            Instant.now()
                    ).getSeconds();
                    uptime = formatUptime(uptimeSeconds);
                }
                
                // Build comprehensive message
                StringBuilder message = new StringBuilder();
                message.append(String.format("【%s - 详细信息】\n\n", instance.getName()));
                
                // Instance Status
                message.append("📌 实例状态\n");
                message.append(String.format("• 状态: %s\n", getStatusEmoji(instanceDetail.getLifecycleState()) + instanceDetail.getLifecycleState()));
                message.append(String.format("• 运行时长: %s\n", uptime));
                message.append(String.format("• 可用域: %s\n", instanceDetail.getAvailabilityDomain()));
                message.append(String.format("• 故障域: %s\n\n", instanceDetail.getFaultDomain() != null ? instanceDetail.getFaultDomain() : "N/A"));
                
                // Hardware Configuration
                message.append("⚙️ 硬件配置\n");
                message.append(String.format("• 配置: %s\n", instance.getShape()));
                message.append(String.format("• CPU核心: %s\n", getShapeCores(instance.getShape())));
                message.append(String.format("• 内存: %s\n", getShapeMemory(instance.getShape())));
                message.append(String.format("• 启动磁盘: %s\n\n", bootVolumeSize));
                
                // Resource Usage (1 hour average)
                message.append("📊 资源占用 (近1小时)\n");
                
                message.append(String.format("💻 CPU: %.2f%%\n", avgCpu));
                message.append(String.format("   %s\n", generateProgressBar(avgCpu)));
                
                message.append(String.format("💾 内存: %.2f%%\n", avgMem));
                message.append(String.format("   %s\n\n", generateProgressBar(avgMem)));
                
                // Disk I/O
                message.append("💿 磁盘I/O\n");
                message.append(String.format("• 读取: %.2f MB\n", diskRead / 1024 / 1024));
                message.append(String.format("• 写入: %.2f MB\n\n", diskWrite / 1024 / 1024));
                
                // Network Traffic
                message.append("🌐 网络流量\n");
                message.append(String.format("• 入站: %.2f MB\n", netIn / 1024 / 1024));
                message.append(String.format("• 出站: %.2f MB\n", netOut / 1024 / 1024));
                message.append(String.format("• 总计: %.2f MB\n\n", (netIn + netOut) / 1024 / 1024));
                
                // Network Info
                message.append("🔌 网络信息\n");
                String publicIps = instance.getPublicIp() != null && !instance.getPublicIp().isEmpty() 
                        ? String.join(", ", instance.getPublicIp()) 
                        : "无";
                message.append(String.format("• 公网IP: %s\n", publicIps));
                message.append(String.format("• 区域: %s\n\n", instance.getRegion()));
                
                // Note
                message.append("━━━━━━━━━━━━━━━━\n");
                message.append("💡 数据来自OCI监控\n");
                message.append("⚠️ 温度监控不可用(云实例)");
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("🔄 刷新", "view_resource_usage:" + instanceIndex)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "instance_resource_usage:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to get resource usage", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取资源占用失败\n\n" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "instance_resource_usage:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    private double queryMetric(MonitoringClient client, String compartmentId, String instanceId, 
                               String metricName, Instant startTime, Instant endTime) {
        try {
            SummarizeMetricsDataRequest request = SummarizeMetricsDataRequest.builder()
                    .compartmentId(compartmentId)
                    .summarizeMetricsDataDetails(SummarizeMetricsDataDetails.builder()
                            .namespace("oci_computeagent")
                            .query(String.format(
                                    "%s[1m]{resourceId = \"%s\"}.%s()",
                                    metricName,
                                    instanceId,
                                    metricName.contains("Bytes") ? "sum" : "mean"
                            ))
                            .startTime(Date.from(startTime))
                            .endTime(Date.from(endTime))
                            .build())
                    .build();
            
            SummarizeMetricsDataResponse response = client.summarizeMetricsData(request);
            
            if (!response.getItems().isEmpty() && 
                !response.getItems().get(0).getAggregatedDatapoints().isEmpty()) {
                return response.getItems().get(0).getAggregatedDatapoints().get(0).getValue();
            }
        } catch (Exception e) {
            log.warn("Failed to query metric {}: {}", metricName, e.getMessage());
        }
        return 0.0;
    }
    
    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (days > 0) {
            return String.format("%d天%d小时", days, hours);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
    
    private String getStatusEmoji(com.oracle.bmc.core.model.Instance.LifecycleState state) {
        switch (state) {
            case Running: return "✅ ";
            case Stopped: return "⏸ ";
            case Stopping: return "⏬ ";
            case Starting: return "⏫ ";
            case Terminated: return "❌ ";
            case Terminating: return "🗑 ";
            default: return "❓ ";
        }
    }
    
    private String getShapeCores(String shape) {
        if (shape.contains("VM.Standard.E2.1.Micro")) return "1核 (ARM)";
        if (shape.contains("VM.Standard.A1.Flex")) return "可变 (ARM)";
        if (shape.contains("VM.Standard.E4.Flex")) return "可变 (AMD)";
        if (shape.contains(".1")) return "1核";
        if (shape.contains(".2")) return "2核";
        if (shape.contains(".4")) return "4核";
        if (shape.contains(".8")) return "8核";
        return "查看配置";
    }
    
    private String getShapeMemory(String shape) {
        if (shape.contains("VM.Standard.E2.1.Micro")) return "1 GB";
        if (shape.contains("VM.Standard.A1.Flex")) return "可变";
        if (shape.contains("VM.Standard.E4.Flex")) return "可变";
        if (shape.contains(".1")) return "8-16 GB";
        if (shape.contains(".2")) return "16-32 GB";
        if (shape.contains(".4")) return "32-64 GB";
        return "查看配置";
    }
    
    private String generateProgressBar(double percentage) {
        int total = 20;
        int filled = (int) (percentage / 100.0 * total);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        return bar.toString();
    }
    
    @Override
    public String getCallbackPattern() {
        return "view_resource_usage:";
    }
}

/**
 * Instance resource usage config selector
 */
@Slf4j
@Component
class InstanceResourceUsageConfigSelectHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            if (CollectionUtil.isEmpty(users)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("【查看客户端负载】\n\n");
            message.append("请选择要查看的 OCI 配置：\n\n");
            
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
                                "instance_resource_usage:" + user.getOciCfg().getId()
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
        return "instance_resource_usage_select";
    }
}
