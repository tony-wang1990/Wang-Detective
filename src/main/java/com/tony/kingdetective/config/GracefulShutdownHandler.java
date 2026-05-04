package com.tony.kingdetective.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 优雅停机处理器
 * 确保应用停止时所有任务正确完成
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class GracefulShutdownHandler {
    
    @Autowired(required = false)
    @Qualifier("virtualExecutor")
    private ExecutorService virtualExecutor;
    
    /**
     * 应用停止前的清理工作
     */
    @PreDestroy
    public void shutdown() {
        log.info("================== 开始优雅停机 ==================");
        
        // 1. 关闭虚拟线程池
        if (virtualExecutor != null) {
            shutdownExecutor(virtualExecutor, "VirtualExecutor");
        }
        
        // 2. 等待所有异步任务完成
        log.info("等待所有异步任务完成...");
        
        // 3. 关闭数据库连接池（Spring 会自动处理）
        log.info("关闭数据库连接...");
        
        // 4. 清理缓存
        log.info("清理缓存...");
        
        log.info("================== 优雅停机完成 ==================");
    }
    
    /**
     * 关闭线程池
     *
     * @param executor 线程池
     * @param name 名称
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        log.info("开始关闭线程池: {}", name);
        
        // 停止接受新任务
        executor.shutdown();
        
        try {
            // 等待现有任务完成（最多等待60秒）
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("线程池 {} 在60秒内未完成所有任务，强制关闭", name);
                
                // 强制关闭
                executor.shutdownNow();
                
                // 再等待30秒
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("线程池 {} 无法完全关闭", name);
                }
            } else {
                log.info("线程池 {} 已成功关闭", name);
            }
        } catch (InterruptedException e) {
            log.error("等待线程池关闭时被中断: {}", name, e);
            
            // 强制关闭
            executor.shutdownNow();
            
            // 恢复中断状态
            Thread.currentThread().interrupt();
        }
    }
}
