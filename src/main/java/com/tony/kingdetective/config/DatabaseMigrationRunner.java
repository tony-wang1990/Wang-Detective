package com.tony.kingdetective.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * 数据库自动迁移器
 * 在应用启动时自动检查并执行数据库迁移
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("================== 开始检查数据库迁移 ==================");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 检查新版本表是否存在 (只要缺一个就执行迁移)
            if (!tableExists(conn, "audit_log") || 
                !tableExists(conn, "ip_blacklist") || 
                !tableExists(conn, "login_attempts")) {
                log.info("检测到数据库结构缺失，开始执行 v4.0 全量迁移...");
                executeMigrationScript(stmt);
                log.info("✅ 数据库迁移完成！");
            } else {
                log.info("✅ 数据库已是最新版本，无需迁移");
            }
            
        } catch (Exception e) {
            log.error("❌ 数据库迁移失败", e);
            // 不抛出异常，允许应用继续启动
            log.warn("应用将继续启动，但部分功能可能不可用");
        }
        
        log.info("================== 数据库迁移检查完成 ==================");
    }
    
    /**
     * 检查表是否存在
     */
    private boolean tableExists(Connection conn, String tableName) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
    
    /**
     * 执行迁移脚本
     */
    private void executeMigrationScript(Statement stmt) throws Exception {
        // 读取 SQL 脚本
        ClassPathResource resource = new ClassPathResource("db/migration_v4_0.sql");
        
        String sql;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        
        // 分割并执行每条 SQL 语句
        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    log.debug("执行 SQL: {}", trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                    stmt.execute(trimmed);
                } catch (Exception e) {
                    // 忽略 "duplicate column" 或 "table already exists" 错误，确保幂等性
                    String msg = e.getMessage().toLowerCase();
                    if (msg.contains("duplicate column") || msg.contains("exists")) {
                        log.warn("忽略已存在的结构: {}", trimmed.split("\n")[0]);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }
}
