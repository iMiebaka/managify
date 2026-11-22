package com.managify.controller;

import com.managify.dto.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public ping endpoint; a template for new controllers. */
@RestController
@RequestMapping("/public")
@Tag(name = "Public", description = "Endpoints that need no token")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Ping", description = "Returns pong; no token needed")
    public ApiResult<String> ping() {
        return ApiResult.success("pong");
    }
}
