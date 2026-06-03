package com.tony.kingdetective.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AdminCredentialService {

    private static final String ADMIN_CREDENTIAL_TYPE = "Y004";
    private static final String ADMIN_ACCOUNT_CODE = "SYS_WEB_ACCOUNT";
    private static final String ADMIN_PASSWORD_CODE = "SYS_WEB_PASSWORD";

    private final IOciKvService kvService;

    @Value("${web.account:admin}")
    private String defaultAccount;

    @Value("${web.password:admin123456}")
    private String defaultPassword;

    public AdminCredentialService(IOciKvService kvService) {
        this.kvService = kvService;
    }

    public String getAccount() {
        return StrUtil.blankToDefault(readValue(ADMIN_ACCOUNT_CODE), defaultAccount);
    }

    public String getPassword() {
        return StrUtil.blankToDefault(readValue(ADMIN_PASSWORD_CODE), defaultPassword);
    }

    public boolean matches(String account, String password) {
        return StrUtil.equals(account, getAccount()) && StrUtil.equals(password, getPassword());
    }

    public String generateToken(Map<String, Object> payload) {
        return CommonUtils.genToken(payload, getPassword());
    }

    public boolean verifyToken(String token) {
        String secret = getPassword();
        return StrUtil.isNotBlank(token)
                && StrUtil.isNotBlank(secret)
                && !CommonUtils.isTokenExpired(token)
                && JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCredential(String currentPassword, String newAccount, String newPassword, String confirmPassword) {
        if (!StrUtil.equals(currentPassword, getPassword())) {
            throw new OciException(-1, "当前密码不正确");
        }
        if (StrUtil.isBlank(newAccount)) {
            throw new OciException(-1, "新登录账号不能为空");
        }
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 8) {
            throw new OciException(-1, "新密码至少需要 8 位");
        }
        if (!StrUtil.equals(newPassword, confirmPassword)) {
            throw new OciException(-1, "两次输入的新密码不一致");
        }
        saveValue(ADMIN_ACCOUNT_CODE, newAccount.trim());
        saveValue(ADMIN_PASSWORD_CODE, newPassword);
    }

    private String readValue(String code) {
        OciKv kv = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, code)
                .orderByDesc(OciKv::getCreateTime)
                .last("limit 1"));
        return kv == null ? null : kv.getValue();
    }

    private void saveValue(String code, String value) {
        kvService.remove(new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, code));
        kvService.save(OciKv.builder()
                .id(IdUtil.getSnowflakeNextIdStr())
                .code(code)
                .type(ADMIN_CREDENTIAL_TYPE)
                .value(value)
                .createTime(LocalDateTime.now())
                .build());
    }
}
