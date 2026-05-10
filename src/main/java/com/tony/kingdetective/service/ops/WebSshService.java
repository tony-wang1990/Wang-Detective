package com.tony.kingdetective.service.ops;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.tony.kingdetective.bean.params.ops.SftpParams;
import com.tony.kingdetective.bean.params.ops.SshCommandParams;
import com.tony.kingdetective.bean.params.ops.SshCredentialParams;
import com.tony.kingdetective.bean.response.ops.SftpListRsp;
import com.tony.kingdetective.bean.response.ops.SshCommandRsp;
import com.tony.kingdetective.exception.OciException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

@Slf4j
@Service
public class WebSshService {
    private static final int DEFAULT_PORT = 22;
    private static final int MAX_TEXT_FILE_BYTES = 1024 * 1024;
    private static final int MAX_TRANSFER_BYTES = 50 * 1024 * 1024;

    public Session openSession(SshCredentialParams credential) {
        validateCredential(credential);
        try {
            JSch jsch = new JSch();
            if (StrUtil.isNotBlank(credential.getPrivateKey())) {
                byte[] privateKey = normalizePrivateKey(credential.getPrivateKey()).getBytes(StandardCharsets.UTF_8);
                byte[] passphrase = StrUtil.isBlank(credential.getPassphrase())
                        ? null
                        : credential.getPassphrase().getBytes(StandardCharsets.UTF_8);
                jsch.addIdentity(identityName(credential), privateKey, null, passphrase);
            }

            Session session = jsch.getSession(credential.getUsername(), credential.getHost(), port(credential));
            if (StrUtil.isNotBlank(credential.getPassword())) {
                session.setPassword(credential.getPassword());
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setConfig(config);
            session.setServerAliveInterval(30_000);
            session.connect(timeoutMillis(credential.getConnectTimeoutSeconds(), 10));
            return session;
        } catch (Exception e) {
            log.warn("SSH connection failed: host={}, user={}, reason={}", credential.getHost(), credential.getUsername(), e.getMessage());
            throw new OciException(-1, "SSH connection failed: " + e.getMessage());
        }
    }

    public boolean testConnection(SshCredentialParams credential) {
        Session session = null;
        try {
            session = openSession(credential);
            return session.isConnected();
        } finally {
            disconnect(session);
        }
    }

    public SshCommandRsp execute(SshCommandParams params) {
        if (params == null || params.getCredential() == null || StrUtil.isBlank(params.getCommand())) {
            throw new OciException(-1, "SSH credential and command are required");
        }

        long startedAt = System.currentTimeMillis();
        Session session = null;
        ChannelExec channel = null;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        boolean timedOut = false;
        int exitStatus = -1;
        try {
            session = openSession(params.getCredential());
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(params.getCommand());
            channel.setInputStream(null);
            InputStream out = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();

            long deadline = startedAt + timeoutMillis(params.getTimeoutSeconds(), 30);
            byte[] buffer = new byte[4096];
            while (!channel.isClosed()) {
                drain(out, stdout, buffer);
                drain(err, stderr, buffer);
                if (System.currentTimeMillis() > deadline) {
                    timedOut = true;
                    break;
                }
                Thread.sleep(80);
            }
            drain(out, stdout, buffer);
            drain(err, stderr, buffer);
            exitStatus = channel.getExitStatus();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            timedOut = true;
        } catch (Exception e) {
            stderr.writeBytes(errorMessage(e).getBytes(StandardCharsets.UTF_8));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            disconnect(session);
        }

        return SshCommandRsp.builder()
                .host(params.getCredential().getHost())
                .exitStatus(exitStatus)
                .timedOut(timedOut)
                .durationMillis(System.currentTimeMillis() - startedAt)
                .stdout(stdout.toString(StandardCharsets.UTF_8))
                .stderr(stderr.toString(StandardCharsets.UTF_8))
                .build();
    }

    public SftpListRsp list(SftpParams params) {
        return withSftp(params, channel -> {
            String path = normalizePath(params.getPath());
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> vector = channel.ls(path);
            List<SftpListRsp.Entry> entries = vector.stream()
                    .filter(item -> !".".equals(item.getFilename()) && !"..".equals(item.getFilename()))
                    .map(item -> SftpListRsp.Entry.builder()
                            .name(item.getFilename())
                            .path(joinRemotePath(path, item.getFilename()))
                            .directory(item.getAttrs().isDir())
                            .size(item.getAttrs().getSize())
                            .permissions(item.getAttrs().getPermissions())
                            .modifiedTime((long) item.getAttrs().getMTime() * 1000)
                            .build())
                    .sorted(Comparator.comparing(SftpListRsp.Entry::getDirectory).reversed()
                            .thenComparing(SftpListRsp.Entry::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return SftpListRsp.builder().path(path).entries(entries).build();
        });
    }

    public String readText(SftpParams params) {
        return withSftp(params, channel -> {
            String path = normalizePath(params.getPath());
            SftpATTRS attrs = channel.stat(path);
            if (attrs.isDir()) {
                throw new OciException(-1, "Cannot read a directory");
            }
            if (attrs.getSize() > MAX_TEXT_FILE_BYTES) {
                throw new OciException(-1, "File is larger than 1MB, text preview is blocked");
            }
            try (InputStream input = channel.get(path)) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        });
    }

    public byte[] download(SftpParams params) {
        return withSftp(params, channel -> {
            String path = normalizePath(params.getPath());
            SftpATTRS attrs = channel.stat(path);
            if (attrs.isDir()) {
                throw new OciException(-1, "Cannot download a directory");
            }
            if (attrs.getSize() > MAX_TRANSFER_BYTES) {
                throw new OciException(-1, "File is larger than 50MB, download is blocked");
            }
            try (InputStream input = channel.get(path)) {
                return input.readAllBytes();
            }
        });
    }

    public void upload(SftpParams params, InputStream input) {
        withSftp(params, channel -> {
            channel.put(input, normalizePath(params.getPath()));
            return null;
        });
    }

    public void writeText(SftpParams params) {
        withSftp(params, channel -> {
            String content = params.getContent() == null ? "" : params.getContent();
            try (ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                channel.put(input, normalizePath(params.getPath()));
            }
            return null;
        });
    }

    public void mkdir(SftpParams params) {
        withSftp(params, channel -> {
            channel.mkdir(normalizePath(params.getPath()));
            return null;
        });
    }

    public void delete(SftpParams params) {
        withSftp(params, channel -> {
            String path = normalizePath(params.getPath());
            rejectDangerousDeletePath(path);
            SftpATTRS attrs = channel.stat(path);
            if (attrs.isDir()) {
                if (Boolean.TRUE.equals(params.getRecursive())) {
                    deleteDirectory(channel, path);
                } else {
                    channel.rmdir(path);
                }
            } else {
                channel.rm(path);
            }
            return null;
        });
    }

    public void rename(SftpParams params) {
        withSftp(params, channel -> {
            if (StrUtil.isBlank(params.getTargetPath())) {
                throw new OciException(-1, "Target path is required");
            }
            channel.rename(normalizePath(params.getPath()), normalizePath(params.getTargetPath()));
            return null;
        });
    }

    private <T> T withSftp(SftpParams params, SftpCallback<T> callback) {
        if (params == null || params.getCredential() == null) {
            throw new OciException(-1, "SSH credential is required");
        }
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = openSession(params.getCredential());
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(timeoutMillis(params.getCredential().getConnectTimeoutSeconds(), 10));
            return callback.apply(channel);
        } catch (OciException e) {
            throw e;
        } catch (Exception e) {
            throw new OciException(-1, "SFTP operation failed: " + errorMessage(e));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            disconnect(session);
        }
    }

    private void deleteDirectory(ChannelSftp channel, String path) throws Exception {
        rejectDangerousDeletePath(path);
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = channel.ls(path);
        for (ChannelSftp.LsEntry entry : entries) {
            if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                continue;
            }
            String child = joinRemotePath(path, entry.getFilename());
            if (entry.getAttrs().isDir()) {
                deleteDirectory(channel, child);
            } else {
                channel.rm(child);
            }
        }
        channel.rmdir(path);
    }

    private void validateCredential(SshCredentialParams credential) {
        if (credential == null || StrUtil.isBlank(credential.getHost()) || StrUtil.isBlank(credential.getUsername())) {
            throw new OciException(-1, "SSH host and username are required");
        }
        if (StrUtil.isBlank(credential.getPassword()) && StrUtil.isBlank(credential.getPrivateKey())) {
            throw new OciException(-1, "SSH password or private key is required");
        }
    }

    private int port(SshCredentialParams credential) {
        return credential.getPort() == null || credential.getPort() <= 0 ? DEFAULT_PORT : credential.getPort();
    }

    private int timeoutMillis(Integer seconds, int defaultSeconds) {
        int value = seconds == null || seconds <= 0 ? defaultSeconds : Math.min(seconds, 300);
        return value * 1000;
    }

    private void drain(InputStream input, ByteArrayOutputStream output, byte[] buffer) throws Exception {
        while (input.available() > 0) {
            int read = input.read(buffer, 0, Math.min(buffer.length, input.available()));
            if (read < 0) {
                break;
            }
            output.write(buffer, 0, read);
        }
    }

    private String normalizePrivateKey(String privateKey) {
        return privateKey.replace("\\n", "\n").trim() + "\n";
    }

    private void rejectDangerousDeletePath(String path) {
        if (StrUtil.equalsAny(path, "/", ".", "~")) {
            throw new OciException(-1, "Refuse to delete dangerous path: " + path);
        }
    }

    private String errorMessage(Exception e) {
        return StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName());
    }

    private String identityName(SshCredentialParams credential) {
        return "king-detective-" + credential.getUsername() + "@" + credential.getHost();
    }

    private String normalizePath(String path) {
        return StrUtil.isBlank(path) ? "." : path.trim();
    }

    private String joinRemotePath(String parent, String child) {
        if ("/".equals(parent)) {
            return "/" + child;
        }
        return parent.endsWith("/") ? parent + child : parent + "/" + child;
    }

    private void disconnect(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    @FunctionalInterface
    private interface SftpCallback<T> {
        T apply(ChannelSftp channel) throws Exception;
    }
}
