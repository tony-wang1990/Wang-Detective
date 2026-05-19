package com.tony.kingdetective.telegram.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IOciUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TG Bot 账号添加流程 Service
 *
 * 重构说明（TgBot 瘦身）：
 * 原 TgBot.handleAddAccountRemarkInput() 中包含：
 *  1. key 文件保存逻辑
 *  2. OciUser 实体构建与数据库保存
 *  3. OCI 连通性验证（listAllRegions）
 *  4. 验证失败时的回滚逻辑
 *
 * 以上纯数据操作与 TgBot 类无关，提取到本 Service 后，TgBot 只需调用并根据
 * 返回结果发送对应消息，不再内联任何数据库或 OCI 操作。
 *
 * @author Tony Wang
 */
@Slf4j
@Service
public class TgAccountFlowService {

    @Resource
    private IOciUserService userService;

    @Value("${oci-cfg.key-dir-path}")
    private String keyDirPath;

    /**
     * 保存账号并验证 OCI 连通性
     *
     * @param remark      用户设置的备注名
     * @param userOctId   OCI User OCID
     * @param fingerprint API Key 指纹
     * @param tenancy     Tenancy OCID
     * @param region      区域 ID
     * @param keyContent  私钥内容（PEM 格式）
     * @return 验证成功则返回 null；失败则返回错误信息（调用方用于推送 TG 提示）
     */
    public String saveAndVerify(String remark, String userOctId, String fingerprint,
                                String tenancy, String region, String keyContent) {

        // 1. 准备目录
        if (!FileUtil.exist(keyDirPath)) {
            FileUtil.mkdir(keyDirPath);
        }

        // 2. 写入 key 文件
        String safeRemark = remark.replaceAll("[^a-zA-Z0-9_-]", "_");
        String keyFileName = String.format("oci_api_key_%s_%d.pem", safeRemark, System.currentTimeMillis());
        String keyPath = keyDirPath + File.separator + keyFileName;

        try {
            FileUtil.writeUtf8String(keyContent, keyPath);
            // 设置文件权限 600（仅 Linux 生效）
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                Files.setPosixFilePermissions(Paths.get(keyPath),
                        Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            }
        } catch (Exception e) {
            log.error("写入 key 文件失败: {}", keyPath, e);
            return "保存私钥文件失败：" + e.getMessage();
        }

        // 3. 构建实体并保存数据库
        OciUser ociUser = new OciUser();
        ociUser.setId(IdUtil.getSnowflakeNextIdStr());
        ociUser.setUsername(remark);
        ociUser.setOciTenantId(tenancy);
        ociUser.setOciUserId(userOctId);
        ociUser.setOciFingerprint(fingerprint);
        ociUser.setOciRegion(region);
        ociUser.setOciKeyPath(keyPath);
        ociUser.setTenantName(remark);
        ociUser.setCreateTime(LocalDateTime.now());
        ociUser.setDeleted(0);

        userService.save(ociUser);
        log.info("账号已保存到数据库: id={}, remark={}", ociUser.getId(), remark);

        // 4. OCI 连通性验证
        SysUserDTO sysUserDTO = SysUserDTO.builder()
                .username(remark)
                .ociCfg(SysUserDTO.OciCfg.builder()
                        .tenantId(tenancy)
                        .userId(userOctId)
                        .fingerprint(fingerprint)
                        .region(region)
                        .privateKey(keyContent)
                        .build())
                .build();

        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            List<?> verifiedRegions = fetcher.listAllRegions();
            log.info("OCI 连通性验证成功: remark={}, regions={}", remark, verifiedRegions.size());
            return null; // 成功
        } catch (Exception verifyEx) {
            // 验证失败：回滚
            log.error("OCI 连通性验证失败，回滚账号: remark={}", remark, verifyEx);
            try { userService.removeById(ociUser.getId()); } catch (Exception ignored) {}
            try { FileUtil.del(keyPath); } catch (Exception ignored) {}
            return verifyEx.getMessage(); // 返回失败原因
        }
    }

    /**
     * 从 config 文本中提取指定 key 的值（支持 `key=value` 或 `key = value`）
     */
    public String getValueFromConfig(String text, String key) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
                return line.split("=", 2)[1].trim();
            }
        }
        return null;
    }

    /**
     * 验证私钥内容格式
     */
    public boolean isValidPrivateKey(String keyContent) {
        return keyContent != null &&
               (keyContent.contains("BEGIN") && keyContent.contains("PRIVATE KEY"));
    }

    /**
     * 解析 config 文本，返回解析后的字段 Map；缺少必要字段时返回 null
     */
    public Map<String, String> parseConfigText(String text) {
        String user = getValueFromConfig(text, "user");
        String fingerprint = getValueFromConfig(text, "fingerprint");
        String tenancy = getValueFromConfig(text, "tenancy");
        String region = getValueFromConfig(text, "region");

        if (user == null || fingerprint == null || tenancy == null || region == null) {
            return null;
        }

        return Map.of(
                "user", user,
                "fingerprint", fingerprint,
                "tenancy", tenancy,
                "region", region,
                "rawConfig", text
        );
    }
}
