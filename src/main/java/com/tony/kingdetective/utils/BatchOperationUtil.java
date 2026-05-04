package com.tony.kingdetective.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.service.IOciKvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量操作工具类
 * 优化数据库批量查询和更新操作
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class BatchOperationUtil {
    
    /**
     * 批量查询（减少数据库往返次数）
     *
     * @param ids ID列表
     * @param queryFunction 查询函数
     * @param <T> 实体类型
     * @return ID到实体的映射
     */
    public static <T> Map<String, T> batchQuery(
            List<String> ids,
            Function<List<String>, List<T>> queryFunction,
            Function<T, String> idExtractor) {
        
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        
        long startTime = System.currentTimeMillis();
        
        // 执行批量查询
        List<T> results = queryFunction.apply(ids);
        
        // 转换为 Map
        Map<String, T> resultMap = results.stream()
                .collect(Collectors.toMap(idExtractor, Function.identity()));
        
        long duration = System.currentTimeMillis() - startTime;
        log.debug("批量查询完成: {} 条记录, 耗时: {}ms", results.size(), duration);
        
        return resultMap;
    }
    
    /**
     * 批量更新（减少数据库往返次数）
     *
     * @param items 待更新的项目列表
     * @param batchSize 批次大小
     * @param updateFunction 更新函数
     * @param <T> 实体类型
     * @return 成功更新的数量
     */
    public static <T> int batchUpdate(
            List<T> items,
            int batchSize,
            Function<List<T>, Integer> updateFunction) {
        
        if (items == null || items.isEmpty()) {
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        int totalUpdated = 0;
        
        // 分批处理
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            List<T> batch = items.subList(i, endIndex);
            
            int updated = updateFunction.apply(batch);
            totalUpdated += updated;
            
            log.debug("批次 {}/{} 完成: 更新 {} 条记录",
                    (i / batchSize) + 1,
                    ((items.size() + batchSize - 1) / batchSize),
                    updated);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("批量更新完成: {} 条记录, 耗时: {}ms", totalUpdated, duration);
        
        return totalUpdated;
    }
    
    /**
     * 批量删除
     *
     * @param ids ID列表
     * @param batchSize 批次大小
     * @param deleteFunction 删除函数
     * @return 成功删除的数量
     */
    public static int batchDelete(
            List<String> ids,
            int batchSize,
            Function<List<String>, Integer> deleteFunction) {
        
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        int totalDeleted = 0;
        
        // 分批处理
        for (int i = 0; i < ids.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, ids.size());
            List<String> batch = ids.subList(i, endIndex);
            
            int deleted = deleteFunction.apply(batch);
            totalDeleted += deleted;
            
            log.debug("批次 {}/{} 完成: 删除 {} 条记录",
                    (i / batchSize) + 1,
                    ((ids.size() + batchSize - 1) / batchSize),
                    deleted);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("批量删除完成: {} 条记录, 耗时: {}ms", totalDeleted, duration);
        
        return totalDeleted;
    }
}
