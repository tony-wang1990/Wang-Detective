package com.tony.kingdetective.constant;

/**
 * OCI 常量定义
 * 
 * @author Tony Wang
 */
public class OciConstants {
    
    // ==================== 重试配置 ====================
    
    /** API 调用最大重试次数 */
    public static final int MAX_RETRY_COUNT = 10;
    
    /** 重试延迟（毫秒） */
    public static final long RETRY_DELAY_MS = 30000;
    
    /** Work Request 轮询间隔（毫秒） */
    public static final long WORK_REQUEST_POLL_INTERVAL_MS = 5000;
    
    // ==================== 网络配置 ====================
    
    /** 默认 CIDR 块 */
    public static final String DEFAULT_CIDR_BLOCK = "10.0.0.0/16";
    
    /** 默认子网 CIDR */
    public static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/24";
    
    /** SSH 默认端口 */
    public static final int SSH_DEFAULT_PORT = 22;
    
    /** VNC 默认端口 */
    public static final int VNC_DEFAULT_PORT = 5901;
    
    /** HTTP 默认端口 */
    public static final int HTTP_DEFAULT_PORT = 80;
    
    /** HTTPS 默认端口 */
    public static final int HTTPS_DEFAULT_PORT = 443;
    
    // ==================== 实例配置 ====================
    
    /** 实例创建超时（分钟） */
    public static final int INSTANCE_CREATE_TIMEOUT_MINUTES = 30;
    
    /** 默认 Shape */
    public static final String DEFAULT_SHAPE = "VM.Standard.A1.Flex";
    
    /** 默认 OCPU 数量 */
    public static final int DEFAULT_OCPU_COUNT = 4;
    
    /** 默认内存（GB） */
    public static final int DEFAULT_MEMORY_GB = 24;
    
    /** 默认引导卷大小（GB） */
    public static final int DEFAULT_BOOT_VOLUME_SIZE_GB = 50;
    
    // ==================== 0Mbps NLB 配置 ====================
    
    /** NLB 名称前缀 */
    public static final String NLB_NAME_PREFIX = "king-detective-nlb-";
    
    /** NAT 网关名称前缀 */
    public static final String NAT_GATEWAY_NAME_PREFIX = "king-detective-nat-";
    
    /** 路由表名称前缀 */
    public static final String ROUTE_TABLE_NAME_PREFIX = "king-detective-rt-";
    
    /** NLB 带宽（Mbps） */
    public static final int NLB_BANDWIDTH_MBPS = 500;
    
    // ==================== API 限制 ====================
    
    /** 配额查询频率限制（次/秒） */
    public static final double QUOTA_QUERY_RATE_LIMIT = 2.0;
    
    /** 成本查询频率限制（次/秒） */
    public static final double COST_QUERY_RATE_LIMIT = 1.0;
    
    /** 实例创建频率限制（次/秒） */
    public static final double INSTANCE_CREATE_RATE_LIMIT = 0.5;
    
    /** 用户请求频率限制（次/秒） */
    public static final double USER_RATE_LIMIT = 10.0;
    
    // ==================== 缓存配置 ====================
    
    /** 用户配置缓存时间（分钟） */
    public static final int USER_CONFIG_CACHE_MINUTES = 10;
    
    /** 区域列表缓存时间（分钟） */
    public static final int REGION_LIST_CACHE_MINUTES = 60;
    
    /** 配额信息缓存时间（分钟） */
    public static final int QUOTA_CACHE_MINUTES = 5;
    
    /** 实例列表缓存时间（分钟） */
    public static final int INSTANCE_LIST_CACHE_MINUTES = 2;
    
    // ==================== Telegram Bot 配置 ====================
    
    /** 消息最大长度 */
    public static final int MAX_MESSAGE_LENGTH = 4096;
    
    /** 按钮最大文本长度 */
    public static final int MAX_BUTTON_TEXT_LENGTH = 64;
    
    /** 分页大小 */
    public static final int PAGE_SIZE = 10;
    
    // ==================== 安全配置 ====================
    
    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    /** 用户名最小长度 */
    public static final int USERNAME_MIN_LENGTH = 3;
    
    /** 用户名最大长度 */
    public static final int USERNAME_MAX_LENGTH = 32;
    
    /** API 密钥最小长度 */
    public static final int API_KEY_MIN_LENGTH = 32;
    
    // ==================== 文件路径 ====================
    
    /** 备份文件目录 */
    public static final String BACKUP_DIR = "backups";
    
    /** 日志文件目录 */
    public static final String LOG_DIR = "logs";
    
    /** 临时文件目录 */
    public static final String TEMP_DIR = "temp";
    
    // ==================== HTTP 状态码 ====================
    
    /** 请求过于频繁 */
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    
    /** 服务暂时不可用 */
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;
    
    /** 网关超时 */
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    
    // ==================== 消息模板 ====================
    
    public static class MessageTemplate {
        /** 实例创建成功广播 */
        public static final String CREATE_SUCCESS_BROADCAST = 
            "【开机任务】 \n\n🎉 用户：[%s] 开机成功 🎉\n\n" +
            "📍 区域：%s\n" +
            "💾 配置：%s | %dC%dG\n" +
            "📊 计费：%s\n" +
            "🌐 公网IP：%s\n" +
            "🔐 SSH端口：%d\n\n" +
            "%s";
        
        /** 实例创建失败广播 */
        public static final String CREATE_FAILURE_BROADCAST = 
            "【开机失败】\n\n❌ 用户：[%s]\n\n" +
            "📍 区域：%s\n" +
            "💾 配置：%s\n" +
            "❗错误：%s";
        
        /** 任务完成通知 */
        public static final String TASK_COMPLETE = 
            "✅ 任务完成\n\n%s";
        
        /** 任务失败通知 */
        public static final String TASK_FAILED = 
            "❌ 任务失败\n\n%s\n\n错误：%s";
        
        /** API 调用失败 */
        public static final String API_CALL_FAILED = 
            "❌ API 调用失败\n\n" +
            "接口：%s\n" +
            "错误：%s";
        
        /** 权限不足 */
        public static final String PERMISSION_DENIED = 
            "❌ 权限不足\n\n" +
            "您没有权限执行此操作";
        
        /** 操作成功 */
        public static final String OPERATION_SUCCESS = 
            "✅ 操作成功\n\n%s";
        
        /** 操作失败 */
        public static final String OPERATION_FAILED = 
            "❌ 操作失败\n\n%s";
    }
    
    // ==================== 配置键 ====================
    
    public static class ConfigKey {
        /** VNC URL 配置键 */
        public static final String VNC_URL = "SYS_VNC";
        
        /** 每日报告开关 */
        public static final String DAILY_REPORT_ENABLED = "daily_report_enabled";
        
        /** 实例监控开关 */
        public static final String INSTANCE_MONITORING_ENABLED = "instance_monitoring_enabled";
        
        /** 自动重启开关 */
        public static final String AUTO_RESTART_ENABLED = "auto_restart_enabled";
        
        /** 防御模式开关 */
        public static final String DEFENSE_MODE_ENABLED = "defense_mode_enabled";
        
        /** 区域拓展开关 */
        public static final String AUTO_REGION_EXPANSION_ENABLED = "auto_region_expansion_enabled";
    }
    
    private OciConstants() {
        // 防止实例化
        throw new AssertionError("常量类不应该被实例化");
    }
}
