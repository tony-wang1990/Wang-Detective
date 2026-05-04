package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.usageapi.UsageapiClient;
import com.oracle.bmc.usageapi.model.*;
import com.oracle.bmc.usageapi.requests.RequestSummarizedUsagesRequest;
import com.oracle.bmc.usageapi.responses.RequestSummarizedUsagesResponse;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Cost query handler - shows spending for last 3 months
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class CostQueryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            if (users == null || users.isEmpty()) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置\n\n请先添加 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            // Use first config
            SysUserDTO user = users.get(0);
            
            StringBuilder message = new StringBuilder();
            message.append("【近3月花费查询】\n\n");
            message.append(String.format("租户: %s\n", user.getUsername()));
            message.append(String.format("区域: %s\n\n", user.getOciCfg().getRegion()));
            message.append("━━━━━━━━━━━━━━━━\n\n");
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                UsageapiClient usageClient = UsageapiClient.builder()
                        .build(fetcher.getAuthenticationDetailsProvider());
                
                // Calculate date range (last 3 months) in UTC
                LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
                LocalDate startDate = endDate.minusMonths(3);
                
                // Query usage data
                RequestSummarizedUsagesDetails requestDetails = RequestSummarizedUsagesDetails.builder()
                        .tenantId(user.getOciCfg().getTenantId())
                        // OCI Usage API requires start/end time to be 00:00:00 UTC
                        .timeUsageStarted(Date.from(startDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                        .timeUsageEnded(Date.from(endDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                        .granularity(RequestSummarizedUsagesDetails.Granularity.Monthly)
                        .queryType(RequestSummarizedUsagesDetails.QueryType.Cost)
                        .groupBy(Arrays.asList("service"))
                        .build();
                
                RequestSummarizedUsagesRequest request = RequestSummarizedUsagesRequest.builder()
                        .requestSummarizedUsagesDetails(requestDetails)
                        .build();
                
                RequestSummarizedUsagesResponse response = usageClient.requestSummarizedUsages(request);
                
                List<UsageSummary> usageSummaries = response.getUsageAggregation().getItems();
                
                if (usageSummaries == null || usageSummaries.isEmpty()) {
                    message.append("📊 近3个月暂无费用记录\n\n");
                    message.append("💡 可能原因：\n");
                    message.append("  • 账户为永久免费层\n");
                    message.append("  • 未超出免费额度\n");
                    message.append("  • 数据同步延迟\n");
                } else {
                    // Group by month
                    Map<String, Map<String, BigDecimal>> monthlyCosts = new TreeMap<>();
                    BigDecimal totalCost = BigDecimal.ZERO;
                    
                    for (UsageSummary summary : usageSummaries) {
                        String month = formatMonth(summary.getTimeUsageStarted());
                        String service = summary.getService() != null ? summary.getService() : "其他";
                        BigDecimal cost = summary.getComputedAmount() != null 
                                ? summary.getComputedAmount() 
                                : BigDecimal.ZERO;
                        
                        monthlyCosts
                                .computeIfAbsent(month, k -> new LinkedHashMap<>())
                                .merge(service, cost, BigDecimal::add);
                        
                        totalCost = totalCost.add(cost);
                    }
                    
                    // Display monthly breakdown
                    for (Map.Entry<String, Map<String, BigDecimal>> monthEntry : monthlyCosts.entrySet()) {
                        String month = monthEntry.getKey();
                        Map<String, BigDecimal> services = monthEntry.getValue();
                        
                        BigDecimal monthTotal = services.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        message.append(String.format("📅 %s\n", month));
                        message.append(String.format("总计: $%.2f\n\n", monthTotal));
                        
                        // Top 5 services
                        services.entrySet().stream()
                                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                                .limit(5)
                                .forEach(entry -> {
                                    message.append(String.format("  • %s: $%.2f\n", 
                                            entry.getKey(), 
                                            entry.getValue()));
                                });
                        
                        message.append("\n");
                    }
                    
                    message.append("━━━━━━━━━━━━━━━━\n");
                    message.append(String.format("💰 3个月总计: $%.2f\n", totalCost));
                    message.append(String.format("📊 月均花费: $%.2f\n", 
                            totalCost.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)));
                }
                
                usageClient.close();
                
            } catch (Exception e) {
                log.error("Failed to query cost data", e);
                return buildEditMessage(
                        callbackQuery,
                        "❌ 查询花费失败\n\n" + e.getMessage(),
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            message.append("\n💡 数据来自 OCI Usage API");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to handle cost query", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 处理失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    /**
     * Format date to month string
     */
    private String formatMonth(Date date) {
        if (date == null) {
            return "未知";
        }
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.format(DateTimeFormatter.ofPattern("yyyy年MM月"));
    }
    
    @Override
    public String getCallbackPattern() {
        return "cost_query";
    }
}
