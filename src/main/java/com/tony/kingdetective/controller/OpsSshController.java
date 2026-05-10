package com.tony.kingdetective.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.ops.SftpParams;
import com.tony.kingdetective.bean.params.ops.SshBatchCommandParams;
import com.tony.kingdetective.bean.params.ops.SshCommandParams;
import com.tony.kingdetective.bean.params.ops.SshSessionCreateParams;
import com.tony.kingdetective.bean.response.ops.SftpListRsp;
import com.tony.kingdetective.bean.response.ops.SshCommandRsp;
import com.tony.kingdetective.bean.response.ops.SshSessionRsp;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.ops.WebSshService;
import com.tony.kingdetective.service.ops.WebSshSessionRegistry;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class OpsSshController {
    private final WebSshService webSshService;
    private final WebSshSessionRegistry sessionRegistry;

    public OpsSshController(WebSshService webSshService, WebSshSessionRegistry sessionRegistry) {
        this.webSshService = webSshService;
        this.sessionRegistry = sessionRegistry;
    }

    @PostMapping("/ssh/session")
    public ResponseData<SshSessionRsp> createSession(@Valid @RequestBody SshSessionCreateParams params) {
        if (params.getCredential() == null) {
            throw new OciException(-1, "SSH credential is required");
        }
        int ttlMinutes = params.getTtlMinutes() == null ? 15 : params.getTtlMinutes();
        WebSshSessionRegistry.Entry entry = sessionRegistry.create(params.getCredential(), ttlMinutes);
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
        return ResponseData.successData(webSshService.testConnection(params.getCredential()));
    }

    @PostMapping("/ssh/exec")
    public ResponseData<SshCommandRsp> execute(@Valid @RequestBody SshCommandParams params) {
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
                    commandParams.setCredential(target.getCredential());
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
        return ResponseData.successData(webSshService.list(params));
    }

    @PostMapping("/sftp/read")
    public ResponseData<String> read(@RequestBody SftpParams params) {
        return ResponseData.successData(webSshService.readText(params));
    }

    @PostMapping("/sftp/write")
    public ResponseData<Void> write(@RequestBody SftpParams params) {
        webSshService.writeText(params);
        return ResponseData.successData("File saved");
    }

    @PostMapping("/sftp/mkdir")
    public ResponseData<Void> mkdir(@RequestBody SftpParams params) {
        webSshService.mkdir(params);
        return ResponseData.successData("Directory created");
    }

    @PostMapping("/sftp/delete")
    public ResponseData<Void> delete(@RequestBody SftpParams params) {
        webSshService.delete(params);
        return ResponseData.successData("Path deleted");
    }

    @PostMapping("/sftp/rename")
    public ResponseData<Void> rename(@RequestBody SftpParams params) {
        webSshService.rename(params);
        return ResponseData.successData("Path renamed");
    }
}
