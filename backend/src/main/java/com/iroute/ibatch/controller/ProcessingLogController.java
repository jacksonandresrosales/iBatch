package com.iroute.ibatch.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.application.usecase.DashboardService;
import com.iroute.ibatch.dto.response.PageResponse;
import com.iroute.ibatch.dto.response.ProcessingLogResponse;

@RestController
@RequestMapping("/logs")
@Validated
public class ProcessingLogController {

    private final DashboardService dashboardService;

    public ProcessingLogController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public PageResponse<ProcessingLogResponse> getLogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return dashboardService.findRecentLogs(page, size);
    }
}
