package com.tony.kingdetective.service.ops;

import cn.hutool.core.util.StrUtil;
import com.tony.kingdetective.exception.OciException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class SecretCryptoService {
    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${ops.ssh.secret-key:${web.password}}")
    private String secretKey;

    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new OciException(-1, "Failed to encrypt SSH secret: " + e.getMessage());
        }
    }

    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return null;
        }
        if (!cipherText.startsWith(PREFIX)) {
            return cipherText;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            if (payload.length <= IV_BYTES) {
                throw new OciException(-1, "Invalid SSH secret payload");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (OciException e) {
            throw e;
        } catch (Exception e) {
            throw new OciException(-1, "Failed to decrypt SSH secret. Check OPS_SSH_SECRET_KEY or web password.");
        }
    }

    private SecretKeySpec key() throws Exception {
        String source = StrUtil.blankToDefault(secretKey, "king-detective-default-ssh-secret");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
