package com.tony.kingdetective.service.impl;

import com.tony.kingdetective.exception.OciException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：OciServiceImpl.addCfg() 的私钥文件名路径遍历防护
 * （同 CVE-2025-13875 模式）。
 * 只测纯静态的文件名校验与路径解析，不启动 Spring、不连数据库、不调 OCI。
 */
class OciServiceImplKeyFilenameTest {

    private static final String KEY_DIR = "/app/king-detective/keys";

    @Test
    void normalFilenameIsAcceptedInsideKeyDir() {
        String resolved = OciServiceImpl.resolveKeyFilePath(KEY_DIR, "oci_api_key.pem");

        assertThat(Path.of(resolved).startsWith(Path.of(KEY_DIR).toAbsolutePath().normalize())).isTrue();
        assertThat(resolved).endsWith("oci_api_key.pem");
    }

    @Test
    void otherNormalFilenamesAreAccepted() {
        assertThat(OciServiceImpl.resolveKeyFilePath(KEY_DIR, "my-key_1.pem"))
                .endsWith("my-key_1.pem");
        assertThat(OciServiceImpl.resolveKeyFilePath(KEY_DIR, "key.pem"))
                .endsWith("key.pem");
        assertThat(OciServiceImpl.resolveKeyFilePath(KEY_DIR, "a.b_c-d.pem"))
                .endsWith("a.b_c-d.pem");
    }

    @Test
    void dotDotSlashTraversalIsRejected() {
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "../../etc/passwd"))
                .isInstanceOf(OciException.class)
                .hasMessageContaining("私钥文件名不合法");
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "..%2f..%2fetc%2fpasswd"))
                .isInstanceOf(OciException.class);
    }

    @Test
    void absolutePathAndSeparatorsAreRejected() {
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "/etc/passwd"))
                .isInstanceOf(OciException.class)
                .hasMessageContaining("私钥文件名不合法");
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "sub/key.pem"))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "..\\..\\windows\\secret.pem"))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "a\\b.pem"))
                .isInstanceOf(OciException.class);
    }

    @Test
    void dotAndDotDotFilenamesAreRejected() {
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "."))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, ".."))
                .isInstanceOf(OciException.class);
    }

    @Test
    void blankAndIllegalFilenamesAreRejected() {
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, null))
                .isInstanceOf(OciException.class)
                .hasMessageContaining("私钥文件名不能为空");
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, ""))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "key pem.pem"))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "key;rm.pem"))
                .isInstanceOf(OciException.class);
    }

    // ---- uploadCfg() 回归测试：配置文件内 key_file 值的路径遍历防护 ----

    @Test
    void uploadCfgKeyFileTraversalIsRejected() {
        // key_file 写成相对穿越路径：旧代码直接拼到密钥目录后即可逃出
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "../../etc/evil.pem"))
                .isInstanceOf(OciException.class)
                .hasMessageContaining("私钥文件名不合法");
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "keys/../../etc/evil.pem"))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "..\\..\\windows\\evil.pem"))
                .isInstanceOf(OciException.class);
    }

    @Test
    void uploadCfgKeyFileAbsolutePathIsRejected() {
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "/etc/passwd"))
                .isInstanceOf(OciException.class);
        assertThatThrownBy(() -> OciServiceImpl.resolveKeyFilePath(KEY_DIR, "/home/opc/.oci/oci_api_key.pem"))
                .isInstanceOf(OciException.class);
    }

    @Test
    void uploadCfgKeyFileNormalNameResolvesInsideKeyDir() {
        String resolved = OciServiceImpl.resolveKeyFilePath(KEY_DIR, "oci_api_key.pem");

        assertThat(Path.of(resolved).startsWith(Path.of(KEY_DIR).toAbsolutePath().normalize())).isTrue();
        assertThat(resolved).endsWith("oci_api_key.pem");
    }

    @Test
    void uploadCfgFilenameSuffixWhitelist() {
        assertThat(OciServiceImpl.isAllowedCfgFilename("config.ini")).isTrue();
        assertThat(OciServiceImpl.isAllowedCfgFilename("config.txt")).isTrue();
        assertThat(OciServiceImpl.isAllowedCfgFilename("CONFIG.INI")).isTrue();
        // 旧 contains() 检查会放行的绕过形式，现在一律拒绝
        assertThat(OciServiceImpl.isAllowedCfgFilename("config.ini.exe")).isFalse();
        assertThat(OciServiceImpl.isAllowedCfgFilename("config.ini/../../x")).isFalse();
        assertThat(OciServiceImpl.isAllowedCfgFilename("config.pdf")).isFalse();
        assertThat(OciServiceImpl.isAllowedCfgFilename(null)).isFalse();
        assertThat(OciServiceImpl.isAllowedCfgFilename("")).isFalse();
    }
}
