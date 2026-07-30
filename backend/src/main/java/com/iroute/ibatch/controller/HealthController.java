package com.iroute.ibatch.controller;

import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.dto.response.ApiHealthResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiHealthResponse health() {
        return new ApiHealthResponse("UP", "iBatch backend disponible", OffsetDateTime.now());
    }
}
