package com.tony.kingdetective.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.ops.SftpParams;
import com.tony.kingdetective.bean.params.ops.SshBatchCommandParams;
import com.tony.kingdetective.bean.params.ops.SshCommandParams;
import com.tony.kingdetective.bean.params.ops.SshCredentialParams;
import com.tony.kingdetective.bean.params.ops.SshHostSaveParams;
import com.tony.kingdetective.bean.params.ops.SshSessionCreateParams;
import com.tony.kingdetective.bean.response.ops.SftpListRsp;
import com.tony.kingdetective.bean.response.ops.SshCommandRsp;
import com.tony.kingdetective.bean.response.ops.SshHostRsp;
import com.tony.kingdetective.bean.response.ops.SshSessionRsp;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.ops.SshHostService;
import com.tony.kingdetective.service.ops.WebSshService;
import com.tony.kingdetective.service.ops.WebSshSessionRegistry;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class OpsSshController {
    private final WebSshService webSshService;
    private final WebSshSessionRegistry sessionRegistry;
    private final SshHostService sshHostService;

    public OpsSshController(WebSshService webSshService, WebSshSessionRegistry sessionRegistry, SshHostService sshHostService) {
        this.webSshService = webSshService;
        this.sessionRegistry = sessionRegistry;
        this.sshHostService = sshHostService;
    }

    @GetMapping("/ssh/hosts")
    public ResponseData<List<SshHostRsp>> hosts(@RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseData.successData(sshHostService.list(keyword));
    }

    @GetMapping("/ssh/hosts/{id}")
    public ResponseData<SshHostRsp> host(@PathVariable("id") String id) {
        return ResponseData.successData(sshHostService.get(id));
    }

    @PostMapping("/ssh/hosts")
    public ResponseData<SshHostRsp> createHost(@Valid @RequestBody SshHostSaveParams params) {
        return ResponseData.successData(sshHostService.create(params));
    }

    @PutMapping("/ssh/hosts/{id}")
    public ResponseData<SshHostRsp> updateHost(@PathVariable("id") String id, @Valid @RequestBody SshHostSaveParams params) {
        return ResponseData.successData(sshHostService.update(id, params));
    }

    @DeleteMapping("/ssh/hosts/{id}")
    public ResponseData<Void> deleteHost(@PathVariable("id") String id) {
        sshHostService.delete(id);
        return ResponseData.successData("Host deleted");
    }

    @PostMapping("/ssh/hosts/{id}/test")
    public ResponseData<Boolean> testHost(@PathVariable("id") String id) {
        return ResponseData.successData(webSshService.testConnection(sshHostService.credentialForHost(id)));
    }

    @PostMapping("/ssh/session")
    public ResponseData<SshSessionRsp> createSession(@Valid @RequestBody SshSessionCreateParams params) {
        if (params.getCredential() == null) {
            throw new OciException(-1, "SSH credential is required");
        }
        SshCredentialParams credential = sshHostService.resolveCredential(params.getCredential());
        int ttlMinutes = params.getTtlMinutes() == null ? 15 : params.getTtlMinutes();
        WebSshSessionRegistry.Entry entry = sessionRegistry.create(credential, ttlMinutes);
        return ResponseData.successData(SshSessionRsp.builder()
                .sessionId(entry.sessionId())
                .expiresAt(entry.expiresAt())
                .websocketPath("/ops/ssh/terminal/" + entry.sessionId())
                .build());
    }

    @PostMapping("/ssh/test")
    public ResponseData<Boolean> test(@RequestBody SshSessionCreateParams params) {
        if (params.getCredential() == null) {
            throw new OciException(-1, "SSH credential is required");
        }
        return ResponseData.successData(webSshService.testConnection(sshHostService.resolveCredential(params.getCredential())));
    }

    @PostMapping("/ssh/exec")
    public ResponseData<SshCommandRsp> execute(@Valid @RequestBody SshCommandParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        return ResponseData.successData(webSshService.execute(params));
    }

    @PostMapping("/ssh/batch")
    public ResponseData<List<SshCommandRsp>> batch(@Valid @RequestBody SshBatchCommandParams params) {
        if (CollectionUtil.isEmpty(params.getTargets())) {
            throw new OciException(-1, "At least one SSH target is required");
        }
        List<SshCommandRsp> results = params.getTargets().parallelStream()
                .map(target -> {
                    SshCommandParams commandParams = new SshCommandParams();
                    commandParams.setCredential(sshHostService.resolveCredential(target.getCredential()));
                    commandParams.setCommand(params.getCommand());
                    commandParams.setTimeoutSeconds(params.getTimeoutSeconds());
                    SshCommandRsp rsp = webSshService.execute(commandParams);
                    rsp.setName(target.getName());
                    return rsp;
                })
                .toList();
        return ResponseData.successData(results);
    }

    @PostMapping("/sftp/list")
    public ResponseData<SftpListRsp> list(@RequestBody SftpParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        return ResponseData.successData(webSshService.list(params));
    }

    @PostMapping("/sftp/read")
    public ResponseData<String> read(@RequestBody SftpParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        return ResponseData.successData(webSshService.readText(params));
    }

    @PostMapping("/sftp/write")
    public ResponseData<Void> write(@RequestBody SftpParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        webSshService.writeText(params);
        return ResponseData.successData("File saved");
    }

    @PostMapping("/sftp/mkdir")
    public ResponseData<Void> mkdir(@RequestBody SftpParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        webSshService.mkdir(params);
        return ResponseData.successData("Directory created");
    }

    @PostMapping("/sftp/delete")
    public ResponseData<Void> delete(@RequestBody SftpParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        webSshService.delete(params);
        return ResponseData.successData("Path deleted");
    }

    @PostMapping("/sftp/rename")
    public ResponseData<Void> rename(@RequestBody SftpParams params) {
        params.setCredential(sshHostService.resolveCredential(params.getCredential()));
        webSshService.rename(params);
        return ResponseData.successData("Path renamed");
    }
}
