package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.vo.SystemDiagnostics;
import com.tony.kingdetective.service.SystemDiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deployment diagnostics and preflight checks.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemDiagnosticsController {

    private final SystemDiagnosticsService diagnosticsService;

    public SystemDiagnosticsController(SystemDiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/diagnostics")
    public ResponseData<SystemDiagnostics> diagnostics() {
        return ResponseData.successData(diagnosticsService.diagnostics(), "系统诊断完成");
    }
}
